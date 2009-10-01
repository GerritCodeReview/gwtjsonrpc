// Copyright 2008 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gwtjsonrpc.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.StatusCodeException;

class JsonCall<T> implements RequestCallback {
  private static final JavaScriptObject jsonParser;

  static {
    jsonParser = selectJsonParser();
  }

  /**
   * Select the most efficient available JSON parser.
   * 
   * If we have a native JSON parser, present in modern browsers (FF 3.5 and
   * IE8, at time of writing), it is returned. If no native parser is found, a
   * parser function using <code>eval</code> is returned.
   * 
   * This is done dynamically, not with a GWT user.agent check, because FF3.5
   * does not have a specific user.agent associated with it. Introducing a new
   * property for the presence of an ES3.1 parser is not worth it, since the
   * check is only done once anyway, and will result in significantly longer
   * compile times.
   * 
   * As GWT will undoubtedly introduce support for the native JSON parser in the
   * {@link com.google.gwt.json.client.JSONParser JSONParser} class, this code
   * should be reevaluated to possibly use the GWT parser reference.
   * 
   * @return a javascript function with the fastest available JSON parser
   * @see "http://wiki.ecmascript.org/doku.php?id=es3.1:json_support"
   */
  private static native JavaScriptObject selectJsonParser()
  /*-{
    if ($wnd.JSON && typeof $wnd.JSON.parse === 'function')
      return $wnd.JSON.parse;
    else
      return function(expr) { return eval('(' + expr + ')'); };
  }-*/;

  private final AbstractJsonProxy proxy;
  private final String methodName;
  private final String requestParams;
  private final ResultDeserializer<T> resultDeserializer;
  private final AsyncCallback<T> callback;
  private int attempts;

  JsonCall(final AbstractJsonProxy abstractJsonProxy, final String methodName,
      final String requestParams,
      final ResultDeserializer<T> resultDeserializer,
      final AsyncCallback<T> callback) {
    this.proxy = abstractJsonProxy;
    this.methodName = methodName;
    this.requestParams = requestParams;
    this.resultDeserializer = resultDeserializer;
    this.callback = callback;
  }

  AbstractJsonProxy getProxy() {
    return proxy;
  }

  String getMethodName() {
    return methodName;
  }

  void send() {
    final StringBuilder body = new StringBuilder();
    body.append("{\"version\":\"1.1\",\"method\":\"");
    body.append(methodName);
    body.append("\",\"params\":[");
    body.append(requestParams);
    body.append("]");
    final String xsrfKey = proxy.getXsrfManager().getToken(proxy);
    if (xsrfKey != null) {
      body.append(",\"xsrfKey\":");
      body.append(JsonSerializer.escapeString(xsrfKey));
    }
    body.append("}");

    final RequestBuilder rb;
    rb = new RequestBuilder(RequestBuilder.POST, proxy.url);
    rb.setHeader("Content-Type", JsonUtil.JSON_REQ_CT);
    rb.setHeader("Accept", JsonUtil.JSON_TYPE);
    rb.setCallback(this);
    rb.setRequestData(body.toString());

    try {
      attempts++;
      rb.send();
    } catch (RequestException e) {
      callback.onFailure(e);
      return;
    }

    if (attempts == 1) {
      fireEvent(RpcStartEvent.e);
    }
  }

  public void onResponseReceived(final Request req, final Response rsp) {
    final int sc = rsp.getStatusCode();
    if (isJsonBody(rsp)) {
      final RpcResult r;
      try {
        r = parse(jsonParser, rsp.getText());
      } catch (RuntimeException e) {
        fireEvent(RpcCompleteEvent.e);
        callback.onFailure(new InvocationException("Bad JSON response: " + e));
        return;
      }

      if (r.xsrfKey() != null) {
        proxy.getXsrfManager().setToken(proxy, r.xsrfKey());
      }

      if (r.error() != null) {
        final String errmsg = r.error().message();
        if (JsonUtil.ERROR_INVALID_XSRF.equals(errmsg)) {
          if (attempts < 2) {
            // The XSRF cookie was invalidated (or didn't exist) and the
            // service demands we have one in place to make calls to it.
            // A new token was returned to us, so start the request over.
            //
            send();
          } else {
            fireEvent(RpcCompleteEvent.e);
            callback.onFailure(new InvocationException(errmsg));
          }
        } else {
          fireEvent(RpcCompleteEvent.e);
          callback.onFailure(new RemoteJsonException(errmsg, r.error().code(),
              new JSONObject(r.error()).get("error")));
        }
        return;
      }

      if (sc == Response.SC_OK) {
        fireEvent(RpcCompleteEvent.e);
        JsonUtil.invoke(resultDeserializer, callback, r);
        return;
      }
    }

    if (sc == Response.SC_OK) {
      fireEvent(RpcCompleteEvent.e);
      callback.onFailure(new InvocationException("No JSON response"));
    } else {
      fireEvent(RpcCompleteEvent.e);
      callback.onFailure(new StatusCodeException(sc, rsp.getStatusText()));
    }
  }

  public void onError(final Request request, final Throwable exception) {
    fireEvent(RpcCompleteEvent.e);
    if (exception.getClass() == RuntimeException.class
        && exception.getMessage().contains("XmlHttpRequest.status")) {
      // GWT's XMLHTTPRequest class gives us RuntimeException when the
      // status code is unreadable from the browser. This occurs when
      // the connection has failed, e.g. the host is down.
      //
      callback.onFailure(new ServerUnavailableException());
    } else {
      callback.onFailure(exception);
    }
  }

  private <S extends EventHandler> void fireEvent(BaseRpcEvent<S> e) {
    e.call = this;
    JsonUtil.fireEvent(e);
  }

  private static boolean isJsonBody(final Response rsp) {
    String type = rsp.getHeader("Content-Type");
    if (type == null) {
      return false;
    }
    int semi = type.indexOf(';');
    if (semi >= 0) {
      type = type.substring(0, semi).trim();
    }
    return JsonUtil.JSON_TYPE.equals(type);
  }

  /**
   * Call a JSON parser javascript function to parse an encoded JSON string.
   * 
   * @param parser a javascript function
   * @param json encoded JSON text
   * @return the parsed data
   * @see #jsonParser
   */
  private static final native RpcResult parse(JavaScriptObject parserFunction,
      String json)
  /*-{
    return parserFunction(json);
  }-*/;


  private static class RpcResult extends JavaScriptObject {
    protected RpcResult() {
    }

    final native RpcError error()/*-{ return this.error; }-*/;

    final native String xsrfKey()/*-{ return this.xsrfKey; }-*/;
  }

  private static class RpcError extends JavaScriptObject {
    protected RpcError() {
    }

    final native String message()/*-{ return this.message; }-*/;

    final native int code()/*-{ return this.code; }-*/;
  }
}
