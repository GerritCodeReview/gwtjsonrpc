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

package com.google.gwtjsonrpc.server;

import com.google.gson.JsonElement;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** An active RPC call. */
public class ActiveCall implements AsyncCallback<Object> {
  protected final HttpServletRequest httpRequest;
  protected final HttpServletResponse httpResponse;
  JsonElement id;
  MethodHandle method;
  String callback;
  Object[] params;
  Object result;
  Throwable externalFailure;
  Throwable internalFailure;
  Map<String, String> cookies;

  /**
   * Create a new call.
   * 
   * @param req the request.
   * @param resp the response.
   */
  public ActiveCall(final HttpServletRequest req, final HttpServletResponse resp) {
    httpRequest = req;
    httpResponse = resp;
  }

  /**
   * Get the HTTP request that is attempting this RPC call.
   * 
   * @return the original servlet HTTP request.
   */
  public HttpServletRequest getHttpServletRequest() {
    return httpRequest;
  }

  /**
   * Get the HTTP response that will be returned from this call.
   * 
   * @return the original servlet HTTP response.
   */
  public HttpServletResponse getHttpServletResponse() {
    return httpResponse;
  }

  /**
   * Get the user specific token to protect per-user XSRF keys.
   * <p>
   * By default this method uses <code>getRemoteUser()</code>. Services may
   * override this method to acquire a different property of the request, such
   * as data from an HTTP cookie or an extended HTTP header.
   * 
   * @return the user identity; null if the user is anonymous.
   */
  public String getUser() {
    return httpRequest.getRemoteUser();
  }

  /**
   * Get the method this request is asking to invoke.
   * 
   * @return the requested method handle.
   */
  public MethodHandle getMethod() {
    return method;
  }

  /**
   * Get the actual parameter values to be supplied to the method.
   * 
   * @return the parameter array; never null but may be 0-length if the method
   *         takes no parameters.
   */
  public Object[] getParams() {
    return params;
  }

  public final void onSuccess(final Object result) {
    this.result = result;
    this.externalFailure = null;
    this.internalFailure = null;
  }

  public void onFailure(final Throwable error) {
    this.result = null;
    this.externalFailure = error;
    this.internalFailure = null;
  }

  public final void onInternalFailure(final Throwable error) {
    this.result = null;
    this.externalFailure = null;
    this.internalFailure = error;
  }

  /** Mark the response to be uncached by proxies and browsers. */
  public void noCache() {
    httpResponse.setHeader("Cache-Control", "no-cache");
    httpResponse.setDateHeader("Expires", System.currentTimeMillis());
  }
}
