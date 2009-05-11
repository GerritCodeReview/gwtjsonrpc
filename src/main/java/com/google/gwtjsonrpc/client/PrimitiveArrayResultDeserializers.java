package com.google.gwtjsonrpc.client;

import com.google.gwt.core.client.JavaScriptObject;

public class PrimitiveArrayResultDeserializers extends ArrayResultDeserializer {
  public static ResultDeserializer<Boolean[]> BOOLEAN_INSTANCE =
      new ResultDeserializer<Boolean[]>() {
        @Override
        public Boolean[] fromResult(JavaScriptObject responseObject) {
          final Boolean[] tmp = new Boolean[getResultSize(responseObject)];
          PrimitiveArraySerializer.INSTANCE.fromJson(getResult(responseObject),
              tmp);
          return tmp;
        }
      };
  public static ResultDeserializer<Byte[]> BYTE_INSTANCE =
      new ResultDeserializer<Byte[]>() {
        @Override
        public Byte[] fromResult(JavaScriptObject responseObject) {
          final Byte[] tmp = new Byte[getResultSize(responseObject)];
          PrimitiveArraySerializer.INSTANCE.fromJson(getResult(responseObject),
              tmp);
          return tmp;
        }
      };
  public static ResultDeserializer<Character[]> CHARACTER_INSTANCE =
      new ResultDeserializer<Character[]>() {
        @Override
        public Character[] fromResult(JavaScriptObject responseObject) {
          final Character[] tmp = new Character[getResultSize(responseObject)];
          PrimitiveArraySerializer.INSTANCE.fromJson(getResult(responseObject),
              tmp);
          return tmp;
        }
      };
  public static ResultDeserializer<Double[]> DOUBLE_INSTANCE =
      new ResultDeserializer<Double[]>() {
        @Override
        public Double[] fromResult(JavaScriptObject responseObject) {
          final Double[] tmp = new Double[getResultSize(responseObject)];
          PrimitiveArraySerializer.INSTANCE.fromJson(getResult(responseObject),
              tmp);
          return tmp;
        }
      };
  public static ResultDeserializer<Float[]> FLOAT_INSTANCE =
      new ResultDeserializer<Float[]>() {
        @Override
        public Float[] fromResult(JavaScriptObject responseObject) {
          final Float[] tmp = new Float[getResultSize(responseObject)];
          PrimitiveArraySerializer.INSTANCE.fromJson(getResult(responseObject),
              tmp);
          return tmp;
        }
      };
  public static ResultDeserializer<Integer[]> INTEGER_INSTANCE =
      new ResultDeserializer<Integer[]>() {
        @Override
        public Integer[] fromResult(JavaScriptObject responseObject) {
          final Integer[] tmp = new Integer[getResultSize(responseObject)];
          PrimitiveArraySerializer.INSTANCE.fromJson(getResult(responseObject),
              tmp);
          return tmp;
        }
      };
  public static ResultDeserializer<Short[]> SHORT_INSTANCE =
      new ResultDeserializer<Short[]>() {
        @Override
        public Short[] fromResult(JavaScriptObject responseObject) {
          final Short[] tmp = new Short[getResultSize(responseObject)];
          PrimitiveArraySerializer.INSTANCE.fromJson(getResult(responseObject),
              tmp);
          return tmp;
        }
      };
}
