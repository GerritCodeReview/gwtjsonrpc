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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class SqlTimestampDeserializer implements
    JsonDeserializer<java.sql.Timestamp>, JsonSerializer<java.sql.Timestamp> {
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

  public JsonElement serialize(final java.sql.Timestamp src,
      final Type typeOfSrc, final JsonSerializationContext context) {
    if (src == null) {
      return new JsonNull();
    }
    String s = src.toString();
    if (s.length() < 29) {
      // The JRE leaves off trailing 0's, but GWT's Timestamp parser wants
      // to see them in the nanos field. If our string is too short, add
      // on trailing 0's to fit the format.
      //
      final StringBuffer r = new StringBuffer(29);
      r.append(s);
      while (r.length() < 29) {
        r.append('0');
      }
      s = r.toString();
    }
    return new JsonPrimitive(s);
  }
}
