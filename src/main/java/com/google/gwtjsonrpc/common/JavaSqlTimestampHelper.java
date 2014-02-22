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

package com.google.gwtjsonrpc.common;

import java.sql.Timestamp;
import java.util.Date;

/** Utility to parse Timestamp from a string. */
public class JavaSqlTimestampHelper {
  public static Timestamp parseTimestamp(String s) {
    String[] components = s.split(" ");
    if (components.length < 1 || components.length > 2) {
      throw new IllegalArgumentException(
          "Expected date and optional time: " + s);
    }
    return components.length == 1
        ? parseDate(components[0])
        : parseTime(components[0], components[1]);
  }

  @SuppressWarnings("deprecation")
  private static Timestamp parseDate(String s) {
    String[] components = s.split("-");
    if (components.length != 3) {
      throw new IllegalArgumentException("Invalid date format: " + s);
    }
    try {
      int yy = Integer.parseInt(components[0]) - 1900;
      int mm = Integer.parseInt(components[1]) - 1;
      int dd = Integer.parseInt(components[2]);
      return new Timestamp(Date.UTC(yy, mm, dd, 00, 00, 00));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid date format: " + s, e);
    }
  }

  @SuppressWarnings("deprecation")
  private static Timestamp parseTime(String date, String time) {
    String[] dSplit = date.split("-");
    if (dSplit.length != 3) {
      throw new IllegalArgumentException("Invalid date format: " + date);
    }
    int yy, mm, dd;
    try {
      yy = Integer.parseInt(dSplit[0]) - 1900;
      mm = Integer.parseInt(dSplit[1]) - 1;
      dd = Integer.parseInt(dSplit[2]);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid date format: " + date, e);
    }

    int hh, mi, ss, ns;
    if (time != null) {
      int p = time.indexOf('.');
      String t;
      double f;
      try {
        if (p >= 0) {
          t = time.substring(0, p);
          f = Double.parseDouble("0." + time.substring(p + 1));
        } else {
          t = time;
          f = 0;
        }
        String[] tSplit = t.split(":");
        if (tSplit.length != 3) {
          throw new IllegalArgumentException("Invalid time format: " + time);
        }
        hh = Integer.parseInt(tSplit[0]);
        mi = Integer.parseInt(tSplit[1]);
        ss = Integer.parseInt(tSplit[2]);
        ns = (int) Math.round(f * 1e9);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid time format: " + time, e);
      }
    } else {
      hh = 0;
      mi = 0;
      ss = 0;
      ns = 0;
    }
    Timestamp result = new Timestamp(Date.UTC(yy, mm, dd, hh, mi, ss));
    result.setNanos(ns);
    return result;
  }

  private JavaSqlTimestampHelper() {
  }
}
