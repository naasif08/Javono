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
import java.util.*;
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

    private static void detectCustomMethodRecursion(ClassOrInterfaceDeclaration clazz, Path file) {
        Map<String, Set<String>> callGraph = new HashMap<>();

        // 1. Build call graph
        clazz.getMethods().stream()
                .filter(m -> m.isAnnotationPresent(JavonoCustomMethod.class))
                .forEach(method -> {
                    String methodName = method.getNameAsString();
                    Set<String> calls = new HashSet<>();

                    method.findAll(MethodCallExpr.class).forEach(call -> {
                        String calledMethod = call.getNameAsString();
                        // Only track calls to other custom methods
                        clazz.getMethodsByName(calledMethod).stream()
                                .filter(m2 -> m2.isAnnotationPresent(JavonoCustomMethod.class))
                                .findAny()
                                .ifPresent(matched -> calls.add(calledMethod));
                    });

                    callGraph.put(methodName, calls);
                });

        // 2. Detect cycles in call graph
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String method : callGraph.keySet()) {
            if (detectCycleDFS(method, callGraph, visited, recursionStack)) {
                System.err.println("[Javono] Oops! A @JavonoCustomMethod is stuck in a loop — either calling itself or bouncing back and forth with another method.\n" +
                        "[Javono] Recursion (direct or mutual) isn’t supported in Javono.\n" +
                        "[Javono] Keep your methods simple and loop-free, like a true embedded Zen master.");

                System.err.println("[Javono] Involved class: " + clazz.getNameAsString() + " (" + file.getFileName() + ")");
                System.exit(1);
            }
        }
    }

    // DFS to detect cycle
    private static boolean detectCycleDFS(String method,
                                          Map<String, Set<String>> graph,
                                          Set<String> visited,
                                          Set<String> stack) {
        if (stack.contains(method)) return true;
        if (visited.contains(method)) return false;

        visited.add(method);
        stack.add(method);

        for (String neighbor : graph.getOrDefault(method, Collections.emptySet())) {
            if (detectCycleDFS(neighbor, graph, visited, stack)) return true;
        }

        stack.remove(method);
        return false;
    }


    private static List<String> getInvokedMethodsInsideAnnotatedMethod(ClassOrInterfaceDeclaration clazz, Class<? extends Annotation> annotationClass) {
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
            System.err.println("[Javono] No class annotated with @Sketch found.");
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
                            System.err.println("[Javono] No @JavonoSetup annotation found.");
                            System.exit(1);
                        } else {
                            getInvokedMethodsInsideAnnotatedMethod(clazz, JavonoSetup.class).forEach(
                                    method -> {
                                        boolean isMethodCalledItself = clazz.getMethods().stream().anyMatch(
                                                method2 -> method2.isAnnotationPresent(JavonoSetup.class) && method2.getNameAsString().equals(method)
                                        );
                                        boolean isLoopCalledFromSetup = clazz.getMethods().stream().anyMatch(
                                                method2 -> method2.isAnnotationPresent(JavonoLoop.class) && method2.getNameAsString().equals(method)
                                        );
                                        if (isLoopCalledFromSetup) {
                                            System.err.println("[Javono] \uD83D\uDE05 Nice try! don't call @JavonoLoop from @JavonoSetup — this is for setting up things.\n" +
                                                    "\n");
                                            System.exit(1);
                                        }
                                        if (isMethodCalledItself) {
                                            System.err.println("[Javono] \uD83D\uDE05 Smart try! You can’t sneak in a call to @JavonoSetup from inside itself or from @JavonoLoop.\n"
                                                    + "[Javono] Let it do its job in peace — it's called *setup* for a reason!\n");
                                            System.exit(1);
                                        }
                                    }
                            );
                        }


                        boolean hasLoop = clazz.getMethods()
                                .stream()
                                .anyMatch(method -> method.isAnnotationPresent(JavonoLoop.class));
                        if (!hasLoop) {
                            System.err.println("[Javono] No @JavonoLoop annotation found.");
                            System.exit(1);
                        } else {
                            getInvokedMethodsInsideAnnotatedMethod(clazz, JavonoLoop.class).forEach(
                                    method -> {
                                        boolean isMethodCalledItself = clazz.getMethods().stream().anyMatch(
                                                method2 -> method2.isAnnotationPresent(JavonoLoop.class) && method2.getNameAsString().equals(method)
                                        );
                                        boolean isSetupCalledFromLoop = clazz.getMethods().stream().anyMatch(
                                                method2 -> method2.isAnnotationPresent(JavonoSetup.class) && method2.getNameAsString().equals(method)
                                        );
                                        if (isSetupCalledFromLoop) {
                                            System.err.println("[Javono] \uD83D\uDE05 Nice try! @JavonoSetup is for setting the stage — not for looping the show.\n" +
                                                    "\n");
                                            System.exit(1);
                                        }
                                        if (isMethodCalledItself) {
                                            System.err.println("[Javono] \uD83D\uDE05 Smart try! You can’t sneak in a call to @JavonoLoop from inside itself or from @JavonoSetup.\n"
                                                    + "[Javono] Let it do its job in peace — it's called *loop* for a reason!\n");
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
                            System.err.printf("[Javono] ");
                            customMethodNames.forEach(methodName -> System.err.print(methodName + " "));
                            System.err.println((customMethodNames.size() == 1 ? " method has " : " methods have ") + "not been annotated with any Javono annotation in " + clazz.getNameAsString() + ".java class");
                            System.err.println("[Javono] Inside @JavonoSketch class every method must be annotated.");
                            System.exit(1);
                        } else {
                            boolean isRecursionFoundInsideCustomMethod = clazz.getMethods().stream().anyMatch(
                                    methodDeclaration ->
                                            methodDeclaration.getBody().isPresent() &&
                                                    methodDeclaration.getBody().get()
                                                            .findAll(MethodCallExpr.class)
                                                            .stream()
                                                            .anyMatch(methodCall -> methodCall.getNameAsString().equals(methodDeclaration.getNameAsString()))
                            );


                            if (isRecursionFoundInsideCustomMethod) {
                                System.err.println("[Javono] Gentle reminder: A @JavonoCustomMethod seems to be calling itself.\n" +
                                        "[Javono] While recursion is clever, Javono encourages a simpler path.\n" +
                                        "[Javono] Let’s keep our methods well-behaved and avoid infinite loops!");
                                System.exit(1);
                            }
                            detectCustomMethodRecursion(clazz, file);

                        }
                        System.out.println("[Javono] @JavonoSketch class found Ok and class name is " + clazz.getName() + ".java");
                    }
                });

            } else {
                System.err.println("[Javono] Failed to parse " + file + ": " + result.getProblems());
            }

        } catch (IOException e) {
            System.err.println("[Javono] Failed to read " + file + ": " + e.getMessage());
        }
    }
}
