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

package com.google.gwtjsonrpc.rebind;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.generator.NameFactory;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwtjsonrpc.client.AbstractJsonProxy;
import com.google.gwtjsonrpc.client.CallbackHandle;
import com.google.gwtjsonrpc.client.JsonSerializer;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

class ProxyCreator {
  private static final String PROXY_SUFFIX = "_JsonProxy";
  private JClassType svcInf;
  private JClassType asyncCallbackClass;
  private SerializerCreator serializerCreator;
  private int instanceField;

  ProxyCreator(final JClassType remoteService) {
    svcInf = remoteService;
  }

  String create(final TreeLogger logger, final GeneratorContext context)
      throws UnableToCompleteException {
    serializerCreator = new SerializerCreator(context);
    final TypeOracle typeOracle = context.getTypeOracle();
    try {
      asyncCallbackClass = typeOracle.getType(AsyncCallback.class.getName());
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, null, e);
      throw new UnableToCompleteException();
    }
    checkMethods(logger, context);

    final SourceWriter srcWriter = getSourceWriter(logger, context);
    if (srcWriter == null) {
      return getProxyQualifiedName();
    }

    generateProxyMethods(logger, srcWriter);
    srcWriter.commit(logger);

    return getProxyQualifiedName();
  }

  private void checkMethods(final TreeLogger logger,
      final GeneratorContext context) throws UnableToCompleteException {
    final Set<String> declaredNames = new HashSet<String>();
    final JMethod[] methodList = svcInf.getOverridableMethods();
    for (final JMethod m : methodList) {
      if (!declaredNames.add(m.getName())) {
        invalid(logger, "Overloading method " + m.getName() + " not supported");
      }

      if (m.getReturnType() != JPrimitiveType.VOID && !returnsCallbackHandle(m)) {
        invalid(logger, "Method " + m.getName() + " must return void or "
            + CallbackHandle.class);
      }

      final JParameter[] params = m.getParameters();
      if (params.length == 0) {
        invalid(logger, "Method " + m.getName() + " requires "
            + AsyncCallback.class.getName() + " as last parameter");
      }

      final JParameter callback = params[params.length - 1];
      if (!callback.getType().getErasedType().getQualifiedSourceName().equals(
          asyncCallbackClass.getQualifiedSourceName())) {
        invalid(logger, "Method " + m.getName() + " requires "
            + AsyncCallback.class.getName() + " as last parameter");
      }
      if (callback.getType().isParameterized() == null) {
        invalid(logger, "Callback " + callback.getName()
            + " must have a type parameter");
      }

      final JClassType resultType =
          callback.getType().isParameterized().getTypeArgs()[0];

      if (returnsCallbackHandle(m)) {
        if (params.length != 1) {
          invalid(logger, "Method " + m.getName()
              + " must not accept parameters");
        }

        final JClassType rt = m.getReturnType().isClass();
        if (rt.isParameterized() == null) {
          invalid(logger, "CallbackHandle return value of " + m.getName()
              + " must have a type parameter");
        }
        if (!resultType.getQualifiedSourceName().equals(
            rt.isParameterized().getTypeArgs()[0].getQualifiedSourceName())) {
          invalid(logger, "CallbackHandle return value of " + m.getName()
              + " must match type with AsyncCallback parameter");
        }
      }

      for (int i = 0; i < params.length - 1; i++) {
        final JParameter p = params[i];
        final TreeLogger branch =
            logger.branch(TreeLogger.DEBUG, m.getName() + ", parameter "
                + p.getName());
        serializerCreator.checkCanSerialize(branch, p.getType());
        if (p.getType().isPrimitive() == null) {
          serializerCreator.create((JClassType) p.getType(), branch);
        }
      }

      final TreeLogger branch =
          logger.branch(TreeLogger.DEBUG, m.getName() + ", result "
              + resultType.getQualifiedSourceName());
      serializerCreator.checkCanSerialize(branch, resultType);
      serializerCreator.create(resultType, branch);
    }
  }

  private boolean returnsCallbackHandle(final JMethod m) {
    return m.getReturnType().getErasedType().getQualifiedSourceName().equals(
        CallbackHandle.class.getName());
  }

  private void invalid(final TreeLogger logger, final String what)
      throws UnableToCompleteException {
    logger.log(TreeLogger.ERROR, what, null);
    throw new UnableToCompleteException();
  }

  private SourceWriter getSourceWriter(final TreeLogger logger,
      final GeneratorContext ctx) {
    final JPackage servicePkg = svcInf.getPackage();
    final String pkgName = servicePkg == null ? "" : servicePkg.getName();
    final PrintWriter pw;
    final ClassSourceFileComposerFactory cf;

    pw = ctx.tryCreate(logger, pkgName, getProxySimpleName());
    if (pw == null) {
      return null;
    }

    cf = new ClassSourceFileComposerFactory(pkgName, getProxySimpleName());
    cf.addImport(AbstractJsonProxy.class.getCanonicalName());
    cf.addImport(JsonSerializer.class.getCanonicalName());
    cf.setSuperclass(AbstractJsonProxy.class.getSimpleName());
    cf.addImplementedInterface(svcInf.getErasedType().getQualifiedSourceName());
    return cf.createSourceWriter(ctx, pw);
  }

  private void generateProxyMethods(final TreeLogger logger,
      final SourceWriter srcWriter) throws UnableToCompleteException {
    final JMethod[] methodList = svcInf.getOverridableMethods();
    for (final JMethod m : methodList) {
      generateProxyMethod(logger, m, srcWriter);
    }
  }

  private void generateProxyMethod(final TreeLogger logger,
      final JMethod method, final SourceWriter w)
      throws UnableToCompleteException {
    final JParameter[] params = method.getParameters();
    final JParameter callback = params[params.length - 1];
    final JClassType resultType =
        callback.getType().isParameterized().getTypeArgs()[0];
    final String[] serializerFields = new String[params.length];

    w.println();
    for (int i = 0; i < params.length - 1; i++) {
      final JType pType = params[i].getType();
      if (pType.isParameterized() != null) {
        serializerFields[i] = "serializer_" + instanceField++;
        w.print("private static final ");
        w.print(JsonSerializer.class.getName());
        w.print(" ");
        w.print(serializerFields[i]);
        w.print(" = ");
        serializerCreator.generateSerializerReference(pType.isParameterized(),
            w);
        w.println(";");
      }
    }
    if (resultType.isParameterized() != null) {
      serializerFields[params.length - 1] = "serializer_" + instanceField++;
      w.print("private static final ");
      w.print(JsonSerializer.class.getName());
      w.print(" ");
      w.print(serializerFields[params.length - 1]);
      w.print(" = ");
      serializerCreator.generateSerializerReference(resultType
          .isParameterized(), w);
      w.println(";");
    }

    w.print("public ");
    w.print(method.getReturnType().getQualifiedSourceName());
    w.print(" ");
    w.print(method.getName());
    w.print("(");
    boolean needsComma = false;
    final NameFactory nameFactory = new NameFactory();
    for (int i = 0; i < params.length; i++) {
      final JParameter param = params[i];

      if (needsComma) {
        w.print(", ");
      } else {
        needsComma = true;
      }

      final JType paramType = param.getType().getErasedType();
      w.print(paramType.getQualifiedSourceName());
      w.print(" ");

      nameFactory.addName(param.getName());
      w.print(param.getName());
    }

    w.println(") {");
    w.indent();

    if (returnsCallbackHandle(method)) {
      w.print("return new ");
      w.print(CallbackHandle.class.getName());
      w.print("(");
      if (resultType.isParameterized() != null) {
        w.print(serializerFields[params.length - 1]);
      } else {
        w.print(serializerCreator.create(resultType, logger) + ".INSTANCE");
      }
      w.print(", " + callback.getName());
      w.println(");");
      w.outdent();
      w.println("}");
      return;
    }

    final String rVersion = "\\\"version\\\":\\\"1.1\\\"";
    final String rMethod = "\\\"method\\\":\\\"" + method.getName() + "\\\"";
    final String reqDataStr;
    if (params.length == 1) {
      reqDataStr = "\"{" + rVersion + "," + rMethod + "}\"";
    } else {
      final String reqData = nameFactory.createName("reqData");
      w.println("final StringBuffer " + reqData + " = new StringBuffer();");
      w.println(reqData + ".append(\"{" + rVersion + "," + rMethod
          + ",\\\"params\\\":[\");");

      needsComma = false;
      for (int i = 0; i < params.length - 1; i++) {
        if (needsComma) {
          w.println(reqData + ".append(\",\");");
        } else {
          needsComma = true;
        }

        final JType pType = params[i].getType();
        final String pName = params[i].getName();
        if (SerializerCreator.isJsonPrimitive(pType)
            && !SerializerCreator.isJsonString(pType)) {
          w.println(reqData + ".append(" + pName + ");");
        } else {
          w.println("if (" + pName + " != null) {");
          w.indent();
          if (pType.isParameterized() != null) {
            w.print(serializerFields[i]);
          } else {
            w.print(serializerCreator.create((JClassType) pType, logger)
                + ".INSTANCE");
          }
          w.println(".printJson(" + reqData + ", " + pName + ");");
          w.outdent();
          w.println("} else {");
          w.indent();
          w.println(reqData + ".append(" + JsonSerializer.class.getName()
              + ".JS_NULL);");
          w.outdent();
          w.println("}");
        }
      }
      w.println(reqData + ".append(\"]}\");");
      reqDataStr = reqData + ".toString()";
    }

    w.print("doInvoke(");
    w.print(reqDataStr);
    w.print(", ");
    if (resultType.isParameterized() != null) {
      w.print(serializerFields[params.length - 1]);
    } else {
      w.print(serializerCreator.create(resultType, logger) + ".INSTANCE");
    }
    w.print(", " + callback.getName());
    w.println(");");

    w.outdent();
    w.println("}");
  }

  private String getProxyQualifiedName() {
    final String[] name = synthesizeTopLevelClassName(svcInf, PROXY_SUFFIX);
    return name[0].length() == 0 ? name[1] : name[0] + "." + name[1];
  }

  private String getProxySimpleName() {
    return synthesizeTopLevelClassName(svcInf, PROXY_SUFFIX)[1];
  }

  static String[] synthesizeTopLevelClassName(JClassType type, String suffix) {
    // Gets the basic name of the type. If it's a nested type, the type name
    // will contains dots.
    //
    String className;
    String packageName;

    JType leafType = type.getLeafType();
    if (leafType.isPrimitive() != null) {
      className = leafType.getSimpleSourceName();
      packageName = "";
    } else {
      JClassType classOrInterface = leafType.isClassOrInterface();
      assert (classOrInterface != null);
      className = classOrInterface.getName();
      packageName = classOrInterface.getPackage().getName();
    }

    JArrayType isArray = type.isArray();
    if (isArray != null) {
      className += "_Array_Rank_" + isArray.getRank();
    }

    // Add the meaningful suffix.
    //
    className += suffix;

    // Make it a top-level name.
    //
    className = className.replace('.', '_');

    return new String[] {packageName, className};
  }
}
