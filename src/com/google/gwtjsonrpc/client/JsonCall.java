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
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.StatusCodeException;

class JsonCall<T> implements RequestCallback {
  private final AbstractJsonProxy proxy;
  private final boolean allowCrossSiteRequest;
  private final String requestData;
  private final JsonSerializer<T> resultSerializer;
  private final AsyncCallback<T> callback;
  private int attempts;

  JsonCall(final AbstractJsonProxy abstractJsonProxy, final boolean allowXsrf,
      final String requestData, final JsonSerializer<T> resultSerializer,
      final AsyncCallback<T> callback) {
    this.proxy = abstractJsonProxy;
    this.allowCrossSiteRequest = allowXsrf;
    this.requestData = requestData;
    this.resultSerializer = resultSerializer;
    this.callback = callback;
  }

  void send() {
    final RequestBuilder rb;

    rb = new RequestBuilder(RequestBuilder.POST, proxy.url);
    rb.setHeader("Content-Type", JsonUtil.JSON_TYPE);
    rb.setHeader("Accept", JsonUtil.JSON_TYPE);
    rb.setRequestData(requestData);
    rb.setCallback(this);
    if (!allowCrossSiteRequest && proxy.xsrfKey != null) {
      rb.setHeader(JsonUtil.XSRF_HEADER, proxy.xsrfKey);
    }

    try {
      attempts++;
      rb.send();
    } catch (RequestException e) {
      callback.onFailure(e);
      return;
    }

    if (attempts == 1) {
      JsonUtil.fireOnCallStart();
    }
  }

  public void onResponseReceived(final Request req, final Response rsp) {
    final int sc = rsp.getStatusCode();

    rememberXsrfKey(rsp);

    if (sc == JsonUtil.SC_INVALID_XSRF
        && JsonUtil.SM_INVALID_XSRF.equals(rsp.getText())) {
      if (allowCrossSiteRequest) {
        // This wasn't supposed to happen.
        //
        JsonUtil.fireOnCallEnd();
        callback.onFailure(new InvocationException(JsonUtil.SM_INVALID_XSRF));
      } else if (attempts < 2) {
        // The XSRF cookie was invalidated (or didn't exist) and the
        // service demands we have one in place to make calls to it.
        // A new token was returned to us, so start the request over.
        //
        send();
      } else {
        JsonUtil.fireOnCallEnd();
        callback.onFailure(new InvocationException(rsp.getText()));
      }
      return;
    }

    if (isJsonBody(rsp)) {
      final RpcResult r = parse(rsp.getText());
      if (r.error() != null) {
        JsonUtil.fireOnCallEnd();
        callback.onFailure(new RemoteJsonException(r.error().message()));
        return;
      }

      if (sc == Response.SC_OK) {
        JsonUtil.fireOnCallEnd();
        JsonUtil.invoke(resultSerializer, callback, r.result());
        return;
      }
    }

    if (sc == Response.SC_OK) {
      JsonUtil.fireOnCallEnd();
      callback.onFailure(new InvocationException("No JSON response"));
    } else {
      JsonUtil.fireOnCallEnd();
      callback.onFailure(new StatusCodeException(sc, rsp.getStatusText()));
    }
  }

  public void onError(final Request request, final Throwable exception) {
    JsonUtil.fireOnCallEnd();
    callback.onFailure(exception);
  }

  private void rememberXsrfKey(final Response rsp) {
    final String v = rsp.getHeader(JsonUtil.XSRF_HEADER);
    if (v != null) {
      proxy.xsrfKey = v;
    }
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

  private static final native RpcResult parse(String json)/*-{ return eval('('+json+')'); }-*/;

  private static class RpcResult extends JavaScriptObject {
    protected RpcResult() {
    }

    final native RpcError error()/*-{ return this.error; }-*/;

    final native JavaScriptObject result()/*-{ return this.result; }-*/;
  }

  private static class RpcError extends JavaScriptObject {
    protected RpcError() {
    }

    final native String message()/*-{ return this.message; }-*/;
  }
}
