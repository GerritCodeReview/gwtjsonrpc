package com.google.gwtjsonrpc.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Inteface class for deserializers of results from JSON RPC calls. Since
 * primitive and array results need to be handled specially, not all results can
 * be deserialized using the standard object serializers.
 *   
 * @author Gert Scholten
 * @param <T> the result type of an RPC call.
 */
public interface ResultDeserializer<T> {
  public T fromResult(JavaScriptObject responseObject);
}
