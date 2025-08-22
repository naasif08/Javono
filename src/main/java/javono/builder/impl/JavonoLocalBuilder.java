package javono.builder.impl;

import javono.builder.JavonoBuilder;
import javono.compilehelper.ColoredDiagnosticListener;
import javono.detector.DetectorFacade;
import javono.flasher.FlasherFacade;
import javono.logger.LoggerFacade;
import javono.probuilder.ProjectBuilderFacade;
import javono.validator.ValidatorFacade;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JavonoLocalBuilder implements JavonoBuilder {

    private File projectDir;

    @Override
    public JavonoBuilder build() {
        checkInsideProjectRoot();
        // ----------------------------
        // CLI compilation with annotation processor
        // ----------------------------
        File srcDir = new File(System.getProperty("user.dir"), "src");
        File outDir = new File(System.getProperty("user.dir"), ".javono/build/classes");
        if (!outDir.exists()) outDir.mkdirs();

        // Collect Java source files
        List<File> sourceFiles = collectJavaFiles(srcDir); // implement this utility method

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFiles);

        List<String> options = new ArrayList<>();
        options.add("-d");
        options.add(outDir.getAbsolutePath());
        options.add("-cp");
        options.add(System.getProperty("java.class.path"));
        options.add("-processor");
        options.add("javono.annotations.processor.AnnotationProcessor"); // full processor class name

        DiagnosticListener<JavaFileObject> listener = new ColoredDiagnosticListener();

        JavaCompiler.CompilationTask task = compiler.getTask(
                null,
                fileManager,
                listener,
                options,
                null,
                compilationUnits
        );

        boolean success = task.call();
        try {
            fileManager.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!success) {
            throw new RuntimeException("Build failed due to annotation validation errors.");
        }

        ValidatorFacade.getInstance().validateProject();

        if (!DetectorFacade.getInstance().isToolPathsInitialized()) {
            DetectorFacade.getInstance().initializeToolPaths();
        }
        try {
            this.projectDir = ProjectBuilderFacade.getInstance().createProject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            String port = DetectorFacade.getInstance().detectEsp32Port();
            if (port == null) {
                throw new IllegalStateException("ESP32 port could not be detected. Please connect your device.");
            }
            ProjectBuilderFacade.getInstance().writeBuildScripts(this.projectDir, port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public JavonoBuilder flash() {
        this.projectDir = detectProjectDir();
        try {
            FlasherFacade.getInstance().flashProject(this.projectDir);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public JavonoBuilder clean() {
        try {
            File dirToDelete = detectProjectDir().getParentFile();
            deleteRecursively(dirToDelete);
            LoggerFacade.getInstance().info("Deleted project directory: " + dirToDelete.getAbsolutePath());
            projectDir = null; // reset after deletion
        } catch (IllegalStateException e) {
            LoggerFacade.getInstance().info("Nothing to clean: " + e.getMessage());
        }
        return this;
    }

    private void deleteRecursively(File file) {
        if (!file.exists()) return;

        if (file.isDirectory()) {
            File[] contents = file.listFiles();
            if (contents != null) {
                for (File f : contents) {
                    deleteRecursively(f);
                }
            }
        }
        file.delete();
    }


    @Override
    public JavonoBuilder setOption(String key, String value) {
        return this;
    }

    private File detectProjectDir() {
        // Get current working directory
        File baseDir = new File(System.getProperty("user.dir"));

        // Common location of Javono project
        File javonoDir = new File(baseDir, ".javono/ESP32Project");
        if (javonoDir.exists() && javonoDir.isDirectory()) {
            return javonoDir;
        }

        // Optionally, scan subfolders if needed
        File[] matches = baseDir.listFiles((dir, name) -> name.equals(".javono"));
        if (matches != null && matches.length > 0) {
            File espDir = new File(matches[0], "ESP32Project");
            if (espDir.exists() && espDir.isDirectory()) return espDir;
        }

        throw new IllegalStateException("Cannot detect Javono ESP32 project folder. Run `javono build` first!");
    }

    private List<File> collectJavaFiles(File dir) {
        List<File> javaFiles = new ArrayList<>();
        collectJavaFilesRecursive(dir, javaFiles);
        return javaFiles;
    }

    private void collectJavaFilesRecursive(File dir, List<File> javaFiles) {
        if (dir == null || !dir.exists()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                collectJavaFilesRecursive(file, javaFiles);
            } else if (file.getName().endsWith(".java")) {
                javaFiles.add(file);
            }
        }
    }

    private void checkInsideProjectRoot() {
        File dir = new File(System.getProperty("user.dir"));
        boolean validRoot = false;

        while (dir != null) {
            File srcDir = new File(dir, "src");
            File pomFile = new File(dir, "pom.xml");
            File gradleFile = new File(dir, "build.gradle");
            File gradleKtsFile = new File(dir, "build.gradle.kts");
            File javonoDir = new File(dir, ".javono");

            if ((srcDir.exists() && srcDir.isDirectory()) ||
                    pomFile.exists() ||
                    gradleFile.exists() ||
                    gradleKtsFile.exists() ||
                    (javonoDir.exists() && javonoDir.isDirectory())) {
                validRoot = true;
                break;
            }
            dir = dir.getParentFile();
        }

        if (!validRoot) {
            throw new IllegalStateException(
                    "Javono build must be run from inside the project root or its subfolders. " +
                            "Cannot find 'src', pom.xml, build.gradle, or .javono folder in current or parent directories."
            );
        }
    }


}
