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

import com.google.gwt.core.client.GWT;
import com.google.gwtjsonrpc.common.AsyncCallback;
import com.google.gwtjsonrpc.common.VoidResult;

/** Simple callback to ignore a return value. */
public class VoidCallback implements AsyncCallback<VoidResult> {
  public static final VoidCallback INSTANCE = new VoidCallback();

  protected VoidCallback() {
  }

  @Override
  public void onSuccess(final VoidResult result) {
  }

  @Override
  public void onFailure(final Throwable caught) {
    GWT.log("Error in VoidCallback", caught);
  }
}
