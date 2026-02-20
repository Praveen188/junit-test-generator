package com.testgen.plugin.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.testgen.plugin.model.ServiceClassInfo;
import com.testgen.plugin.model.ServiceClassInfo.MethodInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog shown before generating tests.
 * Lists all public methods with checkboxes — user picks which ones to test.
 *
 * Shows method signature, return type, and param count for clarity.
 */
public class MethodSelectorDialog extends DialogWrapper {

    private final ServiceClassInfo info;
    private final CheckBoxList<MethodInfo> checkBoxList;

    public MethodSelectorDialog(Project project, ServiceClassInfo info) {
        super(project, true);
        this.info = info;
        this.checkBoxList = new CheckBoxList<>();

        setTitle("Generate JUnit Tests — " + info.getClassName());
        setOKButtonText("Generate Tests");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setPreferredSize(new Dimension(480, 320));

        // Header label
        JBLabel header = new JBLabel(
            "<html><b>" + info.getClassName() + "</b> — select methods to test</html>");
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        panel.add(header, BorderLayout.NORTH);

        // Populate checkbox list
        for (MethodInfo method : info.getPublicMethods()) {
            String label = buildMethodLabel(method);
            checkBoxList.addItem(method, label, true); // all checked by default
        }

        panel.add(new JBScrollPane(checkBoxList), BorderLayout.CENTER);

        // Footer with select-all / deselect-all
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton selectAll   = new JButton("Select All");
        JButton deselectAll = new JButton("Deselect All");

        selectAll.addActionListener(e -> setAllChecked(true));
        deselectAll.addActionListener(e -> setAllChecked(false));

        footer.add(selectAll);
        footer.add(deselectAll);

        JBLabel hint = new JBLabel(
            "  " + info.getInjectedFields().size() + " @Mock dep(s) detected");
        hint.setForeground(new Color(130, 130, 130));
        hint.setFont(hint.getFont().deriveFont(11f));
        footer.add(hint);

        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    public List<MethodInfo> getSelectedMethods() {
        List<MethodInfo> selected = new ArrayList<>();
        for (int i = 0; i < checkBoxList.getItemsCount(); i++) {
            if (checkBoxList.isItemSelected(i)) {
                selected.add(checkBoxList.getItemAt(i));
            }
        }
        return selected;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private String buildMethodLabel(MethodInfo method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getReturnType()).append("  ");
        sb.append(method.getMethodName()).append("(");
        List<ServiceClassInfo.ParamInfo> params = method.getParams();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(params.get(i).getTypeName()).append(" ").append(params.get(i).getParamName());
        }
        sb.append(")");
        if (!method.getThrownExceptions().isEmpty()) {
            sb.append("  throws ").append(String.join(", ", method.getThrownExceptions()));
        }
        return sb.toString();
    }

    private void setAllChecked(boolean checked) {
        for (int i = 0; i < checkBoxList.getItemsCount(); i++) {
            checkBoxList.setItemSelected(checkBoxList.getItemAt(i), checked);
        }
        checkBoxList.repaint();
    }
}
