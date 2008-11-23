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
import com.google.gwt.user.client.Cookies;

/** Provides access to Cookie values, from either client or server code. */
public abstract class CookieAccess {
  private static CookieAccess impl;

  /**
   * Get the value of a cookie.
   * 
   * @param name the name of the cookie.
   * @return the cookie's value; or null.
   */
  public static String get(final String name) {
    if (GWT.isClient()) {
      return Cookies.getCookie(name);
    } else {
      return impl.getCookie(name);
    }
  }

  /**
   * Install the server side implementation.
   * <p>
   * This is invoked by <code>JsonServlet</code>'s static class constructor to
   * insert an implementation which knows how to obtain cookies through the
   * current HttpServletRequest. It should never be necessary for application
   * code to call this method.
   * 
   * @param ca the new access implementation.
   */
  public static void setImplementation(final CookieAccess ca) {
    impl = ca;
  }

  protected abstract String getCookie(String name);
}
