# JUnit Test Generator — IntelliJ Plugin

Auto-generate **JUnit 5 + Mockito** test classes from any Java service class.

## What it generates

Given this service class:

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    public User findById(Long id) throws UserNotFoundException {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
        emailService.sendDeletionConfirmation(id);
    }
}
```

The plugin generates:

```java
package com.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void findById_shouldSucceed() {
        // Arrange
        Long id = 1L;
        User mockUser = mock(User.class);
        when(userRepository.someMethod(any())).thenReturn(mockUser);

        // Act
        User result = userService.findById(id);

        // Assert
        assertNotNull(result);
    }

    @Test
    void findById_shouldThrowUserNotFoundException() {
        // Arrange
        Long id = 1L;
        doThrow(new UserNotFoundException("test error")).when(userRepository).someMethod(any());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () ->
            userService.findById(id));
    }

    @Test
    void save_shouldSucceed() {
        // Arrange
        User user = mock(User.class);
        when(userRepository.someMethod(any())).thenReturn(mock(User.class));

        // Act
        User result = userService.save(user);

        // Assert
        assertNotNull(result);
    }

    @Test
    void deleteUser_shouldSucceed() {
        // Arrange
        Long id = 1L;

        // Act
        userService.deleteUser(id);

        // Assert
        verify(userRepository, atLeastOnce()).someMethod(any());
        verify(emailService, atLeastOnce()).someMethod(any());
    }
}
```

## Project Structure

```
src/main/java/com/testgen/plugin/
├── actions/
│   └── GenerateTestsAction.java      ← Alt+Shift+T entry point
├── generator/
│   ├── PsiClassAnalyzer.java         ← reads the Java PSI tree
│   ├── TestCodeGenerator.java        ← builds the test source string
│   └── TestFileWriter.java           ← writes to src/test/java
├── model/
│   └── ServiceClassInfo.java         ← data model (class info, methods, params)
├── settings/
│   ├── TestGeneratorSettings.java    ← persisted settings
│   └── TestGeneratorConfigurable.java← settings UI panel
└── ui/
    └── MethodSelectorDialog.java     ← method picker dialog
```

## Setup & Run

### Prerequisites
- IntelliJ IDEA (Community or Ultimate)
- JDK 17+
- Gradle

### Run in sandbox
```bash
./gradlew runIde
```
This opens a second IntelliJ window with your plugin loaded.

### Build distributable
```bash
./gradlew buildPlugin
# Output: build/distributions/junit-generator-1.0.0.zip
```

### Run tests (no IntelliJ needed)
```bash
./gradlew test
```

## Usage

1. Open any Java service class in IntelliJ
2. Press **Alt+Shift+T**  (or Code → Generate JUnit Tests)
3. Select which methods to test in the dialog
4. Test file is created at `src/test/java/<package>/<ClassName>Test.java`

## Configuration

Preferences → Tools → **JUnit Test Generator**

| Setting | Default | Description |
|---------|---------|-------------|
| Test naming pattern | `{method}_should{suffix}` | Controls test method names |
| Open after generation | ✅ | Opens the test file immediately |
| Generate exception tests | ✅ | Creates extra tests for declared `throws` |
| Add TODO comments | ✅ | Adds `// TODO` hints in generated stubs |

## Notes

- The generated stubs use `.someMethod(any())` as a placeholder in `when()` and `verify()` — **replace these** with the actual repository/service methods your code calls
- The plugin detects `@Autowired`, `@Inject`, and constructor-injected dependencies automatically
- If a test file already exists, only missing test methods are appended
