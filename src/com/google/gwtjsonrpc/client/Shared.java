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

/** Shared constants between client and server implementations. */
public class Shared {
  /** Proper Content-Type header value for JSON encoded data. */
  public static final String JSON_TYPE = "application/json";

  /**
   * Name of the HTTP header holding the XSRF token is inserted into.
   */
  public static final String XSRF_HEADER = "X-RPC-XSRF-Token";

  /** HTTP status code when the XSRF token is missing or invalid. */
  public static final int SC_INVALID_XSRF = 400; // aka SC_BAD_REQUEST

  /** Complete content when the XSRF token is missing or invalid. */
  public static final String SM_INVALID_XSRF = "INVALID_XSRF";

  private Shared() {
  }
}
