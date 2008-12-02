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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

/**
 * Base class for generated {@link RemoteJsonService} implementations.
 * <p>
 * At runtime <code>GWT.create(Foo.class)</code> returns a subclass of this
 * class, implementing the Foo and {@link ServiceDefTarget} interfaces.
 */
public abstract class AbstractJsonProxy implements ServiceDefTarget {
  /** URL of the service implementation. */
  String url;

  /** Current XSRF token associated with this service. */
  String xsrfKey;

  public String getServiceEntryPoint() {
    return url;
  }

  public void setServiceEntryPoint(final String address) {
    url = address;
  }

  protected <T> void doInvoke(final boolean allowXsrf, final String reqData,
      final JsonSerializer<T> ser, final AsyncCallback<T> cb)
      throws InvocationException {
    if (url == null) {
      throw new NoServiceEntryPointSpecifiedException();
    }
    new JsonCall<T>(this, allowXsrf, reqData, ser, cb).send();
  }
}
