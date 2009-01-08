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

/** Default serialization for a {@link java.sql.Timestamp}. */
public final class JavaSqlTimestamp_JsonSerializer extends
    JsonSerializer<java.sql.Timestamp> {
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
    sb.append(o);
    sb.append('"');
  }

  protected static java.sql.Timestamp parseTimestamp(final String s) {
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

    final java.sql.Date d = JavaSqlDate_JsonSerializer.parseDate(components[0]);
    final java.sql.Time t = parseTime(timeComponents[0]);

    if (timeComponents[1].startsWith("0")) {
      timeComponents[1] = timeComponents[1].replaceFirst("^00*", "");
      if (timeComponents[1].length() == 0) {
        timeComponents[1] = "0";
      }
    }
    final int nanos;
    try {
      nanos = Integer.valueOf(timeComponents[1]);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid escape format: " + s);
    }

    return new java.sql.Timestamp(d.getYear(), d.getMonth(), d.getDate(), t
        .getHours(), t.getMinutes(), t.getSeconds(), nanos);
  }

  private static java.sql.Time parseTime(final String s) {
    final String[] split = s.split(":");
    if (split.length != 3) {
      throw new IllegalArgumentException("Invalid escape format: " + s);
    }
    for (int i = 0; i < 3; i++) {
      if (split[i].startsWith("0")) {
        split[i] = split[i].substring(1);
      }
    }
    try {
      int hh = Integer.parseInt(split[0]);
      int mm = Integer.parseInt(split[1]);
      int ss = Integer.parseInt(split[2]);

      return new java.sql.Time(hh, mm, ss);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid escape format: " + s);
    }
  }
}
