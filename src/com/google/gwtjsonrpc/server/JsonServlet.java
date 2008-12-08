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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.server.rpc.RPCServletUtils;
import com.google.gwtjsonrpc.client.CookieAccess;
import com.google.gwtjsonrpc.client.JsonUtil;
import com.google.gwtjsonrpc.client.RemoteJsonService;

import java.io.IOException;
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
import java.util.regex.Pattern;

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
public abstract class JsonServlet<CallType extends ActiveCall> extends
    HttpServlet {
  /** Pattern that any safe JSON-in-script callback conforms to. */
  public static final Pattern SAFE_CALLBACK =
      Pattern.compile("^([A-Za-z0-9_$.]|\\[|\\])+$");

  private static final ThreadLocal<ActiveCall> perThreadCall;

  static {
    perThreadCall = new ThreadLocal<ActiveCall>();
    CookieAccess.setImplementation(new ServletCookieAccess());
  }

  /** Get the <code>ActiveCall</code> object for the current call. */
  @SuppressWarnings("unchecked")
  public static <CallType extends ActiveCall> CallType getCurrentCall() {
    return (CallType) perThreadCall.get();
  }

  /** Create a default GsonBuilder with some extra types defined. */
  public static GsonBuilder defaultGsonBuilder() {
    final GsonBuilder gb = new GsonBuilder();
    gb.registerTypeAdapter(java.util.Map.class, new MapDeserializer());
    gb.registerTypeAdapter(java.sql.Date.class, new SqlDateDeserializer());
    gb.registerTypeAdapter(java.sql.Timestamp.class,
        new SqlTimestampDeserializer());
    return gb;
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

  /** Create a GsonBuilder to parse a request or return a response. */
  protected GsonBuilder createGsonBuilder() {
    return defaultGsonBuilder();
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
  protected boolean xsrfValidate(final CallType call) throws XsrfException {
    final HttpServletRequest req = call.httpRequest;
    final String username = call.getUser();
    final StringBuilder b = new StringBuilder();
    if (username != null) {
      b.append("user/");
      b.append(username);
    } else {
      b.append("anonymous");
    }
    b.append('$');
    b.append(req.getServletPath());
    final String userpath = b.toString();
    final ValidToken t = xsrf.checkToken(req.getHeader(XSRF_HEADER), userpath);
    if (t == null || t.needsRefresh()) {
      call.httpResponse.addHeader(XSRF_HEADER, xsrf.newToken(userpath));
    }
    return t != null;
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

  /**
   * Create a new call structure for the active HTTP request.
   * 
   * @param req the incoming request.
   * @param resp the response to return to the client.
   * @return the new call wrapping both.
   */
  @SuppressWarnings("unchecked")
  protected CallType createActiveCall(final HttpServletRequest req,
      final HttpServletResponse resp) {
    return (CallType) new ActiveCall(req, resp);
  }

  /**
   * Invoked just before the service method is invoked.
   * <p>
   * Subclasses may override this method to perform additional checks, such as
   * per-method application level security validation. An override of this
   * method should take the following form:
   * 
   * <pre>
   * protected void preInvoke(final CallType call) {
   *   super.preInvoke(call);
   *   if (call.isComplete()) {
   *     return;
   *   }
   *   // your logic here
   * }
   * </pre>
   * <p>
   * If either the {@link ActiveCall#onFailure(Throwable)} or
   * {@link ActiveCall#onInternalFailure(Throwable)} method is invoked with a
   * non-null exception argument the method call itself will be bypassed and the
   * error response will be returned to the client instead.
   * 
   * @param call the current call information.
   */
  protected void preInvoke(final CallType call) {
  }

  @Override
  protected void service(final HttpServletRequest req,
      final HttpServletResponse resp) throws IOException {
    try {
      final CallType c = createActiveCall(req, resp);
      perThreadCall.set(c);
      doService(c);
    } finally {
      perThreadCall.set(null);
    }
  }

  private void doService(final CallType call) throws IOException,
      UnsupportedEncodingException {
    call.noCache();

    if (!JsonUtil.JSON_TYPE.equals(call.httpRequest.getHeader("Accept"))) {
      error(call, SC_BAD_REQUEST, "Must Accept " + JsonUtil.JSON_TYPE);
      return;
    }

    try {
      if ("GET".equals(call.httpRequest.getMethod())) {
        parseGetRequest(call);
      } else if ("POST".equals(call.httpRequest.getMethod())) {
        parsePostRequest(call);
      } else {
        error(call, SC_BAD_REQUEST, "Unsupported HTTP Method");
        return;
      }
    } catch (NoSuchRemoteMethodException err) {
      error(call, SC_NOT_FOUND, "Method Not Found");
      return;
    } catch (JsonParseException err) {
      error(call, SC_BAD_REQUEST, "Invalid JSON Request");
      return;
    }

    if (call.callback != null
        && !SAFE_CALLBACK.matcher(call.callback).matches()) {
      error(call, SC_BAD_REQUEST, "Unsafe callback");
      return;
    }

    if (!call.method.allowCrossSiteRequest()) {
      try {
        if (!xsrfValidate(call)) {
          error(call, JsonUtil.SC_INVALID_XSRF, JsonUtil.SM_INVALID_XSRF);
          return;
        }
      } catch (XsrfException e) {
        error(call, JsonUtil.SC_INVALID_XSRF, JsonUtil.SM_INVALID_XSRF);
        return;
      }
    }

    preInvoke(call);
    if (!call.isComplete()) {
      call.method.invoke(call.params, call);
    }

    if (call.internalFailure != null) {
      final String msg = "Error in " + call.method.getName();
      getServletContext().log(msg, call.internalFailure);
      error(call, SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
      return;
    }

    final String out = formatResult(call);
    RPCServletUtils.writeResponse(getServletContext(), call.httpResponse, out,
        call.callback == null
            && RPCServletUtils.acceptsGzipEncoding(call.httpRequest)
            && RPCServletUtils.exceedsUncompressedContentLengthLimit(out));
  }

  private void parseGetRequest(final ActiveCall call) {
    final Gson gs = createGsonBuilder().create();
    final HttpServletRequest req = call.httpRequest;
    call.method = lookupMethod(req.getParameter("method"));
    if (call.method == null) {
      throw new NoSuchRemoteMethodException();
    }

    final Class<?>[] paramTypes = call.method.getParamTypes();
    final Object[] r = new Object[paramTypes.length];
    for (int i = 0; i < r.length; i++) {
      final String v = req.getParameter("param" + i);
      if (v == null) {
        r[i] = null;
      } else if (paramTypes[i] == String.class) {
        r[i] = v;
      } else if (paramTypes[i].getName().startsWith("java.lang.")) {
        // Primitive type, use the JSON representation of that type.
        //
        r[i] = gs.fromJson(v, paramTypes[i]);
      } else {
        // Assume it is like a java.sql.Timestamp or something and treat
        // the value as JSON string.
        //
        r[i] = gs.fromJson(gs.toJson(v), paramTypes[i]);
      }
    }
    call.params = r;
    call.callback = req.getParameter("callback");
  }

  private static boolean isJsonBody(final ActiveCall call) {
    String type = call.httpRequest.getContentType();
    if (type == null) {
      return false;
    }
    int semi = type.indexOf(';');
    if (semi >= 0) {
      type = type.substring(0, semi).trim();
    }
    return JsonUtil.JSON_TYPE.equals(type);
  }

  private void parsePostRequest(final CallType call)
      throws UnsupportedEncodingException, IOException {
    final HttpServletRequest req = call.httpRequest;
    if (!isJsonBody(call)) {
      throw new JsonParseException("Invalid Request Content-Type");
    }
    if (call.httpRequest.getContentLength() == 0) {
      throw new JsonParseException("POST Body Required");
    }
    final Reader in = req.getReader();
    try {
      try {
        parsePostRequest(call, in);
      } catch (JsonParseException err) {
        call.method = null;
        call.params = null;
        throw err;
      }
    } finally {
      in.close();
    }
  }

  private void parsePostRequest(final CallType call, final Reader in) {
    final GsonBuilder gb = createGsonBuilder();
    gb.registerTypeAdapter(ActiveCall.class, new CallDeserializer<CallType>(
        call, this));
    gb.create().fromJson(in, ActiveCall.class);
  }

  private String formatResult(final ActiveCall call)
      throws UnsupportedEncodingException, IOException {
    final GsonBuilder gb = createGsonBuilder();
    gb.registerTypeAdapter(call.getClass(), new JsonSerializer<ActiveCall>() {
      public JsonElement serialize(final ActiveCall src, final Type typeOfSrc,
          final JsonSerializationContext context) {
        if (call.callback != null) {
          if (src.externalFailure != null) {
            return new JsonNull();
          }
          return context.serialize(src.result);
        }

        final JsonObject r = new JsonObject();
        r.addProperty("version", RPC_VERSION);
        if (src.id != null) {
          r.add("id", src.id);
        }
        if (src.externalFailure != null) {
          final JsonObject error = new JsonObject();
          error.addProperty("name", "JSONRPCError");
          error.addProperty("code", 999);
          error.addProperty("message", src.externalFailure.getMessage());
          r.add("error", error);
        } else {
          r.add("result", context.serialize(src.result));
        }
        return r;
      }
    });

    final StringWriter o = new StringWriter();
    if (call.callback != null) {
      o.write(call.callback);
      o.write("(");
    }
    gb.create().toJson(call, o);
    if (call.callback != null) {
      o.write(");");
    }
    o.close();
    return o.toString();
  }

  private static void error(final ActiveCall call, final int status,
      final String message) throws IOException {
    final HttpServletResponse r = call.httpResponse;
    r.setStatus(status);
    r.setContentType("text/plain; charset=" + ENC);

    final Writer w = new OutputStreamWriter(r.getOutputStream(), ENC);
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
