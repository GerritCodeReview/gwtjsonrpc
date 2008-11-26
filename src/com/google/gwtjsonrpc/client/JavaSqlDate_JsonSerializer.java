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

/** Default serialization for a {@link java.sql.Date}. */
public final class JavaSqlDate_JsonSerializer extends
    JsonSerializer<java.sql.Date> {
  public static final JavaSqlDate_JsonSerializer INSTANCE =
      new JavaSqlDate_JsonSerializer();

  @Override
  public java.sql.Date fromJson(final Object o) {
    if (o != null) {
      return java.sql.Date.valueOf((String) o);
    }
    return null;
  }

  @Override
  public void printJson(final StringBuffer sb, final java.sql.Date o) {
    sb.append('"');
    sb.append(o);
    sb.append('"');
  }
}
