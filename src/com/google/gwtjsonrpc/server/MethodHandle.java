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

import com.google.gwt.user.client.rpc.AsyncCallback;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Pairing of a specific {@link JsonServlet} implementation and method.
 */
public class MethodHandle {
  private final JsonServlet imp;
  private final Method method;
  private final Class<?>[] parameterTypes;

  /**
   * Create a new handle for a specific service implementation and method.
   * 
   * @param imp instance of the service all calls will be made on.
   * @param method Java method to invoke on <code>imp</code>. The last parameter
   *        of the method must accept an {@link AsyncCallback} and the method
   *        must return void.
   */
  MethodHandle(final JsonServlet imp, final Method method) {
    this.imp = imp;
    this.method = method;

    final Class<?>[] args = method.getParameterTypes();
    parameterTypes = new Class<?>[args.length - 1];
    System.arraycopy(args, 0, parameterTypes, 0, parameterTypes.length);
  }

  /**
   * @return unique name of the method within the service.
   */
  public String getName() {
    return method.getName();
  }

  /**
   * @return true if this method requires positional arguments.
   */
  public Class<?>[] getParamTypes() {
    return parameterTypes;
  }

  /**
   * Invoke this method with the specified arguments, updating the callback.
   * 
   * @param arguments arguments to the method. May be the empty array if no
   *        parameters are declared beyond the AsyncCallback, but must not be
   *        null.
   * @param callback the callback the implementation will invoke onSuccess or
   *        onFailure on as it performs its work. Only the last onSuccess or
   *        onFailure invocation matters.
   */
  public void invoke(final Object[] arguments,
      final AsyncCallback<Object> callback) {
    try {
      final Object[] p = new Object[arguments.length + 1];
      System.arraycopy(arguments, 0, p, 0, arguments.length);
      p[p.length - 1] = callback;
      method.invoke(imp, p);
    } catch (IllegalAccessException e) {
      callback.onFailure(e);
    } catch (InvocationTargetException e) {
      callback.onFailure(e);
    } catch (RuntimeException e) {
      callback.onFailure(e);
    } catch (Error e) {
      callback.onFailure(e);
    }
  }
}
