package javono.validator;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import javono.annotations.JavonoCustomMethod;
import javono.annotations.JavonoLoop;
import javono.annotations.JavonoSetup;
import javono.annotations.JavonoSketch;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SketchValidator {

    private final Path userProjectSrcDir = detectUniversalJavaSourceDir();

    // Create a shared parser instance configured for Java 21
    private static final JavaParser parser;

    static {
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        parser = new JavaParser(config);
    }

    private static List<String> getInvokedMethods(ClassOrInterfaceDeclaration clazz, Class<? extends Annotation> annotationClass) {
        List<String> methodsWhichAreInvoked = new ArrayList<>();
        clazz.getMethods().forEach(
                methodDeclaration -> {
                    if (!methodDeclaration.getBody().isEmpty() && methodDeclaration.isAnnotationPresent(annotationClass)) {
                        methodDeclaration.findAll(MethodCallExpr.class).forEach(
                                invokedMethod -> methodsWhichAreInvoked.add(invokedMethod.getNameAsString())
                        );
                    }
                }
        );
        return methodsWhichAreInvoked;
    }

    public static Path detectUniversalJavaSourceDir() {
        Path base = Paths.get(System.getProperty("user.dir"));

        // Maven/Gradle
        Path maven = base.resolve("src").resolve("main").resolve("java");
        if (Files.exists(maven)) return maven;

        // Eclipse or IntelliJ simple project
        Path eclipse = base.resolve("src");
        if (Files.exists(eclipse)) return eclipse;

        // NetBeans
        Path netbeans = base.resolve("src").resolve("java");
        if (Files.exists(netbeans)) return netbeans;

        // Default fallback (project root)
        return base;
    }

    public void validateProject() throws IOException {
        AtomicBoolean sketchFound = new AtomicBoolean(false);

        Files.walk(this.userProjectSrcDir)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(file -> validateFile(file, sketchFound));

        if (!sketchFound.get()) {
            System.err.println("❌ No class annotated with @Sketch found.");
            System.exit(1);
        }
    }

    private static void validateFile(Path file, AtomicBoolean sketchFound) {
        try {
            // Use the configured parser here instead of StaticJavaParser
            ParseResult<CompilationUnit> result = parser.parse(file);

            if (result.isSuccessful() && result.getResult().isPresent()) {
                CompilationUnit cu = result.getResult().get();

                cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
                    if (clazz.isAnnotationPresent(JavonoSketch.class)) {
                        sketchFound.set(true);
                        boolean hasSetup = clazz.getMethods()
                                .stream()
                                .anyMatch(method -> method.isAnnotationPresent(JavonoSetup.class));
                        if (!hasSetup) {
                            System.err.println("No @JavonoSetup annotation found.");
                            System.exit(1);
                        } else {
                            getInvokedMethods(clazz, JavonoSetup.class).forEach(
                                    method -> {
                                        boolean isMethodCalledItself = clazz.getMethods().stream().anyMatch(
                                                method2 -> method2.isAnnotationPresent(JavonoSetup.class) && method2.getNameAsString().equals(method)
                                        );
                                        boolean isLoopCalledFromSetup = clazz.getMethods().stream().anyMatch(
                                                method2 -> method2.isAnnotationPresent(JavonoLoop.class) && method2.getNameAsString().equals(method)
                                        );
                                        if (isLoopCalledFromSetup) {
                                            System.err.println("\uD83D\uDE05 Nice try! @JavonoSetup is for setting the stage — not for looping the show.\n" +
                                                    "\n");
                                            System.exit(1);
                                        }
                                        if (isMethodCalledItself) {
                                            System.err.println("\uD83D\uDE05 Smart try! You can’t sneak in a call to @JavonoSetup from inside itself or from @JavonoLoop.\n"
                                                    + "Let it do its job in peace — it's called *setup* for a reason!\n");
                                            System.exit(1);
                                        }
                                    }
                            );
                        }


                        boolean hasLoop = clazz.getMethods()
                                .stream()
                                .anyMatch(method -> method.isAnnotationPresent(JavonoLoop.class));
                        if (!hasLoop) {
                            System.err.println("No @JavonoLoop annotation found.");
                            System.exit(1);
                        } else {
                            getInvokedMethods(clazz, JavonoLoop.class).forEach(
                                    method -> {
                                        boolean isMethodCalledItself = clazz.getMethods().stream().anyMatch(
                                                method2 -> method2.isAnnotationPresent(JavonoLoop.class) && method2.getNameAsString().equals(method)
                                        );
                                        boolean isSetupCalledFromLoop = clazz.getMethods().stream().anyMatch(
                                                method2 -> method2.isAnnotationPresent(JavonoSetup.class) && method2.getNameAsString().equals(method)
                                        );
                                        if (isSetupCalledFromLoop) {
                                            System.err.println("\uD83D\uDE05 Nice try! @JavonoSetup is for setting the stage — not for looping the show.\n" +
                                                    "\n");
                                            System.exit(1);
                                        }
                                        if (isMethodCalledItself) {
                                            System.err.println("\uD83D\uDE05 Smart try! You can’t sneak in a call to @JavonoLoop from inside itself or from @JavonoSetup.\n"
                                                    + "Let it do its job in peace — it's called *loop* for a reason!\n");
                                            System.exit(1);
                                        }


                                    }
                            );
                        }

                        boolean hasCustomMethod = clazz.getMethods()
                                .stream()
                                .anyMatch(method -> method.isAnnotationPresent(JavonoLoop.class));

                        AtomicBoolean isNotAnnotatedCustomMethodFound = new AtomicBoolean(false);
                        AtomicReference<String> customMethodName = new AtomicReference<String>("null");
                        List<String> customMethodNames = new ArrayList<>();
                        clazz.getMethods().forEach(method -> {
                            if (!method.isAnnotationPresent(JavonoSetup.class) && !method.isAnnotationPresent(JavonoLoop.class) && !method.isAnnotationPresent(JavonoCustomMethod.class)) {
                                isNotAnnotatedCustomMethodFound.set(true);
                                customMethodName.set(method.getNameAsString());
                                customMethodNames.add(method.getNameAsString());
                            }
                        });


                        if (isNotAnnotatedCustomMethodFound.get()) {
                            customMethodNames.forEach(methodName -> System.err.print(methodName + " "));
                            System.err.println((customMethodNames.size() == 1 ? " method has " : " methods have ") + "not been annotated with any Javono annotation in " + clazz.getNameAsString() + ".java class");
                            System.err.println("Inside @JavonoSketch class every method must be annotated.");
                            System.exit(1);
                        } else {
                            System.out.println("@JavonoSketch class found Ok and class name is " + clazz.getName() + ".java");
                        }
                    }
                });

            } else {
                System.err.println("⚠️ Failed to parse " + file + ": " + result.getProblems());
            }

        } catch (IOException e) {
            System.err.println("⚠️ Failed to read " + file + ": " + e.getMessage());
        }
    }
}
