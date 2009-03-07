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

import com.google.gwt.json.client.JSONNull;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Exception given to {@link AsyncCallback#onFailure(Throwable)}.
 * <p>
 * This exception is used if the remote JSON server has returned a well-formed
 * JSON error response.
 */
public class RemoteJsonException extends Exception {
  private int code;
  private JSONValue error;

  /**
   * Construct a new exception representing a welformed JSON error response.
   * 
   * @param message A String value that provides a short description of the
   *        error.
   * @param code A number that indicates the actual error that occurred.
   * @param error A {@link JSONNull}, {@link JSONNumber}, {@link JSONString} or
   *        {@link JSONObject} value that carries custom and
   *        application-specific error information.
   */
  public RemoteJsonException(final String message, int code, JSONValue error) {
    super(message);
    this.code = code;
    this.error = error;
  }

  public RemoteJsonException(final String message) {
    this(message, 999, null);
  }

  /**
   * Gets the error code.
   * <p>
   * Note that the JSON-RPC 1.1 draf does not define error codes yet.
   * 
   * @return A number that indicates the actual error that occurred.
   */
  public int getCode() {
    return code;
  }

  /**
   * Gets the extra error information.
   * 
   * @return <code>null</code> if no error information was specified by the
   *         server, or a {@link JSONNull}, {@link JSONNumber},
   *         {@link JSONString} or {@link JSONObject} value.
   */
  public JSONValue getError() {
    return error;
  }
}
