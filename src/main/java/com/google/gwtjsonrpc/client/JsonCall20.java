// Copyright 2009 Google Inc.
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
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.StatusCodeException;

/** Base JsonCall implementation for JsonRPC version 2.0 */
abstract class JsonCall20<T> extends JsonCall<T> {
  protected static int lastRequestId = 0;

  protected int requestId;

  JsonCall20(AbstractJsonProxy abstractJsonProxy, String methodName,
      String requestParams, ResultDeserializer<T> resultDeserializer,
      AsyncCallback<T> callback) {
    super(abstractJsonProxy, methodName, requestParams, resultDeserializer,
        callback);
  }

  @Override
  public void onResponseReceived(final Request req, final Response rsp) {
    // FIXME: implement version 2.0
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

    if (rsp.getStatusCode() == Response.SC_OK) {
      fireEvent(RpcCompleteEvent.e);
      callback.onFailure(new InvocationException("No JSON response"));
    } else {
      fireEvent(RpcCompleteEvent.e);
      callback.onFailure(new StatusCodeException(rsp.getStatusCode(), rsp
          .getStatusText()));
    }
  }

  protected static boolean isJsonBody(final Response rsp) {
    String type = rsp.getHeader("Content-Type");
    if (type == null) {
      return false;
    }
    int semi = type.indexOf(';');
    if (semi >= 0) {
      type = type.substring(0, semi).trim();
    }
    return JsonUtil.JSONRPC20_ACCEPT_CTS.contains(type);
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
