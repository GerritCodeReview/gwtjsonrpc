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

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Default serialization for any Object[] sort of type.
 * <p>
 * Primitive array types (like <code>int[]</code>) are not supported.
 */
public class ArraySerializer<T> extends JsonSerializer<T[]> {
  private final JsonSerializer<T> serializer;

  public ArraySerializer(final JsonSerializer<T> s) {
    serializer = s;
  }

  @Override
  public void printJson(final StringBuffer sb, final T[] o) {
    sb.append('[');
    for (int i = 0, n = o.length; i < n; i++) {
      if (i > 0) {
        sb.append(',');
      }
      final T v = o[i];
      if (v != null) {
        serializer.printJson(sb, v);
      } else {
        sb.append(JS_NULL);
      }
    }
    sb.append(']');
  }

  @Override
  public T[] fromJson(final Object o) {
    if (o == null) {
      return null;
    }

    final JavaScriptObject jso = (JavaScriptObject) o;
    final int n = size(jso);
    final T[] r = ArraySerializer.<T> newArray(n);
    for (int i = 0; i < n; i++) {
      r[i] = serializer.fromJson(get(jso, i));
    }
    return r;
  }

  @SuppressWarnings("unchecked")
  private static final <T> T[] newArray(final int sz) {
    return (T[]) new Object[sz];
  }

  private static final native int size(JavaScriptObject o)/*-{ return o.length; }-*/;

  private static final native JavaScriptObject get(JavaScriptObject o, int i)/*-{ return o[i]; }-*/;
}
