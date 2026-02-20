package com.testgen.plugin.model;

import java.util.List;

/**
 * Represents all extracted info from a Java service class,
 * ready to be handed to the test generator.
 */
public class ServiceClassInfo {

    private final String packageName;
    private final String className;
    private final List<FieldInfo> injectedFields;   // @Autowired / constructor-injected deps
    private final List<MethodInfo> publicMethods;

    public ServiceClassInfo(String packageName, String className,
                            List<FieldInfo> injectedFields,
                            List<MethodInfo> publicMethods) {
        this.packageName    = packageName;
        this.className      = className;
        this.injectedFields = injectedFields;
        this.publicMethods  = publicMethods;
    }

    public String getPackageName()            { return packageName; }
    public String getClassName()              { return className; }
    public List<FieldInfo> getInjectedFields(){ return injectedFields; }
    public List<MethodInfo> getPublicMethods(){ return publicMethods; }

    // ── Nested: a single injected dependency ──────────────────────────────
    public static class FieldInfo {
        private final String typeName;   // e.g. "UserRepository"
        private final String fieldName;  // e.g. "userRepository"

        public FieldInfo(String typeName, String fieldName) {
            this.typeName  = typeName;
            this.fieldName = fieldName;
        }

        public String getTypeName()  { return typeName; }
        public String getFieldName() { return fieldName; }
    }

    // ── Nested: a single public method ────────────────────────────────────
    public static class MethodInfo {
        private final String methodName;
        private final String returnType;      // "void", "User", "List<String>", etc.
        private final boolean isVoid;
        private final List<ParamInfo> params;
        private final List<String> thrownExceptions;  // e.g. ["UserNotFoundException"]

        public MethodInfo(String methodName, String returnType,
                          boolean isVoid, List<ParamInfo> params,
                          List<String> thrownExceptions) {
            this.methodName       = methodName;
            this.returnType       = returnType;
            this.isVoid           = isVoid;
            this.params           = params;
            this.thrownExceptions = thrownExceptions;
        }

        public String getMethodName()             { return methodName; }
        public String getReturnType()             { return returnType; }
        public boolean isVoid()                   { return isVoid; }
        public List<ParamInfo> getParams()        { return params; }
        public List<String> getThrownExceptions() { return thrownExceptions; }
    }

    // ── Nested: one method parameter ──────────────────────────────────────
    public static class ParamInfo {
        private final String typeName;
        private final String paramName;

        public ParamInfo(String typeName, String paramName) {
            this.typeName  = typeName;
            this.paramName = paramName;
        }

        public String getTypeName()  { return typeName; }
        public String getParamName() { return paramName; }
    }
}
