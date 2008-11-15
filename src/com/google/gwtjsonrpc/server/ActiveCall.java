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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** An active RPC call. */
public class ActiveCall {
  HttpServletRequest httpRequest;
  HttpServletResponse httpResponse;
  JsonElement id;
  MethodHandle method;
  Object[] params;
  Object result;
  Throwable error;

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
}
