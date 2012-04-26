// Copyright 2012 SAP AG
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

import com.google.gwtjsonrpc.client.impl.JsonSerializer;

public class JavaLangInteger_JsonSerializer extends
    JsonSerializer<java.lang.Integer> {
  public static final JavaLangInteger_JsonSerializer INSTANCE =
      new JavaLangInteger_JsonSerializer();

  @Override
  public Integer fromJson(Object o) {
    if (o == null) {
      return null;
    }
    return (Integer) o;
  }

  @Override
  public void printJson(StringBuilder sb, Integer o) {
    sb.append(o);
  }
}
