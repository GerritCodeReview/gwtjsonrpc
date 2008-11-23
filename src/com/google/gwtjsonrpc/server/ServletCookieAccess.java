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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;

final class ServletCookieAccess extends CookieAccess {
  @Override
  protected String getCookie(final String name) {
    final ActiveCall call = JsonServlet.getCurrentCall();
    Map<String, String> cookies = call.cookies;
    if (cookies == null) {
      cookies = new HashMap<String, String>();
      final Cookie[] all = call.httpRequest.getCookies();
      if (all != null) {
        for (final Cookie c : all) {
          cookies.put(c.getName(), c.getValue());
        }
      }
      call.cookies = cookies;
    }
    return cookies.get(name);
  }

  @Override
  protected void removeCookie(final String name) {
    final Cookie c = new Cookie(name, "");
    c.setMaxAge(0);
    JsonServlet.getCurrentCall().httpResponse.addCookie(c);
  }
}
