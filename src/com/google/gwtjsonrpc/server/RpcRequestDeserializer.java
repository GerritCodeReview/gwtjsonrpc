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

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

final class RpcRequestDeserializer implements JsonDeserializer<ActiveCall> {
  private static final String RPC_VERSION = JsonServlet.RPC_VERSION;
  private final JsonServlet server;

  RpcRequestDeserializer(final JsonServlet jsonServlet) {
    server = jsonServlet;
  }

  public ActiveCall deserialize(final JsonElement json, final Type typeOfT,
      final JsonDeserializationContext context) throws JsonParseException,
      NoSuchRemoteMethodException {
    if (!json.isJsonObject()) {
      throw new JsonParseException("Expected object");
    }

    final JsonObject in = json.getAsJsonObject();
    final ActiveCall req = new ActiveCall();
    req.id = in.get("id");

    final JsonElement version = in.get("version");
    if (!isString(version) || !RPC_VERSION.equals(version.getAsString())) {
      throw new JsonParseException("Expected version = " + RPC_VERSION);
    }

    final JsonElement method = in.get("method");
    if (!isString(method)) {
      throw new JsonParseException("Exepected method name as string");
    }

    req.method = server.lookupMethod(method.getAsString());
    if (req.method == null) {
      throw new NoSuchRemoteMethodException();
    }

    final Class<?>[] paramTypes = req.method.getParamTypes();
    final JsonElement params = in.get("params");
    if (params != null) {
      if (!params.isJsonArray()) {
        throw new JsonParseException("Expected params array");
      }

      final JsonArray paramsArray = params.getAsJsonArray();
      if (paramsArray.size() != paramTypes.length) {
        throw new JsonParseException("Expected " + paramTypes.length
            + " parameter values in params array");
      }

      final Object[] r = new Object[paramTypes.length];
      for (int i = 0; i < r.length; i++) {
        r[i] = context.deserialize(paramsArray.get(i), paramTypes[i]);
      }
      req.params = r;
    } else {
      if (paramTypes.length != 0) {
        throw new JsonParseException("Expected params array");
      }
      req.params = JsonServlet.NO_PARAMS;
    }

    return req;
  }

  private static boolean isString(final JsonElement e) {
    return e != null && e.isJsonPrimitive()
        && e.getAsJsonPrimitive().isString();
  }
}
