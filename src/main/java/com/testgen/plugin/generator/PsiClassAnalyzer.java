package com.testgen.plugin.generator;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import com.testgen.plugin.model.ServiceClassInfo;
import com.testgen.plugin.model.ServiceClassInfo.*;

import java.util.*;

/**
 * Reads a PsiClass and extracts everything the test generator needs:
 *  - injected dependencies (@Autowired fields OR constructor params)
 *  - all public non-static methods
 *  - method parameters and thrown exceptions
 */
public class PsiClassAnalyzer {

    /**
     * Entry point. Returns null if the class is not a valid service
     * (e.g. it's an interface or annotation).
     */
    public ServiceClassInfo analyze(PsiClass psiClass) {
        if (psiClass == null || psiClass.isInterface() || psiClass.isAnnotationType()) {
            return null;
        }

        String packageName = getPackageName(psiClass);
        String className   = psiClass.getName();

        List<FieldInfo>  injectedFields = extractInjectedFields(psiClass);
        List<MethodInfo> publicMethods  = extractPublicMethods(psiClass);

        // If no @Autowired fields found, try constructor injection
        if (injectedFields.isEmpty()) {
            injectedFields = extractConstructorInjectedFields(psiClass);
        }

        return new ServiceClassInfo(packageName, className, injectedFields, publicMethods);
    }

    // ── Package ────────────────────────────────────────────────────────────

    private String getPackageName(PsiClass psiClass) {
        PsiFile file = psiClass.getContainingFile();
        if (file instanceof PsiJavaFile) {
            return ((PsiJavaFile) file).getPackageName();
        }
        return "";
    }

    // ── Injected fields (@Autowired / @Inject / @Resource) ────────────────

    private List<FieldInfo> extractInjectedFields(PsiClass psiClass) {
        List<FieldInfo> fields = new ArrayList<>();
        for (PsiField field : psiClass.getFields()) {
            if (isInjected(field)) {
                String typeName  = getSimpleTypeName(field.getType());
                String fieldName = field.getName();
                fields.add(new FieldInfo(typeName, fieldName));
            }
        }
        return fields;
    }

    private boolean isInjected(PsiField field) {
        for (PsiAnnotation ann : field.getAnnotations()) {
            String qName = ann.getQualifiedName();
            if (qName != null && (
                    qName.equals("org.springframework.beans.factory.annotation.Autowired") ||
                    qName.equals("javax.inject.Inject") ||
                    qName.equals("jakarta.inject.Inject") ||
                    qName.equals("javax.annotation.Resource"))) {
                return true;
            }
        }
        return false;
    }

    // ── Constructor injection ──────────────────────────────────────────────

    private List<FieldInfo> extractConstructorInjectedFields(PsiClass psiClass) {
        List<FieldInfo> fields = new ArrayList<>();

        // Find the largest constructor (most likely the injected one)
        PsiMethod bestCtor = null;
        for (PsiMethod method : psiClass.getConstructors()) {
            if (bestCtor == null ||
                    method.getParameterList().getParametersCount() >
                    bestCtor.getParameterList().getParametersCount()) {
                bestCtor = method;
            }
        }

        if (bestCtor != null) {
            for (PsiParameter param : bestCtor.getParameterList().getParameters()) {
                String typeName  = getSimpleTypeName(param.getType());
                String fieldName = param.getName();
                fields.add(new FieldInfo(typeName, fieldName));
            }
        }
        return fields;
    }

    // ── Public methods ─────────────────────────────────────────────────────

    private List<MethodInfo> extractPublicMethods(PsiClass psiClass) {
        List<MethodInfo> methods = new ArrayList<>();

        for (PsiMethod method : psiClass.getMethods()) {
            // Skip constructors, private, static, and Object methods
            if (method.isConstructor()) continue;
            if (!method.hasModifierProperty(PsiModifier.PUBLIC)) continue;
            if (method.hasModifierProperty(PsiModifier.STATIC)) continue;
            if (isObjectMethod(method.getName())) continue;

            String returnType = getReturnTypeString(method);
            boolean isVoid    = returnType.equals("void");

            List<ParamInfo> params = extractParams(method);
            List<String> exceptions = extractExceptions(method);

            methods.add(new MethodInfo(
                method.getName(), returnType, isVoid, params, exceptions
            ));
        }
        return methods;
    }

    private boolean isObjectMethod(String name) {
        return Set.of("toString", "hashCode", "equals", "clone",
                      "finalize", "getClass", "notify", "notifyAll", "wait")
                  .contains(name);
    }

    private String getReturnTypeString(PsiMethod method) {
        PsiType returnType = method.getReturnType();
        if (returnType == null || PsiTypes.voidType().equals(returnType)) return "void";
        return getSimpleTypeName(returnType);
    }

    private List<ParamInfo> extractParams(PsiMethod method) {
        List<ParamInfo> params = new ArrayList<>();
        for (PsiParameter param : method.getParameterList().getParameters()) {
            params.add(new ParamInfo(
                getSimpleTypeName(param.getType()),
                param.getName()
            ));
        }
        return params;
    }

    private List<String> extractExceptions(PsiMethod method) {
        List<String> exceptions = new ArrayList<>();
        for (PsiClassType ex : method.getThrowsList().getReferencedTypes()) {
            exceptions.add(ex.getClassName());
        }
        return exceptions;
    }

    // ── Utility ────────────────────────────────────────────────────────────

    /**
     * Returns the simple type name without package, preserving generics.
     * e.g. java.util.List<com.example.User> → List<User>
     */
    private String getSimpleTypeName(PsiType type) {
        String canonical = type.getCanonicalText();
        // Strip package names from all segments
        return canonical.replaceAll("\\b[a-z][a-zA-Z0-9_]*(?:\\.[a-z][a-zA-Z0-9_]*)*\\.([A-Z])", "$1");
    }
}
