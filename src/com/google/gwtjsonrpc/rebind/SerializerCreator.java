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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwtjsonrpc.client.ArraySerializer;
import com.google.gwtjsonrpc.client.EnumSerializer;
import com.google.gwtjsonrpc.client.JavaLangString_JsonSerializer;
import com.google.gwtjsonrpc.client.JavaSqlDate_JsonSerializer;
import com.google.gwtjsonrpc.client.JavaSqlTimestamp_JsonSerializer;
import com.google.gwtjsonrpc.client.JavaUtilDate_JsonSerializer;
import com.google.gwtjsonrpc.client.JsonSerializer;
import com.google.gwtjsonrpc.client.ListSerializer;
import com.google.gwtjsonrpc.client.SetSerializer;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

class SerializerCreator {
  private static final String SER_SUFFIX = "_JsonSerializer";
  private static final Comparator<JField> FIELD_COMP =
      new Comparator<JField>() {
        public int compare(final JField o1, final JField o2) {
          return o1.getName().compareTo(o2.getName());
        }
      };

  private static final HashMap<String, String> defaultSerializers;
  private static final HashMap<String, String> parameterizedSerializers;
  static {
    defaultSerializers = new HashMap<String, String>();
    parameterizedSerializers = new HashMap<String, String>();

    defaultSerializers.put(java.lang.String.class.getCanonicalName(),
        JavaLangString_JsonSerializer.class.getCanonicalName());
    defaultSerializers.put(java.util.Date.class.getCanonicalName(),
        JavaUtilDate_JsonSerializer.class.getCanonicalName());
    defaultSerializers.put(java.sql.Date.class.getCanonicalName(),
        JavaSqlDate_JsonSerializer.class.getCanonicalName());
    defaultSerializers.put(java.sql.Timestamp.class.getCanonicalName(),
        JavaSqlTimestamp_JsonSerializer.class.getCanonicalName());

    parameterizedSerializers.put(java.util.List.class.getCanonicalName(),
        ListSerializer.class.getCanonicalName());
    parameterizedSerializers.put(java.util.Set.class.getCanonicalName(),
        SetSerializer.class.getCanonicalName());
  }

  private final HashMap<String, String> generatedSerializers;
  private final GeneratorContext context;
  private JClassType targetType;

  SerializerCreator(final GeneratorContext c) {
    context = c;
    generatedSerializers = new HashMap<String, String>();
  }

  String create(final JClassType targetType, final TreeLogger logger)
      throws UnableToCompleteException {
    String sClassName = serializerFor(targetType);
    if (sClassName != null) {
      return sClassName;
    }

    checkCanSerialize(logger, targetType);
    recursivelyCreateSerializers(logger, targetType);

    this.targetType = targetType;
    final TypeOracle typeOracle = context.getTypeOracle();
    final SourceWriter srcWriter = getSourceWriter(logger, context);
    final String sn = getSerializerQualifiedName(targetType);
    if (!generatedSerializers.containsKey(targetType.getQualifiedSourceName())) {
      generatedSerializers.put(targetType.getQualifiedSourceName(), sn);
    }
    if (srcWriter == null) {
      return sn;
    }

    if (targetType.isParameterized() == null) {
      generateSingleton(srcWriter);
    }
    if (targetType.isEnum() != null) {
      generateEnumFromJson(srcWriter);
    } else {
      generateInstanceMembers(srcWriter);
      generatePrintJson(srcWriter);
      generateFromJson(srcWriter);
      generateGetSets(srcWriter);
    }

    srcWriter.commit(logger);
    return sn;
  }

  private void recursivelyCreateSerializers(final TreeLogger logger,
      final JType targetType) throws UnableToCompleteException {
    if (targetType.isPrimitive() != null) {
      return;
    }

    for (final JField f : sortFields((JClassType) targetType)) {
      final JType type = f.getType();
      if (isJsonPrimitive(type)) {
        continue;
      }

      if (type.isArray() != null) {
        create((JClassType) type.isArray().getComponentType(), logger);
        continue;
      }

      if (type.isParameterized() != null) {
        final JClassType[] typeArgs = type.isParameterized().getTypeArgs();
        for (final JClassType t : typeArgs) {
          create(t, logger);
        }
      }

      final String qsn = type.getQualifiedSourceName();
      if (defaultSerializers.containsKey(qsn)
          || parameterizedSerializers.containsKey(qsn)) {
        continue;
      }

      create((JClassType) type, logger);
    }
  }

  void checkCanSerialize(final TreeLogger logger, final JType type)
      throws UnableToCompleteException {
    if (type.isPrimitive() == JPrimitiveType.LONG) {
      logger.log(TreeLogger.ERROR,
          "Type 'long' not supported in JSON encoding", null);
      throw new UnableToCompleteException();
    }

    if (type.isPrimitive() == JPrimitiveType.VOID) {
      logger.log(TreeLogger.ERROR,
          "Type 'void' not supported in JSON encoding", null);
      throw new UnableToCompleteException();
    }

    final String qsn = type.getQualifiedSourceName();
    if (type.isEnum() != null) {
      return;
    }

    if (isJsonPrimitive(type)) {
      return;
    }

    if (type.isArray() != null) {
      if (type.isArray().getComponentType().isPrimitive() != null) {
        logger.log(TreeLogger.ERROR,
            "Primitive array not supported in JSON encoding", null);
        throw new UnableToCompleteException();
      }
      checkCanSerialize(logger, type.isArray().getComponentType());
      return;
    }

    if (defaultSerializers.containsKey(qsn)) {
      return;
    }

    if (type.isParameterized() != null) {
      final JClassType[] typeArgs = type.isParameterized().getTypeArgs();
      for (final JClassType t : typeArgs) {
        checkCanSerialize(logger, t);
      }
      if (parameterizedSerializers.containsKey(qsn)) {
        return;
      }
    } else if (parameterizedSerializers.containsKey(qsn)) {
      logger.log(TreeLogger.ERROR,
          "Type " + qsn + " requires type paramter(s)", null);
      throw new UnableToCompleteException();
    }

    if (qsn.startsWith("java.") || qsn.startsWith("javax.")) {
      logger.log(TreeLogger.ERROR, "Standard type " + qsn
          + " not supported in JSON encoding", null);
      throw new UnableToCompleteException();
    }

    if (type.isInterface() != null) {
      logger.log(TreeLogger.ERROR, "Interface " + qsn
          + " not supported in JSON encoding", null);
      throw new UnableToCompleteException();
    }

    final JClassType ct = (JClassType) type;
    final TreeLogger branch = logger.branch(TreeLogger.DEBUG, "In type " + qsn);
    for (final JField f : sortFields(ct)) {
      checkCanSerialize(branch, f.getType());
    }
  }

  String serializerFor(final JClassType t) {
    if (t.isArray() != null) {
      return ArraySerializer.class.getCanonicalName();
    }

    final String qsn = t.getQualifiedSourceName();
    if (defaultSerializers.containsKey(qsn)) {
      return defaultSerializers.get(qsn);
    }

    if (parameterizedSerializers.containsKey(qsn)) {
      return parameterizedSerializers.get(qsn);
    }

    return generatedSerializers.get(qsn);
  }

  private void generateSingleton(final SourceWriter w) {
    w.print("public static final ");
    w.print(getSerializerSimpleName());
    w.print(" INSTANCE = new ");
    w.print(getSerializerSimpleName());
    w.println("();");
    w.println();
  }

  private void generateInstanceMembers(final SourceWriter w) {
    for (final JField f : sortFields(targetType)) {
      final JParameterizedType pt = f.getType().isParameterized();
      if (pt == null) {
        continue;
      }

      final String serType = serializerFor(pt);
      w.print("private final ");
      w.print(serType);
      w.print(" ");
      w.print("ser_" + f.getName());
      w.print(" = ");
      generateSerializerReference(pt, w);
      w.println(";");
    }
    w.println();
  }

  void generateSerializerReference(final JParameterizedType type,
      final SourceWriter w) {
    w.print("new " + serializerFor(type) + "(");
    boolean first = true;
    for (final JClassType t : type.isParameterized().getTypeArgs()) {
      if (first) {
        first = false;
      } else {
        w.print(", ");
      }

      if (t.isParameterized() == null) {
        w.print(serializerFor(t) + ".INSTANCE");
      } else {
        generateSerializerReference(t.isParameterized(), w);
      }
    }
    w.print(")");
  }

  private void generateGetSets(final SourceWriter w) {
    for (final JField f : sortFields(targetType)) {
      if (f.isPrivate()) {
        w.print("private static final native ");
        w.print(f.getType().getQualifiedSourceName());
        w.print(" objectGet_" + f.getName());
        w.print("(");
        w.print(targetType.getQualifiedSourceName() + " instance");
        w.print(")");
        w.println("/*-{ ");
        w.indent();

        w.print("return instance.@");
        w.print(targetType.getQualifiedSourceName());
        w.print("::");
        w.print(f.getName());
        w.println(";");

        w.outdent();
        w.println("}-*/;");

        w.print("private static final native void ");
        w.print(" objectSet_" + f.getName());
        w.print("(");
        w.print(targetType.getQualifiedSourceName() + " instance, ");
        w.print(f.getType().getQualifiedSourceName() + " value");
        w.print(")");
        w.println("/*-{ ");
        w.indent();

        w.print("instance.@");
        w.print(targetType.getQualifiedSourceName());
        w.print("::");
        w.print(f.getName());
        w.println(" = value;");

        w.outdent();
        w.println("}-*/;");
      }

      if (f.getType() == JPrimitiveType.CHAR) {
        w.print("private static final native String");
        w.print(" jsonGet0_" + f.getName());
        w.print("(final JavaScriptObject instance)");
        w.println("/*-{ ");
        w.indent();
        w.print("return instance.");
        w.print(f.getName());
        w.println(";");
        w.outdent();
        w.println("}-*/;");

        w.print("private static final char");
        w.print(" jsonGet_" + f.getName());
        w.print("(JavaScriptObject instance)");
        w.println(" {");
        w.indent();
        w.print("return ");
        w.print(JsonSerializer.class.getName());
        w.print(".toChar(");
        w.print("jsonGet0_" + f.getName());
        w.println("(instance));");
        w.outdent();
        w.println("}");
      } else {
        w.print("private static final native ");
        if (isJsonPrimitive(f.getType())) {
          w.print(f.getType().getQualifiedSourceName());
        } else {
          w.print("Object");
        }
        w.print(" jsonGet_" + f.getName());
        w.print("(JavaScriptObject instance)");
        w.println("/*-{ ");
        w.indent();

        w.print("return instance.");
        w.print(f.getName());
        w.println(";");

        w.outdent();
        w.println("}-*/;");
      }

      w.println();
    }
  }

  private void generateEnumFromJson(final SourceWriter w) {
    w.print("public ");
    w.print(targetType.getQualifiedSourceName());
    w.println(" fromJson(Object in) {");
    w.indent();
    w.print("return in != null");
    w.print(" ? " + targetType.getQualifiedSourceName()
        + ".valueOf((String)in)");
    w.print(" : null");
    w.println(";");
    w.outdent();
    w.println("}");
    w.println();
  }

  private void generatePrintJson(final SourceWriter w) {
    final JField[] fieldList = sortFields(targetType);
    w.print("public void printJson(StringBuffer sb, ");
    w.print(targetType.getQualifiedSourceName());
    w.println(" src) {");
    w.indent();

    final String docomma;
    if (fieldList.length > 1) {
      w.println("int fieldCount = -1;");
      docomma = "if (++fieldCount > 0) sb.append(\",\");";
    } else {
      docomma = "";
    }

    w.println("sb.append(\"{\");");

    for (final JField f : fieldList) {
      final String doget;
      if (f.isPrivate()) {
        doget = "objectGet_" + f.getName() + "(src)";
      } else {
        doget = "src." + f.getName();
      }

      final String doname = "sb.append(\"\\\"" + f.getName() + "\\\":\");";
      if (isJsonString(f.getType())) {
        w.println("if (" + doget + " != null) {");
        w.indent();
        w.println(docomma);
        w.println(doname);
        w.println("sb.append(" + JsonSerializer.class.getSimpleName()
            + ".escapeString(" + doget + "));");
        w.outdent();
        w.println("}");
        w.println();
      } else if (isJsonPrimitive(f.getType())) {
        w.println(docomma);
        w.println(doname);
        w.println("sb.append(" + doget + ");");
        w.println();
      } else {
        w.println("if (" + doget + " != null) {");
        w.indent();
        w.println(docomma);
        w.println(doname);
        if (f.getType().isParameterized() != null) {
          w.print("ser_" + f.getName());
        } else {
          w.print(serializerFor((JClassType) f.getType()) + ".INSTANCE");
        }
        w.println(".printJson(sb, " + doget + ");");
        w.outdent();
        w.println("}");
        w.println();
      }
    }

    w.println("sb.append(\"}\");");
    w.outdent();
    w.println("}");
    w.println();
  }

  private void generateFromJson(final SourceWriter w) {
    w.print("public ");
    w.print(targetType.getQualifiedSourceName());
    w.println(" fromJson(Object in) {");
    w.indent();

    w.println("if (in == null) return null;");
    w.println("final JavaScriptObject jso = (JavaScriptObject)in;");
    w.print("final ");
    w.print(targetType.getQualifiedSourceName());
    w.print(" dst = new ");
    w.println(targetType.getQualifiedSourceName() + "();");

    for (final JField f : sortFields(targetType)) {
      final String doget = "jsonGet_" + f.getName() + "(jso)";
      final String doset0, doset1;

      if (f.isPrivate()) {
        doset0 = "objectSet_" + f.getName() + "(dst, ";
        doset1 = ")";
      } else {
        doset0 = "dst." + f.getName() + " = ";
        doset1 = "";
      }

      if (isJsonPrimitive(f.getType())) {
        w.print(doset0);
        w.print(doget);
        w.print(doset1);
        w.println(";");
      } else {
        w.print(doset0);
        if (f.getType().isParameterized() != null) {
          w.print("ser_" + f.getName());
        } else {
          w.print(serializerFor((JClassType) f.getType()) + ".INSTANCE");
        }
        w.print(".fromJson(" + doget + ")");
        w.print(doset1);
        w.println(";");
      }
    }

    w.println("return dst;");
    w.outdent();
    w.println("}");
    w.println();
  }

  static boolean isJsonPrimitive(final JType t) {
    return t.isPrimitive() != null || isJsonString(t);
  }

  static boolean isJsonString(final JType t) {
    return t.getQualifiedSourceName().equals(String.class.getCanonicalName());
  }

  private SourceWriter getSourceWriter(final TreeLogger logger,
      final GeneratorContext ctx) {
    final JPackage targetPkg = targetType.getPackage();
    final String pkgName = targetPkg == null ? "" : targetPkg.getName();
    final PrintWriter pw;
    final ClassSourceFileComposerFactory cf;

    pw = ctx.tryCreate(logger, pkgName, getSerializerSimpleName());
    if (pw == null) {
      return null;
    }

    cf = new ClassSourceFileComposerFactory(pkgName, getSerializerSimpleName());
    cf.addImport(JavaScriptObject.class.getCanonicalName());
    cf.addImport(JsonSerializer.class.getCanonicalName());
    if (targetType.isEnum() != null) {
      cf.addImport(EnumSerializer.class.getCanonicalName());
      cf.setSuperclass(EnumSerializer.class.getSimpleName() + "<"
          + targetType.getQualifiedSourceName() + ">");
    } else {
      cf.setSuperclass(JsonSerializer.class.getSimpleName() + "<"
          + targetType.getQualifiedSourceName() + ">");
    }
    return cf.createSourceWriter(ctx, pw);
  }

  private String getSerializerQualifiedName(final JClassType targetType) {
    final String[] name;
    name = ProxyCreator.synthesizeTopLevelClassName(targetType, SER_SUFFIX);
    return name[0].length() == 0 ? name[1] : name[0] + "." + name[1];
  }

  private String getSerializerSimpleName() {
    return ProxyCreator.synthesizeTopLevelClassName(targetType, SER_SUFFIX)[1];
  }

  private static JField[] sortFields(final JClassType targetType) {
    final ArrayList<JField> r = new ArrayList<JField>();
    for (final JField f : targetType.getFields()) {
      if (!f.isStatic() && !f.isTransient() && !f.isFinal()) {
        r.add(f);
      }
    }
    Collections.sort(r, FIELD_COMP);
    return r.toArray(new JField[r.size()]);
  }
}
