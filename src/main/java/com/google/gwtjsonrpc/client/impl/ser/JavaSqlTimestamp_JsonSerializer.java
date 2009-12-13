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

import java.sql.Timestamp;
import java.util.Date;

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

  @SuppressWarnings("deprecation")
  private static java.sql.Timestamp parseTimestamp(final String s) {
    final String[] components = s.split(" ");
    if (components.length != 2) {
      throw new IllegalArgumentException("Invalid escape format: " + s);
    }

    final String[] timeComponents = components[1].split("\\.");
    if (timeComponents.length != 2) {
      throw new IllegalArgumentException("Invalid escape format: " + s);
    } else if (timeComponents[1].length() != 9) {
      throw new IllegalArgumentException("Invalid escape format: " + s);
    }

    final String[] dSplit = components[0].split("-");
    final String[] tSplit = timeComponents[0].split(":");
    if (dSplit.length != 3 || tSplit.length != 3) {
      throw new IllegalArgumentException("Invalid escape format: " + s);
    }
    trimLeading0(dSplit);
    trimLeading0(tSplit);

    if (timeComponents[1].startsWith("0")) {
      timeComponents[1] = timeComponents[1].replaceFirst("^00*", "");
      if (timeComponents[1].length() == 0) {
        timeComponents[1] = "0";
      }
    }

    try {
      int yy = Integer.parseInt(dSplit[0]) - 1900;
      int mm = Integer.parseInt(dSplit[1]) - 1;
      int dd = Integer.parseInt(dSplit[2]);

      int hh = Integer.parseInt(tSplit[0]);
      int mi = Integer.parseInt(tSplit[1]);
      int ss = Integer.parseInt(tSplit[2]);
      int ms = Integer.valueOf(timeComponents[1]) / 1000000;

      return new java.sql.Timestamp(Date.UTC(yy, mm, dd, hh, mi, ss) + ms);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid escape format: " + s);
    }
  }

  private static void trimLeading0(final String[] dSplit) {
    for (int i = 0; i < 3; i++) {
      if (dSplit[i].startsWith("0")) {
        dSplit[i] = dSplit[i].substring(1);
      }
    }
  }

  @Override
  public Timestamp fromResult(JavaScriptObject responseObject) {
    return fromJson(PrimitiveResultDeserializers.stringResult(responseObject));
  }
}
