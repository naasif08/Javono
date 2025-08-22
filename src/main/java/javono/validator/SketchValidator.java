package javono.validator;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import javono.annotations.JavonoEmbeddedUserMethod;
import javono.annotations.JavonoEmbeddedLoop;
import javono.annotations.JavonoEmbeddedInit;
import javono.annotations.JavonoEmbeddedSketch;
import javono.logger.LoggerFacade;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

class SketchValidator {

    private static final SketchValidator INSTANCE = new SketchValidator();
    private final Path userProjectSrcDir = detectUniversalJavaSourceDir();
    private int classCount = 0;
    private String className = "Class not found.";

    // Create a shared parser instance configured for Java 21
    private static final JavaParser parser;

    static {
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        parser = new JavaParser(config);
    }

    // Private constructor to prevent external instantiation
    private SketchValidator() {
    }

    // Global access point
    public static SketchValidator getInstance() {
        return INSTANCE;
    }

    private static void detectCustomMethodRecursion(ClassOrInterfaceDeclaration clazz, Path file) {
        Map<String, Set<String>> callGraph = new HashMap<>();

        // 1. Build call graph
        clazz.getMethods().stream().filter(m -> m.isAnnotationPresent(JavonoEmbeddedUserMethod.class))
                .forEach(method -> {
                    String methodName = method.getNameAsString();
                    Set<String> calls = new HashSet<>();

                    method.findAll(MethodCallExpr.class).forEach(call -> {
                        String calledMethod = call.getNameAsString();
                        // Only track calls to other custom methods
                        clazz.getMethodsByName(calledMethod).stream()
                                .filter(m2 -> m2.isAnnotationPresent(JavonoEmbeddedUserMethod.class))
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
                System.err.println("[Javono] Oops! A @JavonoEmbeddedUserMethod is stuck in a loop — either calling itself or bouncing back and forth with another method.\n" + "[Javono] Recursion (direct or mutual) isn’t supported in Javono.\n" + "[Javono] Keep your methods simple and loop-free, like a true embedded Zen master.");

                System.err.println("[Javono] Involved class: " + clazz.getNameAsString() + " (" + file.getFileName() + ")");
                System.exit(1);
            }
        }
    }

    // DFS to detect cycle
    private static boolean detectCycleDFS(String method, Map<String, Set<String>> graph, Set<String> visited, Set<String> stack) {
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
        clazz.getMethods().forEach(methodDeclaration -> {
            if (!methodDeclaration.getBody().isEmpty() && methodDeclaration.isAnnotationPresent(annotationClass)) {
                methodDeclaration.findAll(MethodCallExpr.class)
                        .forEach(invokedMethod -> methodsWhichAreInvoked.add(invokedMethod.getNameAsString()));
            }
        });
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

    public void validateProject() {
        AtomicBoolean sketchFound = new AtomicBoolean(false);

        try {
            Files.walk(this.userProjectSrcDir)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(file -> validateFile(file, sketchFound));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!sketchFound.get()) {
            LoggerFacade.getInstance().error("No class annotated with @JavonoEmbeddedSketch found.");
            System.exit(1);
        }
        if (checkAndDeleteProcessorMarker()) {
            LoggerFacade.getInstance().info("@JavonoEmbeddedSketch class found and class name is " + className + ".java");
        } else {
            System.exit(1);
        }
    }

    public static boolean checkAndDeleteProcessorMarker() {
        try {
            // Try to locate the marker file in the classpath
            URL markerUrl = ClassLoader.getSystemResource("javono-processor.marker");
            if (markerUrl == null) {
                LoggerFacade.getInstance().error(
                        "Annotation processor not detected! "
                                + "\nTo use Javono correctly, please enable annotation processing in your IDE "
                                + "\n(For example, in IntelliJ: Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable) "
                                + "\nor in your build tool (Maven/Gradle)."
                );
                return false;
            }

            LoggerFacade.getInstance().info("Annotation processor detected.");

            return true;

        } catch (Exception e) {
            throw new RuntimeException("Failed to check annotation processor status.", e);
        }
    }

    public static List<String> listClassNamesFromLib() throws IOException {
        List<String> classNames = new ArrayList<>();
        String libPackagePath = "javono/lib";

        URL url = Thread.currentThread().getContextClassLoader().getResource(libPackagePath);
        if (url == null) {
            throw new FileNotFoundException("Resource not found: " + libPackagePath);
        }

        if (url.getProtocol().equals("file")) {
            // Running from IDE or unpackaged class folder
            File libDir = new File(url.getPath());
            if (libDir.exists()) {
                for (File file : Objects.requireNonNull(libDir.listFiles())) {
                    if (file.getName().endsWith(".class")) {
                        String className = file.getName().replace(".class", "");
                        classNames.add(className);
                    }
                }
            }
        } else if (url.getProtocol().equals("jar")) {
            // Running from inside a JAR
            String jarPath = url.getPath().substring(5, url.getPath().indexOf("!"));
            try (JarFile jarFile = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(libPackagePath) && name.endsWith(".class")) {
                        String className = name.substring(name.lastIndexOf('/') + 1).replace(".class", "");
                        classNames.add(className);
                    }
                }
            }
        }

        return classNames;
    }


    private void validateFile(Path file, AtomicBoolean sketchFound) {
        try {
            // Use the configured parser here instead of StaticJavaParser
            ParseResult<CompilationUnit> result = parser.parse(file);

            if (result.isSuccessful() && result.getResult().isPresent()) {
                CompilationUnit cu = result.getResult().get();

                cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
                    if (clazz.isAnnotationPresent(JavonoEmbeddedSketch.class)) {
                        sketchFound.set(true);

                        clazz.findAll(TryStmt.class).forEach(tryStmt -> {
                            LoggerFacade.getInstance().error("try-catch blocks are not allowed in @JavonoEmbeddedSketch classes.");
                            System.exit(1);
                        });


                        clazz.getMethods().forEach(method -> {
                            if (!method.getThrownExceptions().isEmpty()) {
                                LoggerFacade.getInstance().error("Methods in @JavonoEmbeddedSketch class must not declare any thrown exceptions: " + method.getNameAsString());
                                System.exit(1);
                            }
                        });


                        //Starts: this is to restrict users to outside methods
                        // 1. Collect all method names declared in this class
                        Set<String> localMethodNames = clazz.getMethods()
                                .stream()
                                .map(m -> m.getNameAsString())
                                .collect(Collectors.toSet());

                        // 2. Collect all javono.lib imports
                        boolean wildcardImport = cu.getImports()
                                .stream()
                                .anyMatch(imp -> imp.getNameAsString().equals("javono.lib") && imp.isAsterisk());


                        Set<String> allowedImportedClassNames = cu.getImports().stream().map(NodeWithName::getNameAsString)  // e.g., javono.lib.GPIO
                                .filter(name -> name.startsWith("javono.lib"))
                                .map(name -> name.substring(name.lastIndexOf('.') + 1))
                                .collect(Collectors.toSet());

                        allowedImportedClassNames.removeIf(name -> name.equals("lib"));


                        if (wildcardImport) {
                            File libFolder = new File("src/main/java/javono/lib");// adjust path as needed

                            if (libFolder.exists() && libFolder.isDirectory()) {

                                for (File file2 : Objects.requireNonNull(libFolder.listFiles())) {

                                    if (file2.getName().endsWith(".java")) {
                                        String className = file2.getName().replace(".java", "");
                                        allowedImportedClassNames.add(className);
                                    }
                                }
                            } else {
                                try {
                                    allowedImportedClassNames.addAll(listClassNamesFromLib());
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }

                        // 3. Validate method calls
                        clazz.getMethods().forEach(method -> {
                            method.findAll(MethodCallExpr.class)
                                    .forEach(call -> {
                                        if (call.hasScope()) {
                                            Optional<Expression> scope = call.getScope();

                                            Expression scp = scope.orElse(null);
                                            String variableName = scp.toString();

                                            // Find the variable declaration matching this name
                                            Optional<String> className = call.findAncestor(CompilationUnit.class)
                                                    .flatMap(cu2 -> cu2.findAll(VariableDeclarator.class)
                                                            .stream()
                                                            .filter(vd -> vd.getNameAsString().equals(variableName))
                                                            .map(vd -> vd.getType().asString()) // e.g., "GPIO"
                                                            .findFirst());
                                            if (className.isPresent() && allowedImportedClassNames.contains(className.get())) {
                                                Expression sc = scope.orElse(null);
                                                String scopeStr = sc.toString();
                                                allowedImportedClassNames.add(scopeStr);
                                            }


                                            if (scope.isEmpty() || scope.get().isThisExpr()) {
                                                // Unscoped or this → must be local
                                                if (!localMethodNames.contains(call.getNameAsString())) {
                                                    LoggerFacade.getInstance().error("Call to undefined local method: " + call.getNameAsString());
                                                    System.exit(1);
                                                }
                                            } else {
                                                String scopeText = scope.get().toString();
                                                if (!allowedImportedClassNames.contains(scopeText)) {
                                                    LoggerFacade.getInstance().error("External method call not allowed: " + call);
                                                    System.exit(1);
                                                }
                                            }
                                        }
                                    });
                        });
                        //Ends: this is to restrict users to outside methods

                        clazz.getMethods().forEach(method -> {
                            method.getBody().ifPresent(body -> {
                                body.findAll(VariableDeclarator.class)
                                        .forEach(var -> {
                                            String typeName = var.getType().asString();
                                            if (!typeName.equals("int") && !typeName.equals("float") && !typeName.equals("char") && !typeName.equals("JavonoString") && !isAllowedJavonoType(typeName)) {
                                                LoggerFacade.getInstance().error("Invalid local variable type detected!\n" + "  Found type: " + typeName + "\n" + "Allowed types are: int, float, char, JavonoString, or classes from javono.lib");
                                                System.exit(1);
                                            }
                                        });
                            });
                        });


                        boolean hasSetup = clazz.getMethods()
                                .stream()
                                .anyMatch(method -> method.isAnnotationPresent(JavonoEmbeddedInit.class));

                        if (!hasSetup) {
                            System.err.println("[Javono] No @JavonoEmbeddedInit annotation found.");
                            System.exit(1);
                        } else {
                            getInvokedMethodsInsideAnnotatedMethod(clazz, JavonoEmbeddedInit.class)
                                    .forEach(method -> {
                                        boolean isMethodCalledItself = clazz.getMethods()
                                                .stream()
                                                .anyMatch(method2 -> method2.isAnnotationPresent(JavonoEmbeddedInit.class) && method2.getNameAsString().equals(method));

                                        boolean isLoopCalledFromSetup = clazz.getMethods()
                                                .stream()
                                                .anyMatch(method2 -> method2.isAnnotationPresent(JavonoEmbeddedLoop.class) && method2.getNameAsString().equals(method));

                                        if (isLoopCalledFromSetup) {
                                            System.err.println("[Javono] \uD83D\uDE05 Nice try! don't call @JavonoEmbeddedLoop from @JavonoEmbeddedInit — this is for setting up things.\n" + "\n");
                                            System.exit(1);
                                        }
                                        if (isMethodCalledItself) {
                                            System.err.println("[Javono] \uD83D\uDE05 Smart try! You can’t sneak in a call to @JavonoEmbeddedInit from inside itself or from @JavonoEmbeddedLoop.\n" + "[Javono] Let it do its job in peace — it's called *setup* for a reason!\n");
                                            System.exit(1);
                                        }
                                    });
                        }


                        boolean hasLoop = clazz.getMethods()
                                .stream()
                                .anyMatch(method -> method.isAnnotationPresent(JavonoEmbeddedLoop.class));

                        if (!hasLoop) {
                            System.err.println("[Javono] No @JavonoEmbeddedLoop annotation found.");
                            System.exit(1);
                        } else {
                            getInvokedMethodsInsideAnnotatedMethod(clazz, JavonoEmbeddedLoop.class)
                                    .forEach(method -> {

                                        boolean isMethodCalledItself = clazz.getMethods()
                                                .stream()
                                                .anyMatch(method2 -> method2.isAnnotationPresent(JavonoEmbeddedLoop.class) && method2.getNameAsString().equals(method));

                                        boolean isSetupCalledFromLoop = clazz.getMethods()
                                                .stream()
                                                .anyMatch(method2 -> method2.isAnnotationPresent(JavonoEmbeddedInit.class) && method2.getNameAsString().equals(method));

                                        if (isSetupCalledFromLoop) {
                                            System.err.println("[Javono] \uD83D\uDE05 Nice try! @JavonoEmbeddedInit is for setting the stage — not for looping the show.\n" + "\n");
                                            System.exit(1);
                                        }
                                        if (isMethodCalledItself) {
                                            System.err.println("[Javono] \uD83D\uDE05 Smart try! You can’t sneak in a call to @JavonoEmbeddedLoop from inside itself or from @JavonoEmbeddedInit.\n" + "[Javono] Let it do its job in peace — it's called *loop* for a reason!\n");
                                            System.exit(1);
                                        }


                                    });
                        }

                        boolean hasCustomMethod = clazz.getMethods()
                                .stream()
                                .anyMatch(method -> method.isAnnotationPresent(JavonoEmbeddedLoop.class));

                        AtomicBoolean isNotAnnotatedCustomMethodFound = new AtomicBoolean(false);
                        AtomicReference<String> customMethodName = new AtomicReference<String>("null");
                        List<String> customMethodNames = new ArrayList<>();
                        clazz.getMethods()
                                .forEach(method -> {
                                    if (!method.isAnnotationPresent(JavonoEmbeddedInit.class) && !method.isAnnotationPresent(JavonoEmbeddedLoop.class) && !method.isAnnotationPresent(JavonoEmbeddedUserMethod.class)) {
                                        isNotAnnotatedCustomMethodFound.set(true);
                                        customMethodName.set(method.getNameAsString());
                                        customMethodNames.add(method.getNameAsString());
                                    }
                                });


                        if (isNotAnnotatedCustomMethodFound.get()) {
                            System.err.printf("[Javono] ");
                            customMethodNames.forEach(methodName -> System.err.print(methodName + " "));
                            System.err.println((customMethodNames.size() == 1 ? " method has " : " methods have ") + "not been annotated with any Javono annotation in " + clazz.getNameAsString() + ".java class");
                            System.err.println("[Javono] Inside @JavonoEmbeddedSketch class every method must be annotated.");
                            System.exit(1);
                        } else {
                            getInvokedMethodsInsideAnnotatedMethod(clazz, JavonoEmbeddedUserMethod.class).forEach(customMethod -> {

                                boolean isSetupMethodCalledFromCustomMethod = clazz.getMethods()
                                        .stream()
                                        .anyMatch(methodDeclaration -> methodDeclaration.isAnnotationPresent(JavonoEmbeddedInit.class) && methodDeclaration.getNameAsString().equals(customMethod));

                                boolean isLoopMethodCalledFromCustomMethod = clazz.getMethods()
                                        .stream()
                                        .anyMatch(methodDeclaration -> methodDeclaration.isAnnotationPresent(JavonoEmbeddedLoop.class) && methodDeclaration.getNameAsString().equals(customMethod));


                                if (isSetupMethodCalledFromCustomMethod) {
                                    System.err.println("[Javono] Nope! @JavonoEmbeddedInit can't be summoned like a Pokémon from a @JavonoEmbeddedUserMethod.");
                                    System.err.println("[Javono] Let the setup method do set up things. You do you.");
                                    System.exit(1);
                                }

                                if (isLoopMethodCalledFromCustomMethod) {
                                    System.err.println("[Javono] Nope! @JavonoEmbeddedLoop can't be summoned like a Pokémon from a @JavonoEmbeddedUserMethod.");
                                    System.err.println("[Javono] Let the loop do the looping. You do you.");
                                    System.exit(1);
                                }
                            });


                            boolean isConstructorFound = !clazz.getConstructors().isEmpty();
                            if (isConstructorFound) {
                                System.err.println("[Javono] Sketch class must not declare any constructors. Please remove it and use @JavonoEmbeddedInit instead.\n");
                                System.exit(1);
                            }


                            boolean isRecursionFoundInsideCustomMethod = clazz.getMethods()
                                    .stream()
                                    .anyMatch(methodDeclaration ->
                                            methodDeclaration.getBody().isPresent() && methodDeclaration.getBody().get().findAll(MethodCallExpr.class)
                                                    .stream()
                                                    .anyMatch(methodCall -> methodCall.getNameAsString().equals(methodDeclaration.getNameAsString())));


                            if (isRecursionFoundInsideCustomMethod) {
                                System.err.println("[Javono] Gentle reminder: A @JavonoEmbeddedUserMethod seems to be calling itself.\n" + "[Javono] While recursion is clever, Javono encourages a simpler path.\n" + "[Javono] Let’s keep our methods well-behaved and avoid infinite loops!");
                                System.exit(1);
                            }
                            detectCustomMethodRecursion(clazz, file);

                        }

                        classCount = classCount + 1;
                        if (classCount > 1) {
                            LoggerFacade.getInstance().error("More than one class found having @JavonoEmbeddedSketch annotation and class name is " + clazz.getNameAsString() + ".java" + " and please use only one.");
                            System.exit(1);
                        } else if (classCount == 1) {
                            this.className = clazz.getNameAsString();
                        }

                    }
                });

            } else {
                System.err.println("[Javono] Failed to parse " + file + ": " + result.getProblems());
            }

        } catch (IOException e) {
            System.err.println("[Javono] Failed to read " + file + ": " + e.getMessage());
        }
    }

    private static boolean isAllowedJavonoType(String typeName) {
        try {
            Set<String> libClasses = new HashSet<>();
            String path = "javono/lib";

            // Try to read from JAR or classpath
            URL url = Thread.currentThread().getContextClassLoader().getResource(path);
            if (url != null) {
                if ("jar".equals(url.getProtocol())) {
                    String jarPath = URLDecoder.decode(url.getPath().substring(5, url.getPath().indexOf("!")), "UTF-8");
                    try (JarFile jarFile = new JarFile(jarPath)) {
                        Enumeration<JarEntry> entries = jarFile.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            if (entry.getName().startsWith(path) && entry.getName().endsWith(".class")) {
                                libClasses.add(entry.getName().substring(entry.getName().lastIndexOf('/') + 1, entry.getName().length() - 6));
                            }
                        }
                    }
                } else if ("file".equals(url.getProtocol())) {
                    File folder = new File(url.toURI());
                    for (File f : Objects.requireNonNull(folder.listFiles())) {
                        if (f.getName().endsWith(".class")) {
                            libClasses.add(f.getName().replace(".class", ""));
                        }
                    }
                }
            }

            return libClasses.contains(typeName);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
