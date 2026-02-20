package com.testgen.plugin;

import com.testgen.plugin.generator.TestCodeGenerator;
import com.testgen.plugin.model.ServiceClassInfo;
import com.testgen.plugin.model.ServiceClassInfo.*;
import com.testgen.plugin.settings.TestGeneratorSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TestCodeGeneratorTest {

    private TestCodeGenerator generator;
    private TestGeneratorSettings settings;

    @BeforeEach
    void setUp() {
        settings = mock(TestGeneratorSettings.class);
        when(settings.getTestNamingPattern()).thenReturn("{method}_should{suffix}");
        generator = new TestCodeGenerator(settings);
    }

    @Test
    void generate_shouldIncludeExtendWithAnnotation() {
        ServiceClassInfo info = buildSampleInfo();
        String result = generator.generate(info);
        assertTrue(result.contains("@ExtendWith(MockitoExtension.class)"));
    }

    @Test
    void generate_shouldIncludeMockFields() {
        ServiceClassInfo info = buildSampleInfo();
        String result = generator.generate(info);
        assertTrue(result.contains("@Mock"));
        assertTrue(result.contains("UserRepository userRepository"));
    }

    @Test
    void generate_shouldIncludeInjectMocks() {
        ServiceClassInfo info = buildSampleInfo();
        String result = generator.generate(info);
        assertTrue(result.contains("@InjectMocks"));
        assertTrue(result.contains("UserService userService"));
    }

    @Test
    void generate_shouldIncludeBeforeEach() {
        ServiceClassInfo info = buildSampleInfo();
        String result = generator.generate(info);
        assertTrue(result.contains("@BeforeEach"));
        assertTrue(result.contains("MockitoAnnotations.openMocks(this)"));
    }

    @Test
    void generate_shouldCreateTestMethodForEachPublicMethod() {
        ServiceClassInfo info = buildSampleInfo();
        String result = generator.generate(info);
        assertTrue(result.contains("findById_shouldSucceed"));
        assertTrue(result.contains("save_shouldSucceed"));
    }

    @Test
    void generate_shouldIncludeArrangeActAssert() {
        ServiceClassInfo info = buildSampleInfo();
        String result = generator.generate(info);
        assertTrue(result.contains("// Arrange"));
        assertTrue(result.contains("// Act"));
        assertTrue(result.contains("// Assert"));
    }

    @Test
    void generate_shouldHandleVoidMethod() {
        MethodInfo voidMethod = new MethodInfo("deleteUser", "void", true,
            List.of(new ParamInfo("Long", "id")), List.of());
        ServiceClassInfo info = new ServiceClassInfo("com.example", "UserService",
            List.of(new FieldInfo("UserRepository", "userRepository")),
            List.of(voidMethod));

        String result = generator.generate(info);
        // void methods: no result variable, verify interactions
        assertFalse(result.contains("void result ="));
        assertTrue(result.contains("verify(userRepository"));
    }

    @Test
    void generate_shouldGenerateExceptionTestForThrowingMethod() {
        MethodInfo throwingMethod = new MethodInfo("findById", "User", false,
            List.of(new ParamInfo("Long", "id")),
            List.of("UserNotFoundException"));
        ServiceClassInfo info = new ServiceClassInfo("com.example", "UserService",
            List.of(new FieldInfo("UserRepository", "userRepository")),
            List.of(throwingMethod));

        String result = generator.generate(info);
        assertTrue(result.contains("shouldThrowUserNotFoundException"));
        assertTrue(result.contains("assertThrows(UserNotFoundException.class"));
    }

    // ── Sample data ────────────────────────────────────────────────────────

    private ServiceClassInfo buildSampleInfo() {
        return new ServiceClassInfo(
            "com.example.service",
            "UserService",
            List.of(new FieldInfo("UserRepository", "userRepository")),
            List.of(
                new MethodInfo("findById", "User", false,
                    List.of(new ParamInfo("Long", "id")), List.of()),
                new MethodInfo("save", "User", false,
                    List.of(new ParamInfo("User", "user")), List.of())
            )
        );
    }
}
