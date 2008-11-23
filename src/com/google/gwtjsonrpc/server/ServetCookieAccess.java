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

import com.google.gwtjsonrpc.client.CookieAccess;

import javax.servlet.http.Cookie;

final class ServetCookieAccess extends CookieAccess {
  @Override
  protected String getCookie(final String name) {
    if (name == null) {
      return null;
    }
    for (final Cookie c : JsonServlet.getCurrentRequest().getCookies()) {
      if (name.equals(c.getName())) {
        return c.getValue();
      }
    }
    return null;
  }

  @Override
  protected void removeCookie(final String name) {
    final Cookie c = new Cookie(name, "");
    c.setMaxAge(0);
    JsonServlet.getCurrentResponse().addCookie(c);
  }
}
