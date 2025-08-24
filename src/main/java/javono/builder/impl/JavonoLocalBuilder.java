package javono.builder.impl;

import javono.bootstrap.JavonoBootstrap;
import javono.builder.JavonoBuilder;
import javono.detector.DetectorFacade;
import javono.flasher.FlasherFacade;
import javono.logger.LoggerFacade;
import javono.probuilder.ProjectBuilderFacade;
import javono.utils.UtilsFacade;
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
        JavonoBootstrap.ensureInstalled();
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

        DiagnosticListener<JavaFileObject> listener = UtilsFacade.getInstance().getColoredDiagnosticListener();

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
        } else {
            LoggerFacade.getInstance().error("Toolchain missing make sure Javono is installed perfectly.");
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
        cleanProjectJavonoDir();
        return this;
    }

    @Override
    public JavonoBuilder setOption(String key, String value) {
        return this;
    }

    public void cleanProjectJavonoDir() {
        try {
            File baseDir = new File(System.getProperty("user.dir"));
            File javonoProjectDir = new File(baseDir, ".javono"); // project-level .javono
            File systemBinDir = new File(System.getProperty("user.home"), ".javono/bin");

            // Safety check: do not touch the system-level CLI folder
            if (javonoProjectDir.getAbsolutePath().equals(systemBinDir.getParentFile().getAbsolutePath())) {
                LoggerFacade.getInstance().info("Skipping system .javono directory: " + javonoProjectDir.getAbsolutePath());
                return;
            }

            if (javonoProjectDir.exists() && javonoProjectDir.isDirectory()) {
                deleteRecursively(javonoProjectDir);
                LoggerFacade.getInstance().info("Deleted project-level .javono directory: " + javonoProjectDir.getAbsolutePath());
            } else {
                LoggerFacade.getInstance().info("Nothing to clean: project-level .javono folder does not exist.");
            }
        } catch (IllegalStateException e) {
            LoggerFacade.getInstance().info("Nothing to clean: " + e.getMessage());
        } catch (IOException e) {
            LoggerFacade.getInstance().error("Failed to clean project-level .javono directory: " + e.getMessage());
        }
    }

    private void deleteRecursively(File file) throws IOException {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteRecursively(child);
            }
        }
        if (!file.delete()) {
            throw new IOException("Failed to delete: " + file.getAbsolutePath());
        }
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

        File srcDir = new File(dir, "src");
        File pomFile = new File(dir, "pom.xml");
        File gradleFile = new File(dir, "build.gradle");
        File gradleKtsFile = new File(dir, "build.gradle.kts");

        // Only valid if 'src' folder exists AND at least one build file exists in the same directory
        boolean validRoot = ((srcDir.exists() && srcDir.isDirectory()))
                || (pomFile.exists()
                || gradleFile.exists()
                || gradleKtsFile.exists());

        if (!validRoot) {
            throw new IllegalStateException(
                    "Javono build must be run from the project root.\n" +
                            "Expected a 'src' folder and one of: pom.xml, build.gradle, build.gradle.kts in the current directory.\n" +
                            "Current directory: " + dir.getAbsolutePath()
            );
        }
    }


}
