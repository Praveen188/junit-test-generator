package com.testgen.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import org.jetbrains.annotations.NotNull;

/**
 * Persists user preferences for the plugin via IntelliJ's PersistentStateComponent.
 * Settings are saved in ~/Library/Application Support/JetBrains/.../options/TestGeneratorSettings.xml
 */
@State(
    name = "TestGeneratorSettings",
    storages = @Storage("TestGeneratorSettings.xml")
)
@Service
public final class TestGeneratorSettings implements PersistentStateComponent<TestGeneratorSettings.State> {

    // Defaults
    public static class State {
        public String testNamingPattern = "{method}_should{suffix}";
        public boolean openFileAfterGeneration = true;
        public boolean generateExceptionTests = true;
        public boolean addTodoComments = true;
    }

    private State state = new State();

    public static TestGeneratorSettings getInstance() {
        return ApplicationManager.getApplication().getService(TestGeneratorSettings.class);
    }

    @Override
    public @NotNull State getState() { return state; }

    @Override
    public void loadState(@NotNull State state) { this.state = state; }

    // ── Accessors ──────────────────────────────────────────────────────────

    public String getTestNamingPattern()       { return state.testNamingPattern; }
    public boolean isOpenAfterGeneration()     { return state.openFileAfterGeneration; }
    public boolean isGenerateExceptionTests()  { return state.generateExceptionTests; }
    public boolean isAddTodoComments()         { return state.addTodoComments; }

    public void setTestNamingPattern(String p)      { state.testNamingPattern = p; }
    public void setOpenAfterGeneration(boolean v)   { state.openFileAfterGeneration = v; }
    public void setGenerateExceptionTests(boolean v){ state.generateExceptionTests = v; }
    public void setAddTodoComments(boolean v)       { state.addTodoComments = v; }
}
