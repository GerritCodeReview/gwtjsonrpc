package com.google.gwtjsonrpc.client;

import com.google.gwt.core.client.JavaScriptObject;

public class PrimitiveResultDeserializers {
  static native boolean booleanResult(JavaScriptObject responseObject)
  /*-{
    return responseObject.result;
  }-*/;

  static native byte byteResult(JavaScriptObject responseObject)
  /*-{
    return responseObject.result;
  }-*/;

  static native String stringResult(JavaScriptObject responseObject)
  /*-{
    return responseObject.result;
  }-*/;

  static char charResult(JavaScriptObject responseObject) {
    return JsonSerializer.toChar(stringResult(responseObject));
  }

  static native double doubleResult(JavaScriptObject responseObject)
  /*-{
    return responseObject.result;
  }-*/;

  static native float floatResult(JavaScriptObject responseObject)
  /*-{
    return responseObject.result;
  }-*/;

  static native int intResult(JavaScriptObject responseObject)
  /*-{
    return responseObject.result;
  }-*/;

  static native short shortResult(JavaScriptObject responseObject)
  /*-{
    return responseObject.result;
  }-*/;

  public static ResultDeserializer<Boolean> BOOLEAN_INSTANCE =
      new ResultDeserializer<Boolean>() {
        @Override
        public Boolean fromResult(JavaScriptObject responseObject) {
          return booleanResult(responseObject);
        }
      };
  public static ResultDeserializer<Byte> BYTE_INSTANCE =
      new ResultDeserializer<Byte>() {
        @Override
        public Byte fromResult(JavaScriptObject responseObject) {
          return byteResult(responseObject);
        }
      };
  public static ResultDeserializer<Character> CHARACTER_INSTANCE =
      new ResultDeserializer<Character>() {
        @Override
        public Character fromResult(JavaScriptObject responseObject) {
          return charResult(responseObject);
        }
      };
  public static ResultDeserializer<Double> DOUBLE_INSTANCE =
      new ResultDeserializer<Double>() {
        @Override
        public Double fromResult(JavaScriptObject responseObject) {
          return doubleResult(responseObject);
        }
      };
  public static ResultDeserializer<Float> FLOAT_INSTANCE =
      new ResultDeserializer<Float>() {
        @Override
        public Float fromResult(JavaScriptObject responseObject) {
          return floatResult(responseObject);
        }
      };
  public static ResultDeserializer<Integer> INTEGER_INSTANCE =
      new ResultDeserializer<Integer>() {
        @Override
        public Integer fromResult(JavaScriptObject responseObject) {
          return intResult(responseObject);
        }
      };
  public static ResultDeserializer<Short> SHORT_INSTANCE =
      new ResultDeserializer<Short>() {
        @Override
        public Short fromResult(JavaScriptObject responseObject) {
          return shortResult(responseObject);
        }
      };
}
