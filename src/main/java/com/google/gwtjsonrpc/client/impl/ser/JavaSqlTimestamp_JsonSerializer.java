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

package com.google.gwtjsonrpc.client.impl.ser;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwtjsonrpc.client.impl.JsonSerializer;
import com.google.gwtjsonrpc.client.impl.ResultDeserializer;
import com.google.gwtjsonrpc.common.JavaSqlTimestampHelper;

import java.sql.Timestamp;

/** Default serialization for a {@link java.sql.Timestamp}. */
public final class JavaSqlTimestamp_JsonSerializer extends
    JsonSerializer<java.sql.Timestamp> implements
    ResultDeserializer<java.sql.Timestamp> {
  public static final JavaSqlTimestamp_JsonSerializer INSTANCE =
      new JavaSqlTimestamp_JsonSerializer();

  @Override
  public java.sql.Timestamp fromJson(final Object o) {
    if (o != null) {
      return parseTimestamp((String) o);
    }
    return null;
  }

  @Override
  public void printJson(final StringBuilder sb, final java.sql.Timestamp o) {
    sb.append('"');
    sb.append(toString(o.getTime()));
    sb.append('"');
  }

  protected static String padTwo(final int v) {
    if (v < 10) {
      return "0" + v;
    } else {
      return String.valueOf(v);
    }
  }

  protected static String padThree(final int v) {
    if (v < 10) {
      return "00" + v;
    } else if (v < 100) {
      return "0" + v;
    } else {
      return String.valueOf(v);
    }
  }

  private static native String toString(double utcMilli)
  /*-{
    var d = new Date(utcMilli);
    var p2 = @com.google.gwtjsonrpc.client.impl.ser.JavaSqlTimestamp_JsonSerializer::padTwo(I);
    var p3 = @com.google.gwtjsonrpc.client.impl.ser.JavaSqlTimestamp_JsonSerializer::padThree(I);
    return d.getUTCFullYear() + "-" +
    p2(1 + d.getUTCMonth()) + "-" +
    p2(d.getUTCDate())+ " " +
    p2(d.getUTCHours()) + ":" +
    p2(d.getUTCMinutes()) + ":" +
    p2(d.getUTCSeconds()) + "." +
    p3(d.getUTCMilliseconds()) + "000000";
  }-*/;

  public static java.sql.Timestamp parseTimestamp(final String s) {
    return JavaSqlTimestampHelper.parseTimestamp(s);
  }

  @Override
  public Timestamp fromResult(JavaScriptObject responseObject) {
    return fromJson(PrimitiveResultDeserializers.stringResult(responseObject));
  }
}
