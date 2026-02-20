package com.testgen.plugin.actions;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.testgen.plugin.generator.*;
import com.testgen.plugin.model.ServiceClassInfo;
import com.testgen.plugin.settings.TestGeneratorSettings;
import com.testgen.plugin.ui.MethodSelectorDialog;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Entry point for the plugin.
 * Triggered by: Alt+Shift+T OR Code menu → "Generate JUnit Tests"
 *
 * Flow:
 *   1. Get PsiClass from current editor cursor position
 *   2. Analyze it (PsiClassAnalyzer)
 *   3. Show method selector dialog (user picks which methods to test)
 *   4. Generate test source (TestCodeGenerator)
 *   5. Write file to src/test/java (TestFileWriter)
 *   6. Show success notification
 */
public class GenerateTestsAction extends AnAction {

    private static final NotificationGroup NOTIF_GROUP =
        NotificationGroupManager.getInstance()
            .getNotificationGroup("JUnit Test Generator");

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        // ── 1. Resolve the PsiClass under cursor ──────────────────────────
        PsiClass psiClass = resolvePsiClass(e);
        if (psiClass == null) {
            notifyError(project, "No Java class found at cursor position.");
            return;
        }

        if (psiClass.isInterface()) {
            notifyError(project, "Cannot generate tests for an interface. Open a concrete class.");
            return;
        }

        // ── 2. Analyze the class ──────────────────────────────────────────
        PsiClassAnalyzer analyzer = new PsiClassAnalyzer();
        ServiceClassInfo info = analyzer.analyze(psiClass);

        if (info == null || info.getPublicMethods().isEmpty()) {
            notifyError(project, "No public methods found in " + psiClass.getName() + ".");
            return;
        }

        // ── 3. Show method selector dialog ────────────────────────────────
        MethodSelectorDialog dialog = new MethodSelectorDialog(project, info);
        if (!dialog.showAndGet()) return; // user cancelled

        List<ServiceClassInfo.MethodInfo> selectedMethods = dialog.getSelectedMethods();
        if (selectedMethods.isEmpty()) {
            notifyError(project, "No methods selected.");
            return;
        }

        // Rebuild ServiceClassInfo with only selected methods
        ServiceClassInfo filteredInfo = new ServiceClassInfo(
            info.getPackageName(),
            info.getClassName(),
            info.getInjectedFields(),
            selectedMethods
        );

        // ── 4. Generate test source ───────────────────────────────────────
        TestGeneratorSettings settings = TestGeneratorSettings.getInstance();
        TestCodeGenerator generator    = new TestCodeGenerator(settings);
        String testSource              = generator.generate(filteredInfo);

        // ── 5. Write file ─────────────────────────────────────────────────
        TestFileWriter writer = new TestFileWriter(project);
        PsiFile testFile = writer.writeTestFile(psiClass, testSource, filteredInfo);

        // ── 6. Notify success ─────────────────────────────────────────────
        if (testFile != null) {
            notifySuccess(project,
                filteredInfo.getClassName() + "Test.java generated with " +
                selectedMethods.size() + " test method(s).");
        } else {
            notifyError(project, "Failed to create test file. Check that src/test/java exists.");
        }
    }

    // ── Availability: only enable when inside a Java file ─────────────────

    @Override
    public void update(@NotNull AnActionEvent e) {
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabledAndVisible(file instanceof PsiJavaFile);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    // ── Resolve PsiClass from cursor or file ──────────────────────────────

    private PsiClass resolvePsiClass(AnActionEvent e) {
        // Try from editor cursor first
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (psiFile == null) return null;

        if (editor != null) {
            int offset = editor.getCaretModel().getOffset();
            PsiElement element = psiFile.findElementAt(offset);
            PsiClass fromCursor = PsiTreeUtil.getParentOfType(element, PsiClass.class);
            if (fromCursor != null) return fromCursor;
        }

        // Fallback: return first class in file
        if (psiFile instanceof PsiJavaFile) {
            PsiClass[] classes = ((PsiJavaFile) psiFile).getClasses();
            return classes.length > 0 ? classes[0] : null;
        }

        return null;
    }

    // ── Notifications ─────────────────────────────────────────────────────

    private void notifySuccess(Project project, String message) {
        NOTIF_GROUP.createNotification(
            "✅ JUnit Tests Generated",
            message,
            NotificationType.INFORMATION
        ).notify(project);
    }

    private void notifyError(Project project, String message) {
        NOTIF_GROUP.createNotification(
            "⚠️ JUnit Generator",
            message,
            NotificationType.WARNING
        ).notify(project);
    }
}
