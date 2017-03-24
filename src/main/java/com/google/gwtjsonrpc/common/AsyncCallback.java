// Copyright 2012 Google Inc.
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

/** Invoked with the result (or error) of an RPC. */
public interface AsyncCallback<T> {
  /**
   * Called when an asynchronous call fails to complete normally. {@link
   * com.google.gwt.user.client.rpc.InvocationException}s, or checked exceptions thrown by the
   * service method are examples of the type of failures that can be passed to this method.
   *
   * @param caught failure encountered while executing a remote procedure call
   */
  void onFailure(Throwable caught);

  /**
   * Called when an asynchronous call completes successfully.
   *
   * @param result the return value of the remote produced call
   */
  void onSuccess(T result);
}
