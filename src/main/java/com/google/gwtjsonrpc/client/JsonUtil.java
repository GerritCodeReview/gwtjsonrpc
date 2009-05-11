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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import java.util.ArrayList;

/** Shared constants between client and server implementations. */
public class JsonUtil {
  /** Proper Content-Type header value for JSON encoded data. */
  public static final String JSON_TYPE = "application/json";

  /** Character encoding preferred for JSON text. */
  public static final String JSON_ENC = "UTF-8";

  /** Request Content-Type header for JSON data. */
  static final String JSON_REQ_CT = JSON_TYPE + "; charset=utf-8";

  /** Error message when xsrfKey in request is missing or invalid. */
  public static final String ERROR_INVALID_XSRF = "Invalid xsrfKey in request";

  private static final ArrayList<RpcStatusListener> listeners =
      new ArrayList<RpcStatusListener>();

  /**
   * Bind a RemoteJsonService proxy to its server URL.
   * 
   * @param <T> type of the service interface.
   * @param imp the service instance, returned by <code>GWT.create</code>.
   * @param path the path of the service, relative to the GWT module.
   * @return always <code>imp</code>.
   */
  public static <T extends RemoteJsonService> T bind(final T imp,
      final String path) {
    assert GWT.isClient();
    assert imp instanceof ServiceDefTarget;
    final String base = GWT.getModuleBaseURL();
    ((ServiceDefTarget) imp).setServiceEntryPoint(base + path);
    return imp;
  }

  /** Register a status listener. */
  public static void addRpcStatusListener(final RpcStatusListener l) {
    assert !listeners.contains(l);
    listeners.add(l);
  }

  /** Remove a registered status listener. */
  public static void removeRpcStatusListener(final RpcStatusListener l) {
    listeners.remove(l);
  }

  static void fireOnCallStart() {
    for (final RpcStatusListener l : listeners) {
      l.onCallStart();
    }
  }

  static void fireOnCallEnd() {
    for (final RpcStatusListener l : listeners) {
      l.onCallEnd();
    }
  }

  public static <T> void invoke(final ResultDeserializer<T> resultDeserializer,
      final AsyncCallback<T> callback, final JavaScriptObject rpcResult) {
    final T result;
    try {
      result = resultDeserializer.fromResult(rpcResult);
    } catch (RuntimeException e) {
      callback.onFailure(new InvocationException("Invalid JSON Response", e));
      return;
    }
    callback.onSuccess(result);
  }

  private JsonUtil() {
  }
}
