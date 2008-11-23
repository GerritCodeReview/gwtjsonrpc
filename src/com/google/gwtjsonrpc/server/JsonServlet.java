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

import static com.google.gwtjsonrpc.client.JsonUtil.XSRF_HEADER;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.server.rpc.RPCServletUtils;
import com.google.gwtjsonrpc.client.CookieAccess;
import com.google.gwtjsonrpc.client.JsonUtil;
import com.google.gwtjsonrpc.client.RemoteJsonService;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Basic HTTP servlet to forward JSON based RPC requests onto services.
 * <p>
 * Implementors of a JSON-RPC service should extend this servlet and implement
 * any interface(s) that extend from {@link RemoteJsonService}. Clients may
 * invoke methods declared in any implemented interface.
 * <p>
 * Calling conventions match the JSON-RPC 1.1 working draft from 7 August 2006
 * (<a href="http://json-rpc.org/wd/JSON-RPC-1-1-WD-20060807.html">draft</a>).
 * Only positional parameters are supported.
 * <p>
 * When supported by the browser/client, the "gzip" encoding is used to compress
 * the resulting JSON, reducing transfer time for the response data.
 */
public abstract class JsonServlet extends HttpServlet {
  private static final ThreadLocal<HttpServletRequest> perThreadRequest;
  private static final ThreadLocal<HttpServletResponse> perThreadResponse;

  static {
    perThreadRequest = new ThreadLocal<HttpServletRequest>();
    perThreadResponse = new ThreadLocal<HttpServletResponse>();
    CookieAccess.setImplementation(new ServetCookieAccess());
  }

  /** Get the <code>HttpServletRequest</code> object for the current call. */
  public static final HttpServletRequest getCurrentRequest() {
    return perThreadRequest.get();
  }

  /** Get the <code>HttpServletResponse</code> object for the current call */
  public static final HttpServletResponse getCurrentResponse() {
    return perThreadResponse.get();
  }

  static final Object[] NO_PARAMS = {};
  static final String RPC_VERSION = "1.1";
  private static final String ENC = "UTF-8";

  private Map<String, MethodHandle> myMethods;
  private SignedToken xsrf;

  @Override
  public void init(final ServletConfig config) throws ServletException {
    super.init(config);

    final RemoteJsonService impl;
    try {
      impl = (RemoteJsonService) createServiceHandle();
    } catch (Exception e) {
      throw new ServletException("Service handle not available", e);
    }

    myMethods = methods(impl);
    if (myMethods.isEmpty()) {
      throw new ServletException("No service methods declared");
    }

    try {
      xsrf = createXsrfSignedToken();
    } catch (XsrfException e) {
      throw new ServletException("Cannot initialize XSRF", e);
    }
  }

  /**
   * Get the object which provides the RemoteJsonService implementation.
   * 
   * @return by default <code>this</code>, but any object which implements a
   *         RemoteJsonService interface.
   * @throws Exception any error indicating the service is not configured.
   */
  protected Object createServiceHandle() throws Exception {
    return this;
  }

  /**
   * Initialize the XSRF state for this service.
   * <p>
   * By default this method creates a unique XSRF key for this service. Service
   * implementors may wish to override this method to use a pooled instance that
   * relies upon a stable private key.
   * 
   * @return new XSRF implementation. Null if the caller has overridden all
   *         relevant XSRF methods and is implementing their own XSRF protection
   *         algorithm.
   * @throws XsrfException the XSRF utility could not be created.
   */
  protected SignedToken createXsrfSignedToken() throws XsrfException {
    return new SignedToken(4 * 60 * 60 /* seconds */);
  }

  /**
   * Get the user specific token to protect per-user XSRF keys.
   * <p>
   * By default this method uses <code>getRemoteUser()</code>. Services may
   * override this method to acquire a different property of the request, such
   * as data from an HTTP cookie or an extended HTTP header.
   * 
   * @param call current RPC being processed.
   * @return the user identity; null if the user is anonymous.
   */
  protected String xsrfUser(final ActiveCall call) {
    return call.httpRequest.getRemoteUser();
  }

  /**
   * Verify the XSRF token submitted is valid.
   * <p>
   * By default this method validates the token, and refreshes it with a new
   * token for the currently authenticated user.
   * 
   * @param call current RPC being processed.
   * @return true if the token was supplied and is valid; false otherwise.
   * @throws XsrfException the token could not be validated due to an error that
   *         the client cannot recover from.
   */
  protected boolean xsrfValidate(final ActiveCall call) throws XsrfException {
    final HttpServletRequest req = call.httpRequest;
    final String user = xsrfUser(call);
    final String path = req.getServletPath();
    final String userpath = ":" + user + ":" + path;
    call.httpResponse.addHeader(XSRF_HEADER, xsrf.newToken(userpath));
    return xsrf.checkToken(req.getHeader(XSRF_HEADER), userpath);
  }

  /**
   * Lookup a method implemented by this servlet.
   * 
   * @param methodName name of the method.
   * @return the method handle; null if the method is not declared.
   */
  protected MethodHandle lookupMethod(final String methodName) {
    return myMethods.get(methodName);
  }

  @Override
  protected void doPost(final HttpServletRequest req,
      final HttpServletResponse resp) throws IOException {
    try {
      perThreadRequest.set(req);
      perThreadResponse.set(resp);
      doService(req, resp);
    } finally {
      perThreadRequest.set(null);
      perThreadResponse.set(null);
    }
  }

  private void doService(final HttpServletRequest req,
      final HttpServletResponse resp) throws IOException,
      UnsupportedEncodingException {
    noCache(resp);

    if (!JsonUtil.JSON_TYPE.equals(req.getContentType())) {
      error(resp, SC_BAD_REQUEST, "Invalid request Content-Type");
      return;
    }
    if (!JsonUtil.JSON_TYPE.equals(req.getHeader("Accept"))) {
      error(resp, SC_BAD_REQUEST, "Must accept " + JsonUtil.JSON_TYPE);
      return;
    }
    if (req.getContentLength() == 0) {
      error(resp, SC_BAD_REQUEST, "POST body required");
      return;
    }

    final ActiveCall call;
    try {
      call = parseRequest(req);
    } catch (NoSuchRemoteMethodException err) {
      error(resp, SC_NOT_FOUND, "Not Found");
      return;
    }

    if (call.method != null) {
      call.httpRequest = req;
      call.httpResponse = resp;

      try {
        if (!xsrfValidate(call)) {
          error(resp, JsonUtil.SC_INVALID_XSRF, JsonUtil.SM_INVALID_XSRF);
          return;
        }
      } catch (XsrfException e) {
        error(resp, JsonUtil.SC_INVALID_XSRF, JsonUtil.SM_INVALID_XSRF);
        return;
      }

      call.method.invoke(call.params, new AsyncCallback<Object>() {
        public void onFailure(final Throwable c) {
          call.error = c;
        }

        public void onSuccess(final Object r) {
          call.result = r;
        }
      });
    }

    final String out = formatResult(call);
    if (call.error != null) {
      resp.setStatus(SC_INTERNAL_SERVER_ERROR);
    }
    RPCServletUtils.writeResponse(getServletContext(), resp, out,
        RPCServletUtils.acceptsGzipEncoding(req)
            && RPCServletUtils.exceedsUncompressedContentLengthLimit(out));
  }

  private static void noCache(final HttpServletResponse resp) {
    resp.setHeader("Cache-Control", "no-cache");
    resp.addDateHeader("Expires", System.currentTimeMillis());
  }

  private ActiveCall parseRequest(final HttpServletRequest req)
      throws UnsupportedEncodingException, IOException {
    final Reader in = new InputStreamReader(req.getInputStream(), ENC);
    try {
      try {
        return parseRequest(in);
      } catch (JsonParseException err) {
        final ActiveCall call = new ActiveCall();
        call.error = err;
        return call;
      }
    } finally {
      in.close();
    }
  }

  private ActiveCall parseRequest(final Reader in) {
    final GsonBuilder gb = new GsonBuilder();
    gb.registerTypeAdapter(java.sql.Date.class,
        new JsonDeserializer<java.sql.Date>() {
          public java.sql.Date deserialize(final JsonElement json,
              final Type typeOfT, final JsonDeserializationContext context)
              throws JsonParseException {
            if (json.isJsonNull()) {
              return null;
            }
            if (!json.isJsonPrimitive()) {
              throw new JsonParseException("Expected string for date type");
            }
            final JsonPrimitive p = (JsonPrimitive) json;
            if (!p.isString()) {
              throw new JsonParseException("Expected string for date type");
            }
            try {
              return java.sql.Date.valueOf(p.getAsString());
            } catch (IllegalArgumentException e) {
              throw new JsonParseException("Not a date string");
            }
          }
        });
    gb.registerTypeAdapter(java.sql.Timestamp.class,
        new JsonDeserializer<java.sql.Timestamp>() {
          public java.sql.Timestamp deserialize(final JsonElement json,
              final Type typeOfT, final JsonDeserializationContext context)
              throws JsonParseException {
            if (json.isJsonNull()) {
              return null;
            }
            if (!json.isJsonPrimitive()) {
              throw new JsonParseException("Expected string for timestamp type");
            }
            final JsonPrimitive p = (JsonPrimitive) json;
            if (!p.isString()) {
              throw new JsonParseException("Expected string for timestamp type");
            }
            try {
              return java.sql.Timestamp.valueOf(p.getAsString());
            } catch (IllegalArgumentException e) {
              throw new JsonParseException("Not a timestamp string");
            }
          }
        });
    gb.registerTypeAdapter(ActiveCall.class, new RpcRequestDeserializer(this));
    return gb.create().fromJson(in, ActiveCall.class);
  }

  private String formatResult(final ActiveCall result)
      throws UnsupportedEncodingException, IOException {
    final StringWriter o = new StringWriter();
    formatResult(result, o);
    o.close();
    return o.toString();
  }

  private void formatResult(final ActiveCall result, final Writer o) {
    final GsonBuilder gb = new GsonBuilder();
    gb.registerTypeAdapter(java.sql.Date.class,
        new JsonSerializer<java.sql.Date>() {
          public JsonElement serialize(final java.sql.Date src,
              final Type typeOfSrc, final JsonSerializationContext context) {
            if (src == null) {
              return new JsonNull();
            }
            return new JsonPrimitive(src.toString());
          }
        });
    gb.registerTypeAdapter(java.sql.Timestamp.class,
        new JsonSerializer<java.sql.Timestamp>() {
          public JsonElement serialize(final java.sql.Timestamp src,
              final Type typeOfSrc, final JsonSerializationContext context) {
            if (src == null) {
              return new JsonNull();
            }
            return new JsonPrimitive(src.toString());
          }
        });
    gb.registerTypeAdapter(ActiveCall.class, new JsonSerializer<ActiveCall>() {
      public JsonElement serialize(final ActiveCall src, final Type typeOfSrc,
          final JsonSerializationContext context) {
        final JsonObject r = new JsonObject();
        r.addProperty("version", RPC_VERSION);
        if (src.id != null) {
          r.add("id", src.id);
        }
        if (src.error != null) {
          final JsonObject error = new JsonObject();
          error.addProperty("name", "JSONRPCError");
          error.addProperty("code", 999);
          error.addProperty("message", src.error.getMessage());
          r.add("error", error);
        } else {
          r.add("result", context.serialize(src.result));
        }
        return r;
      }
    });
    gb.create().toJson(result, o);
  }

  private static void error(final HttpServletResponse resp, final int status,
      final String message) throws IOException {
    resp.setStatus(status);
    resp.setContentType("text/plain; charset=" + ENC);

    final Writer w = new OutputStreamWriter(resp.getOutputStream(), ENC);
    try {
      w.write(message);
    } finally {
      w.close();
    }
  }

  private static Map<String, MethodHandle> methods(final RemoteJsonService impl) {
    final Class<? extends RemoteJsonService> d = findInterface(impl.getClass());
    if (d == null) {
      return Collections.<String, MethodHandle> emptyMap();
    }

    final Map<String, MethodHandle> r = new HashMap<String, MethodHandle>();
    for (final Method m : d.getMethods()) {
      if (!Modifier.isPublic(m.getModifiers())) {
        continue;
      }

      if (m.getReturnType() != Void.TYPE) {
        continue;
      }

      final Class<?>[] params = m.getParameterTypes();
      if (params.length < 1) {
        continue;
      }

      if (!params[params.length - 1].isAssignableFrom(AsyncCallback.class)) {
        continue;
      }

      final MethodHandle h = new MethodHandle(impl, m);
      r.put(h.getName(), h);
    }
    return Collections.unmodifiableMap(r);
  }

  @SuppressWarnings("unchecked")
  private static Class<? extends RemoteJsonService> findInterface(Class<?> c) {
    while (c != null) {
      if (c.isInterface() && RemoteJsonService.class.isAssignableFrom(c)) {
        return (Class<RemoteJsonService>) c;
      }
      for (final Class<?> i : c.getInterfaces()) {
        final Class<? extends RemoteJsonService> r = findInterface(i);
        if (r != null) {
          return r;
        }
      }
      c = c.getSuperclass();
    }
    return null;
  }
}
