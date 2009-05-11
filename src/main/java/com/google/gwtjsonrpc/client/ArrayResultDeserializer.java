package com.google.gwtjsonrpc.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Base class for the {@link PrimitiveArrayResultDeserializer} and generated
 * object array result deserializers.
 */
public abstract class ArrayResultDeserializer {
  protected static native JavaScriptObject getResult(JavaScriptObject responseObject)
  /*-{
    return responseObject.result;
  }-*/;

  protected static native int getResultSize(JavaScriptObject responseObject)
  /*-{
    return responseObject.result.length;
  }-*/;
}
