package javono.annotations.processor;

import javono.annotations.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.Writer;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@SupportedAnnotationTypes({"javono.annotations.JavonoEmbeddedSketch", "javono.annotations.JavonoEmbeddedInit", "javono.annotations.JavonoEmbeddedLoop", "javono.annotations.JavonoEmbeddedUserMethod"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class AnnotationProcessor extends AbstractProcessor {

    // Accumulate all @JavonoEmbeddedSketch annotated classes across rounds
    private final Set<Element> allSketches = new HashSet<>();

    private boolean setupFound = false;
    private boolean loopFound = false;
    private boolean JavonoEmbeddedSketchFound = false;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            // Final validation after all rounds are done
            if (allSketches.size() != 1) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] Exactly one class must be annotated with @JavonoEmbeddedSketch, found: " + allSketches.size());
            }
            createProcessorMarker();
            return false;
        }



        // Accumulate sketches found this round
        Set<? extends Element> sketchesThisRound = roundEnv.getElementsAnnotatedWith(JavonoEmbeddedSketch.class);
        allSketches.addAll(sketchesThisRound);

        if (allSketches.size() > 1) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] More than one class annotated with @JavonoEmbeddedSketch found.");
            // We continue processing to show more errors, but this is a fatal situation
        }

        if (allSketches.size() == 1) {
            setJavonoEmbeddedSketchFound(true);
            Element sketchClass = allSketches.iterator().next();
            if (!sketchClass.getModifiers().contains(Modifier.PUBLIC)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] @JavonoEmbeddedSketch class must be public.");
            }
            if (sketchClass.getModifiers().contains(Modifier.ABSTRACT)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] @JavonoEmbeddedSketch class must not be abstract.");
            }

            if (sketchClass instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) sketchClass;

                validateUnannotatedMethods(typeElement);

                // Check for extends (other than Object)
                String superClassName = typeElement.getSuperclass().toString();
                if (!superClassName.equals("java.lang.Object")) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] Classes annotated with @JavonoEmbeddedSketch cannot extend other classes. Please keep it standalone.", sketchClass);
                }

                // Check for implements
                if (!typeElement.getInterfaces().isEmpty()) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] Classes annotated with @JavonoEmbeddedSketch cannot implement interfaces. Keep it simple and pure!", sketchClass);
                }
            }


            // Validate @JavonoEmbeddedInit methods
            Set<? extends Element> setupMethods = roundEnv.getElementsAnnotatedWith(JavonoEmbeddedInit.class);
            if (setupMethods.size() != 1) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] Exactly one method must be annotated with @JavonoEmbeddedInit, found: " + setupMethods.size());
            }
            for (Element setup : setupMethods) {
                validateMethodInSketchClass(setup, sketchClass, "@JavonoEmbeddedInit");
                validateVoidMethod(setup, "@JavonoEmbeddedInit");
                setSetupFound(true);
            }

            // Validate @JavonoEmbeddedLoop methods
            Set<? extends Element> loopMethods = roundEnv.getElementsAnnotatedWith(JavonoEmbeddedLoop.class);
            if (loopMethods.size() != 1) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] Exactly one method must be annotated with @JavonoEmbeddedLoop, found: " + loopMethods.size());
            }
            for (Element loop : loopMethods) {
                validateMethodInSketchClass(loop, sketchClass, "@JavonoEmbeddedLoop");
                validateVoidMethod(loop, "@JavonoEmbeddedLoop");
                setLoopFound(true);
            }

            // Validate @JavonoEmbeddedUserMethod methods are inside sketch class
            Set<? extends Element> customMethods = roundEnv.getElementsAnnotatedWith(JavonoEmbeddedUserMethod.class);
            for (Element custom : customMethods) {
                if (!custom.getEnclosingElement().equals(sketchClass)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] @JavonoEmbeddedUserMethod methods must be inside the @JavonoEmbeddedSketch class", custom);
                }
            }

            for (Element custom : customMethods) {
                if (!custom.getEnclosingElement().equals(sketchClass)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] @JavonoEmbeddedUserMethod methods must be inside the @JavonoEmbeddedSketch class", custom);
                }

                // Check that the method is private
                Set<Modifier> modifiers = custom.getModifiers();
                if (!modifiers.contains(Modifier.PRIVATE)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] @JavonoEmbeddedUserMethod methods must be private", custom);
                }

                // Check that the method is NOT static
                if (modifiers.contains(Modifier.STATIC)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] @JavonoEmbeddedUserMethod methods must NOT be static", custom);
                }
            }

            for (Element element : roundEnv.getElementsAnnotatedWith(JavonoEmbeddedSketch.class)) {
                if (element.getKind() == ElementKind.CLASS) {
                    TypeElement sketchClazz = (TypeElement) element;
                    validateFields(sketchClazz);
                    validateMethodSignatures(sketchClazz);  // Mainly validating users custom methods
                    validateNoInnerClasses(sketchClazz);
                }
            }

        }

        return true;
    }


    private void validateNoInnerClasses(TypeElement sketchClass) {
        for (Element enclosed : sketchClass.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.CLASS || enclosed.getKind() == ElementKind.INTERFACE) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] Inner or nested classes or anything this type are not allowed inside a @JavonoEmbeddedSketch class.", enclosed);
            }
        }
    }


    private void validateMethodInSketchClass(Element method, Element sketchClass, String annotationName) {
        if (!method.getEnclosingElement().equals(sketchClass)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] " + annotationName + " method must be inside the @JavonoEmbeddedSketch class", method);
        }
        if (method.getKind() != ElementKind.METHOD) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] " + annotationName + " can only be applied to methods", method);
        }
    }

    private void validateVoidMethod(Element method, String annotationName) {
        if (method.getKind() == ElementKind.METHOD) {
            ExecutableElement executable = (ExecutableElement) method;

            // Check return type
            if (executable.getReturnType().getKind() != TypeKind.VOID) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] " + annotationName + " method must have a void return type.", method);
            }

            // Check if method is private
            if (!method.getModifiers().contains(Modifier.PRIVATE)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] " + annotationName + " method must be private. Keep it secret, keep it safe.", method);
            }

            if (!executable.getParameters().isEmpty()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] " + annotationName + " method must not take any parameters.", method);
            }
            long count = method.getAnnotationMirrors().stream().filter(mirror -> mirror.getAnnotationType().toString().startsWith("javono.annotations")).count();
            if (count > 1) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] A method must not have multiple Javono annotations.", method);
            }

            if (method.getModifiers().contains(Modifier.STATIC)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] @" + annotationName + " methods must not be static.", method);
            }
        }
    }


    private void validateUnannotatedMethods(TypeElement sketchClass) {
        List<? extends Element> enclosedElements = sketchClass.getEnclosedElements();

        for (Element element : enclosedElements) {
            if (element.getKind() == ElementKind.METHOD) {
                boolean isAnnotated = element.getAnnotation(JavonoEmbeddedLoop.class) != null || element.getAnnotation(JavonoEmbeddedInit.class) != null || element.getAnnotation(JavonoEmbeddedUserMethod.class) != null;

                if (!isAnnotated) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] Every method inside a @JavonoEmbeddedSketch class must be annotated with @JavonoEmbeddedLoop, @JavonoEmbeddedInit, or @JavonoEmbeddedUserMethod.", element);
                }
            }
        }
    }

    private void validateMethodSignatures(TypeElement sketchClass) {
        for (Element enclosed : sketchClass.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) enclosed;

                // Skip constructors
                if (method.getSimpleName().toString().equals("<init>")) continue;


                // Validate return type
                TypeMirror returnType = method.getReturnType();
                if (!returnType.getKind().equals(TypeKind.VOID) && !isAllowedReturnType(returnType)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Invalid return type. Method name: " + method.getSimpleName() + " and return type: " + returnType + ". \n    Allowed return types are int, float, boolean, char and JavonoString.", method);
                }

                // Validate parameter types
                for (VariableElement param : method.getParameters()) {
                    if (!param.asType().getKind().equals(TypeKind.VOID) && !isAllowedParameterType(param.asType())) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Invalid method parameter type. Method name: " + method.getSimpleName() + " and parameter type: " + param.asType() + ". \n    Allowed parameter types are int, float, boolean, char, JavonoString, and javono.lib.*", param);
                    }
                }
            }
        }
    }

    private void createProcessorMarker() {
        try {
            // Create a simple marker file in CLASS_OUTPUT (target/classes)
            FileObject marker = processingEnv.getFiler()
                    .createResource(StandardLocation.CLASS_OUTPUT, "", "javono-processor.marker");
            try (Writer writer = marker.openWriter()) {
                writer.write("Processor ran successfully at " + System.currentTimeMillis());
            }
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "[Javono] Failed to create processor marker: " + e.getMessage());
        }
    }

    private void validateFields(Element sketchClass) {
        for (Element enclosed : sketchClass.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;
                TypeMirror fieldType = field.asType();
                Set<Modifier> modifiers = field.getModifiers();

                Set<Modifier> allowed = Set.of(Modifier.PRIVATE, Modifier.FINAL);

// Default (package-private) means: no access modifier (i.e., not public/protected/private)
                boolean hasIllegalModifier = modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.PROTECTED) || modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.TRANSIENT) || modifiers.contains(Modifier.VOLATILE) || modifiers.contains(Modifier.SYNCHRONIZED);

                boolean isOnlyAllowed = modifiers.stream().allMatch(mod -> allowed.contains(mod));

                if (hasIllegalModifier || !isOnlyAllowed) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] You aren't allowed to use public, static, protected modifiers.", field);
                }


                if (!isAllowedFieldType(fieldType)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] '" + field.getSimpleName() + "' is not allowed type field : " + fieldType.toString() + "\n" + "    Please try to use int, float, boolean, char and javono.lib.*", field);
                }
            }
        }
    }

    private boolean isAllowedFieldType(TypeMirror type) {

        // Allow primitive types
        if (type.getKind() == TypeKind.INT || type.getKind() == TypeKind.FLOAT || type.getKind() == TypeKind.BOOLEAN || type.getKind() == TypeKind.CHAR) {
            return true;
        }

        // Allow types in javono.lib package
        String typeName = type.toString();
        if (typeName.startsWith("javono.lib.")) {
            return true;
        }

        return false;
    }


    private boolean isAllowedReturnType(TypeMirror type) {
        TypeKind kind = type.getKind();

        // Allow primitives
        if (kind == TypeKind.INT || kind == TypeKind.FLOAT || kind == TypeKind.BOOLEAN || kind == TypeKind.CHAR) {
            return true;
        }

        // Check reference types (like String, or anything in javono.lib.*)
        String typeStr = type.toString();
        if (typeStr.startsWith("javono.lib.JavonoString")) return true;

        return false;
    }


    private boolean isAllowedParameterType(TypeMirror type) {
        TypeKind kind = type.getKind();

        // Allow primitives
        if (kind == TypeKind.INT || kind == TypeKind.FLOAT || kind == TypeKind.BOOLEAN || kind == TypeKind.CHAR) {
            return true;
        }

        // Check reference types (like String, or anything in javono.lib.*)
        String typeStr = type.toString();
        if (typeStr.startsWith("javono.lib.")) return true;

        return false;
    }

    public boolean isLoopFound() {
        return loopFound;
    }

    public void setLoopFound(boolean loopFound) {
        this.loopFound = loopFound;
    }

    public boolean isSetupFound() {
        return setupFound;
    }

    public void setSetupFound(boolean setupFound) {
        this.setupFound = setupFound;
    }

    public boolean isJavonoEmbeddedSketchFound() {
        return JavonoEmbeddedSketchFound;
    }

    public void setJavonoEmbeddedSketchFound(boolean JavonoEmbeddedSketchFound) {
        this.JavonoEmbeddedSketchFound = JavonoEmbeddedSketchFound;
    }
}
