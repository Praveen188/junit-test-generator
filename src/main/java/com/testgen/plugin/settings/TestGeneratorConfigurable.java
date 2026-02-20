package com.testgen.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Settings panel shown under Preferences → Tools → JUnit Test Generator
 */
public class TestGeneratorConfigurable implements Configurable {

    private JBTextField namingPatternField;
    private JBCheckBox openAfterGenerationBox;
    private JBCheckBox generateExceptionTestsBox;
    private JBCheckBox addTodoCommentsBox;

    @Override
    public @Nls String getDisplayName() {
        return "JUnit Test Generator";
    }

    @Override
    public @Nullable JComponent createComponent() {
        TestGeneratorSettings settings = TestGeneratorSettings.getInstance();

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 4, 8);

        // ── Naming Pattern ─────────────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JBLabel("Test naming pattern:"), gbc);

        namingPatternField = new JBTextField(settings.getTestNamingPattern(), 30);
        gbc.gridx = 1;
        panel.add(namingPatternField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        panel.add(new JBLabel(
            "<html><small>Placeholders: {method} = method name, {suffix} = scenario suffix<br>" +
            "Examples: {method}_{suffix} &nbsp;|&nbsp; test_{method} &nbsp;|&nbsp; {method}_should{suffix}</small></html>"
        ), gbc);

        // ── Checkboxes ─────────────────────────────────────────────────────
        gbc.gridy = 2; gbc.gridwidth = 2;
        openAfterGenerationBox = new JBCheckBox(
            "Open generated test file in editor", settings.isOpenAfterGeneration());
        panel.add(openAfterGenerationBox, gbc);

        gbc.gridy = 3;
        generateExceptionTestsBox = new JBCheckBox(
            "Generate exception test methods for declared throws",
            settings.isGenerateExceptionTests());
        panel.add(generateExceptionTestsBox, gbc);

        gbc.gridy = 4;
        addTodoCommentsBox = new JBCheckBox(
            "Add // TODO comments in generated tests", settings.isAddTodoComments());
        panel.add(addTodoCommentsBox, gbc);

        // Padding
        gbc.gridy = 5; gbc.weighty = 1.0;
        panel.add(new JPanel(), gbc);

        return panel;
    }

    @Override
    public boolean isModified() {
        TestGeneratorSettings s = TestGeneratorSettings.getInstance();
        return !namingPatternField.getText().equals(s.getTestNamingPattern())
            || openAfterGenerationBox.isSelected()    != s.isOpenAfterGeneration()
            || generateExceptionTestsBox.isSelected() != s.isGenerateExceptionTests()
            || addTodoCommentsBox.isSelected()         != s.isAddTodoComments();
    }

    @Override
    public void apply() {
        TestGeneratorSettings s = TestGeneratorSettings.getInstance();
        s.setTestNamingPattern(namingPatternField.getText().trim());
        s.setOpenAfterGeneration(openAfterGenerationBox.isSelected());
        s.setGenerateExceptionTests(generateExceptionTestsBox.isSelected());
        s.setAddTodoComments(addTodoCommentsBox.isSelected());
    }

    @Override
    public void reset() {
        TestGeneratorSettings s = TestGeneratorSettings.getInstance();
        namingPatternField.setText(s.getTestNamingPattern());
        openAfterGenerationBox.setSelected(s.isOpenAfterGeneration());
        generateExceptionTestsBox.setSelected(s.isGenerateExceptionTests());
        addTodoCommentsBox.setSelected(s.isAddTodoComments());
    }
}
