package com.testgen.plugin.generator;

import com.testgen.plugin.model.ServiceClassInfo;
import com.testgen.plugin.model.ServiceClassInfo.*;
import com.testgen.plugin.settings.TestGeneratorSettings;

import java.util.*;

/**
 * Pure Java string-builder that produces a complete JUnit 5 + Mockito
 * test class from a ServiceClassInfo. No PSI dependency — easy to unit test.
 *
 * Output example for UserService:
 * ─────────────────────────────────────────────────────────────────
 * @ExtendWith(MockitoExtension.class)
 * class UserServiceTest {
 *
 *     @Mock
 *     private UserRepository userRepository;
 *
 *     @InjectMocks
 *     private UserService userService;
 *
 *     @BeforeEach
 *     void setUp() { MockitoAnnotations.openMocks(this); }
 *
 *     @Test
 *     void findById_shouldReturnUser_whenUserExists() {
 *         // Arrange
 *         Long id = 1L;
 *         User mockUser = mock(User.class);
 *         when(userRepository.findById(id)).thenReturn(Optional.of(mockUser));
 *
 *         // Act
 *         User result = userService.findById(id);
 *
 *         // Assert
 *         assertNotNull(result);
 *         verify(userRepository).findById(id);
 *     }
 * }
 */
public class TestCodeGenerator {

    private final TestGeneratorSettings settings;

    public TestCodeGenerator(TestGeneratorSettings settings) {
        this.settings = settings;
    }

    public String generate(ServiceClassInfo info) {
        StringBuilder sb = new StringBuilder();

        appendPackage(sb, info);
        appendImports(sb, info);
        appendClassDeclaration(sb, info);
        appendFields(sb, info);
        appendSetUp(sb);
        appendTestMethods(sb, info);
        sb.append("}\n");

        return sb.toString();
    }

    // ── Package ────────────────────────────────────────────────────────────

    private void appendPackage(StringBuilder sb, ServiceClassInfo info) {
        if (!info.getPackageName().isEmpty()) {
            sb.append("package ").append(info.getPackageName()).append(";\n\n");
        }
    }

    // ── Imports ────────────────────────────────────────────────────────────

    private void appendImports(StringBuilder sb, ServiceClassInfo info) {
        Set<String> imports = new LinkedHashSet<>();

        // JUnit 5
        imports.add("org.junit.jupiter.api.BeforeEach");
        imports.add("org.junit.jupiter.api.Test");
        imports.add("org.junit.jupiter.api.extension.ExtendWith");
        imports.add("static org.junit.jupiter.api.Assertions.*");

        // Mockito
        imports.add("org.mockito.InjectMocks");
        imports.add("org.mockito.Mock");
        imports.add("org.mockito.MockitoAnnotations");
        imports.add("org.mockito.junit.jupiter.MockitoExtension");
        imports.add("static org.mockito.Mockito.*");

        // Add exception imports for methods that throw
        for (MethodInfo method : info.getPublicMethods()) {
            if (!method.getThrownExceptions().isEmpty()) {
                imports.add("org.junit.jupiter.api.Assertions.assertThrows");
            }
        }

        for (String imp : imports) {
            sb.append("import ").append(imp).append(";\n");
        }
        sb.append("\n");
    }

    // ── Class declaration ─────────────────────────────────────────────────

    private void appendClassDeclaration(StringBuilder sb, ServiceClassInfo info) {
        sb.append("@ExtendWith(MockitoExtension.class)\n");
        sb.append("class ").append(info.getClassName()).append("Test {\n\n");
    }

    // ── @Mock fields + @InjectMocks ───────────────────────────────────────

    private void appendFields(StringBuilder sb, ServiceClassInfo info) {
        for (FieldInfo field : info.getInjectedFields()) {
            sb.append("    @Mock\n");
            sb.append("    private ").append(field.getTypeName())
              .append(" ").append(field.getFieldName()).append(";\n\n");
        }

        sb.append("    @InjectMocks\n");
        sb.append("    private ").append(info.getClassName())
          .append(" ").append(decapitalize(info.getClassName())).append(";\n\n");
    }

    // ── @BeforeEach setUp ─────────────────────────────────────────────────

    private void appendSetUp(StringBuilder sb) {
        sb.append("    @BeforeEach\n");
        sb.append("    void setUp() {\n");
        sb.append("        MockitoAnnotations.openMocks(this);\n");
        sb.append("    }\n\n");
    }

    // ── Test methods ──────────────────────────────────────────────────────

    private void appendTestMethods(StringBuilder sb, ServiceClassInfo info) {
        String serviceVar = decapitalize(info.getClassName());

        for (MethodInfo method : info.getPublicMethods()) {
            // Happy path test
            appendHappyPathTest(sb, method, serviceVar, info);

            // Exception test (if method declares thrown exceptions)
            for (String ex : method.getThrownExceptions()) {
                appendExceptionTest(sb, method, ex, serviceVar, info);
            }
        }
    }

    private void appendHappyPathTest(StringBuilder sb, MethodInfo method,
                                      String serviceVar, ServiceClassInfo info) {
        String testName = buildTestName(method.getMethodName(), "shouldSucceed");
        sb.append("    @Test\n");
        sb.append("    void ").append(testName).append("() {\n");

        // Arrange
        sb.append("        // Arrange\n");
        appendParamDeclarations(sb, method);
        appendMockStubbing(sb, method, info);

        // Act
        sb.append("\n        // Act\n");
        appendActLine(sb, method, serviceVar);

        // Assert
        sb.append("\n        // Assert\n");
        appendAssertions(sb, method, info);

        sb.append("    }\n\n");
    }

    private void appendExceptionTest(StringBuilder sb, MethodInfo method,
                                      String exceptionType, String serviceVar,
                                      ServiceClassInfo info) {
        String testName = buildTestName(method.getMethodName(),
                                        "shouldThrow" + exceptionType);
        sb.append("    @Test\n");
        sb.append("    void ").append(testName).append("() {\n");

        sb.append("        // Arrange\n");
        appendParamDeclarations(sb, method);

        // Stub first mock to throw
        if (!info.getInjectedFields().isEmpty()) {
            FieldInfo firstDep = info.getInjectedFields().get(0);
            sb.append("        doThrow(new ").append(exceptionType)
              .append("(\"test error\")).when(").append(firstDep.getFieldName())
              .append(")").append(buildAnyMethodMock(method, firstDep)).append(";\n");
        }

        sb.append("\n        // Act & Assert\n");
        sb.append("        assertThrows(").append(exceptionType).append(".class, () ->\n");
        sb.append("            ").append(serviceVar).append(".")
          .append(method.getMethodName()).append("(")
          .append(buildArgList(method)).append("));\n");

        sb.append("    }\n\n");
    }

    // ── Arrange helpers ───────────────────────────────────────────────────

    private void appendParamDeclarations(StringBuilder sb, MethodInfo method) {
        for (ParamInfo param : method.getParams()) {
            sb.append("        ")
              .append(param.getTypeName()).append(" ")
              .append(param.getParamName()).append(" = ")
              .append(defaultValueFor(param.getTypeName())).append(";\n");
        }
    }

    private void appendMockStubbing(StringBuilder sb, MethodInfo method,
                                     ServiceClassInfo info) {
        // Only stub if method is non-void (something needs to be returned)
        if (!method.isVoid() && !info.getInjectedFields().isEmpty()) {
            FieldInfo dep = info.getInjectedFields().get(0);
            String mockReturn = defaultValueFor(method.getReturnType());
            sb.append("        when(").append(dep.getFieldName())
              .append(buildAnyMethodMock(method, dep))
              .append(").thenReturn(").append(mockReturn).append(");\n");
        }
    }

    // ── Act helpers ───────────────────────────────────────────────────────

    private void appendActLine(StringBuilder sb, MethodInfo method, String serviceVar) {
        if (method.isVoid()) {
            sb.append("        ").append(serviceVar).append(".")
              .append(method.getMethodName()).append("(")
              .append(buildArgList(method)).append(");\n");
        } else {
            sb.append("        ").append(method.getReturnType())
              .append(" result = ").append(serviceVar).append(".")
              .append(method.getMethodName()).append("(")
              .append(buildArgList(method)).append(");\n");
        }
    }

    // ── Assert helpers ────────────────────────────────────────────────────

    private void appendAssertions(StringBuilder sb, MethodInfo method,
                                   ServiceClassInfo info) {
        if (method.isVoid()) {
            // For void methods, verify interactions
            if (!info.getInjectedFields().isEmpty()) {
                for (FieldInfo dep : info.getInjectedFields()) {
                    sb.append("        verify(").append(dep.getFieldName())
                      .append(", atLeastOnce()).").append(buildAnyVerify(method, dep))
                      .append(";\n");
                }
            } else {
                sb.append("        // TODO: verify expected side effects\n");
            }
        } else {
            sb.append("        assertNotNull(result);\n");
            // Add type-specific assertion
            String rt = method.getReturnType();
            if (rt.equals("boolean") || rt.equals("Boolean")) {
                sb.append("        assertTrue(result); // or assertFalse — adjust to your logic\n");
            } else if (rt.startsWith("List") || rt.startsWith("Collection") || rt.startsWith("Set")) {
                sb.append("        assertFalse(result.isEmpty());\n");
            } else if (rt.startsWith("Optional")) {
                sb.append("        assertTrue(result.isPresent());\n");
            }
        }
    }

    // ── Naming ────────────────────────────────────────────────────────────

    private String buildTestName(String methodName, String suffix) {
        String pattern = settings.getTestNamingPattern(); // e.g. "{method}_{suffix}"
        return pattern
            .replace("{method}", methodName)
            .replace("{suffix}", suffix);
    }

    // ── Argument helpers ─────────────────────────────────────────────────

    private String buildArgList(MethodInfo method) {
        StringBuilder args = new StringBuilder();
        for (int i = 0; i < method.getParams().size(); i++) {
            if (i > 0) args.append(", ");
            args.append(method.getParams().get(i).getParamName());
        }
        return args.toString();
    }

    private String buildAnyMethodMock(MethodInfo method, FieldInfo dep) {
        // We don't know exact method name on dep, so use a descriptive placeholder
        StringBuilder call = new StringBuilder(".someMethod(");
        for (int i = 0; i < method.getParams().size(); i++) {
            if (i > 0) call.append(", ");
            call.append("any()");
        }
        call.append(")");
        return call.toString();
        // NOTE: user should replace .someMethod with the actual dep method called
    }

    private String buildAnyVerify(MethodInfo method, FieldInfo dep) {
        return "someMethod(any()) /* TODO: replace with actual method */";
    }

    // ── Default values ────────────────────────────────────────────────────

    private String defaultValueFor(String typeName) {
        return switch (typeName) {
            case "int", "Integer"       -> "1";
            case "long", "Long"         -> "1L";
            case "double", "Double"     -> "1.0";
            case "float", "Float"       -> "1.0f";
            case "boolean", "Boolean"   -> "true";
            case "String"               -> "\"testValue\"";
            case "void"                 -> "null";
            default -> {
                if (typeName.startsWith("List"))       yield "new java.util.ArrayList<>()";
                if (typeName.startsWith("Set"))        yield "new java.util.HashSet<>()";
                if (typeName.startsWith("Map"))        yield "new java.util.HashMap<>()";
                if (typeName.startsWith("Optional"))   yield "Optional.empty()";
                // Object type — use mock()
                yield "mock(" + stripGenerics(typeName) + ".class)";
            }
        };
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    private String decapitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private String stripGenerics(String typeName) {
        int idx = typeName.indexOf('<');
        return idx >= 0 ? typeName.substring(0, idx) : typeName;
    }
}
