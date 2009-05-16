// Copyright 2009 Google Inc.
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


/** Event received by {@link RpcCompleteHandler} */
public class RpcCompleteEvent extends BaseRpcEvent<RpcCompleteHandler> {
  private static final Type<RpcCompleteHandler> TYPE =
      new Type<RpcCompleteHandler>();
  static final RpcCompleteEvent e = new RpcCompleteEvent();

  public static Type<RpcCompleteHandler> getType() {
    return TYPE;
  }

  @Override
  public Type<RpcCompleteHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(final RpcCompleteHandler handler) {
    handler.onRpcComplete(this);
  }
}
