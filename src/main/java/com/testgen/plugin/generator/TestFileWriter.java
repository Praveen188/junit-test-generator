package com.testgen.plugin.generator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.testgen.plugin.model.ServiceClassInfo;

import java.io.IOException;
import java.util.Arrays;

/**
 * Writes the generated test source to the correct location:
 *   src/test/java/<package>/<ClassName>Test.java
 *
 * If the file already exists, it appends only the missing test methods.
 */
public class TestFileWriter {

    private final Project project;

    public TestFileWriter(Project project) {
        this.project = project;
    }

    /**
     * Main entry: create or update test file.
     * Returns the written PsiFile (so we can open it in editor).
     */
    public PsiFile writeTestFile(PsiClass sourceClass, String generatedSource,
                                  ServiceClassInfo info) {
        String testFileName  = info.getClassName() + "Test.java";
        String packagePath   = info.getPackageName().replace('.', '/');

        Module module = ModuleUtilCore.findModuleForPsiElement(sourceClass);
        if (module == null) return null;

        VirtualFile testRoot = findOrCreateTestSourceRoot(module);
        if (testRoot == null) return null;

        return WriteCommandAction.writeCommandAction(project)
            .withName("Generate JUnit Tests")
            .compute(() -> {
                try {
                    // Navigate to / create the package directory
                    VirtualFile packageDir = VfsUtil.createDirectoryIfMissing(
                        testRoot, packagePath);
                    if (packageDir == null) return null;

                    VirtualFile existing = packageDir.findChild(testFileName);

                    if (existing != null) {
                        // File exists — append only missing test methods
                        return appendMissingMethods(existing, generatedSource, info);
                    } else {
                        // Create brand new file
                        VirtualFile newFile = packageDir.createChildData(this, testFileName);
                        VfsUtil.saveText(newFile, generatedSource);
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(newFile);
                        openInEditor(newFile);
                        return psiFile;
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to write test file: " + e.getMessage(), e);
                }
            });
    }

    // ── Append missing methods to existing test file ──────────────────────

    private PsiFile appendMissingMethods(VirtualFile existingFile,
                                          String generatedSource,
                                          ServiceClassInfo info) throws IOException {
        String existing = VfsUtil.loadText(existingFile);

        // Extract test methods from generated source that aren't in existing file
        StringBuilder toAppend = new StringBuilder();
        String[] lines = generatedSource.split("\n");
        boolean inMethod = false;
        int braceDepth = 0;
        StringBuilder currentMethod = new StringBuilder();
        String methodName = null;

        for (String line : lines) {
            if (!inMethod && line.trim().startsWith("@Test")) {
                inMethod = true;
                currentMethod = new StringBuilder();
                braceDepth = 0;
            }

            if (inMethod) {
                currentMethod.append(line).append("\n");
                for (char c : line.toCharArray()) {
                    if (c == '{') braceDepth++;
                    else if (c == '}') braceDepth--;
                }

                // Capture method name from "void methodName"
                if (line.trim().startsWith("void ") && methodName == null) {
                    String trimmed = line.trim();
                    int start = "void ".length();
                    int end   = trimmed.indexOf('(');
                    if (end > start) methodName = trimmed.substring(start, end).trim();
                }

                // Method block closed
                if (braceDepth == 0 && currentMethod.toString().contains("{")) {
                    inMethod = false;
                    // Only append if this method name doesn't already exist
                    if (methodName != null && !existing.contains(methodName + "(")) {
                        toAppend.append("\n").append(currentMethod);
                    }
                    methodName = null;
                }
            }
        }

        if (toAppend.length() > 0) {
            // Insert before the last closing brace of the class
            int lastBrace = existing.lastIndexOf('}');
            String updated = existing.substring(0, lastBrace)
                           + toAppend
                           + existing.substring(lastBrace);
            VfsUtil.saveText(existingFile, updated);
        }

        openInEditor(existingFile);
        return PsiManager.getInstance(project).findFile(existingFile);
    }

    // ── Find or create src/test/java source root ──────────────────────────

    private VirtualFile findOrCreateTestSourceRoot(Module module) {
        // 1. Try to find existing test source root
        for (VirtualFile root : ModuleRootManager.getInstance(module).getSourceRoots(true)) {
            String path = root.getPath();
            if (path.contains("test") && !path.contains("resources")) {
                return root;
            }
        }

        // 2. Fall back: construct path next to main source root
        for (VirtualFile root : ModuleRootManager.getInstance(module).getSourceRoots(false)) {
            String path = root.getPath();
            if (path.endsWith("src/main/java") || path.endsWith("src\\main\\java")) {
                String testPath = path.replace("src/main/java", "src/test/java")
                                      .replace("src\\main\\java", "src\\test\\java");
                try {
                    return VfsUtil.createDirectoryIfMissing(testPath);
                } catch (IOException e) {
                    return null;
                }
            }
        }
        return null;
    }

    // ── Open file in editor ───────────────────────────────────────────────

    private void openInEditor(VirtualFile file) {
        ApplicationManager.getApplication().invokeLater(() ->
            FileEditorManager.getInstance(project).openFile(file, true));
    }
}
