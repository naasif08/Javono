package javono.probuilder;

import javono.validator.ValidatorFacade;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProjectBuilderFacade {

    private static final ProjectBuilderFacade INSTANCE = new ProjectBuilderFacade();
    private static final BatchBuilder batchBuilder = new BatchBuilder();
    private static final ProjectCreator projectCreator = new ProjectCreator();
    private static final String PROCESSOR_CLASS = "javono.annotations.processor.AnnotationProcessor";


    private ProjectBuilderFacade() {
    }


    public static ProjectBuilderFacade getInstance() {
        return INSTANCE;
    }

    public void writeBuildScripts(File projectDir, String comPort) throws IOException {
        batchBuilder.writeBuildScripts(projectDir, comPort);
    }

    public File createProject() throws IOException {
        return projectCreator.createProject();
    }

    public boolean compileWithProcessor(File sourceDir, File outputDir, String classpath) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No Java compiler found. Are you running with a JDK instead of a JRE?");
        }

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        try {
            // Collect all .java files in sourceDir
            List<File> sourceFiles = new ArrayList<>();
            collectJavaFiles(sourceDir, sourceFiles);

            Iterable<? extends JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjectsFromFiles(sourceFiles);

            // Build compiler options
            List<String> options = new ArrayList<>();
            options.add("-d");
            options.add(outputDir.getAbsolutePath());

            if (classpath != null && !classpath.isEmpty()) {
                options.add("-cp");
                options.add(classpath);
            }

            options.add("-processor");
            options.add(PROCESSOR_CLASS);

            // Run compilation
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, fileManager, null, options, null, compilationUnits
            );

            return task.call();
        } catch (Exception e) {
            throw new RuntimeException("Compilation failed: " + e.getMessage(), e);
        }
    }

    private void collectJavaFiles(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                collectJavaFiles(f, result);
            } else if (f.getName().endsWith(".java")) {
                result.add(f);
            }
        }
    }

}
