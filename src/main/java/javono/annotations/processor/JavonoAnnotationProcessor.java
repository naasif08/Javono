package javono.annotations.processor;

import javono.annotations.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@SupportedAnnotationTypes({"javono.annotations.JavonoSketch", "javono.annotations.JavonoSetup", "javono.annotations.JavonoLoop", "javono.annotations.JavonoCustomMethod"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class JavonoAnnotationProcessor extends AbstractProcessor {

    // Accumulate all @JavonoSketch annotated classes across rounds
    private final Set<Element> allSketches = new HashSet<>();

    private boolean setupFound = false;
    private boolean loopFound = false;
    private boolean javonoSketchFound = false;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            // Final validation after all rounds are done
            if (allSketches.size() != 1) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] Exactly one class must be annotated with @JavonoSketch, found: " + allSketches.size());
            }
            return false;
        }

        // Accumulate sketches found this round
        Set<? extends Element> sketchesThisRound = roundEnv.getElementsAnnotatedWith(JavonoSketch.class);
        allSketches.addAll(sketchesThisRound);

        if (allSketches.size() > 1) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] More than one class annotated with @JavonoSketch found.");
            // We continue processing to show more errors, but this is a fatal situation
        }

        if (allSketches.size() == 1) {
            setJavonoSketchFound(true);
            Element sketchClass = allSketches.iterator().next();
            if (!sketchClass.getModifiers().contains(Modifier.PUBLIC)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] @JavonoSketch class must be public.");
            }
            if (sketchClass.getModifiers().contains(Modifier.ABSTRACT)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] @JavonoSketch class must not be abstract.");
            }

            if (sketchClass instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) sketchClass;

                validateUnannotatedMethods(typeElement);

                // Check for extends (other than Object)
                String superClassName = typeElement.getSuperclass().toString();
                if (!superClassName.equals("java.lang.Object")) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] Classes annotated with @JavonoSketch cannot extend other classes. Please keep it standalone.", sketchClass);
                }

                // Check for implements
                if (!typeElement.getInterfaces().isEmpty()) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] Classes annotated with @JavonoSketch cannot implement interfaces. Keep it simple and pure!", sketchClass);
                }
            }


            // Validate @JavonoSetup methods
            Set<? extends Element> setupMethods = roundEnv.getElementsAnnotatedWith(JavonoSetup.class);
            if (setupMethods.size() != 1) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] Exactly one method must be annotated with @JavonoSetup, found: " + setupMethods.size());
            }
            for (Element setup : setupMethods) {
                validateMethodInSketchClass(setup, sketchClass, "@JavonoSetup");
                validateVoidMethod(setup, "@JavonoSetup");
                setSetupFound(true);
            }

            // Validate @JavonoLoop methods
            Set<? extends Element> loopMethods = roundEnv.getElementsAnnotatedWith(JavonoLoop.class);
            if (loopMethods.size() != 1) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] Exactly one method must be annotated with @JavonoLoop, found: " + loopMethods.size());
            }
            for (Element loop : loopMethods) {
                validateMethodInSketchClass(loop, sketchClass, "@JavonoLoop");
                validateVoidMethod(loop, "@JavonoLoop");
                setLoopFound(true);
            }

            // Validate @JavonoCustomMethod methods are inside sketch class
            Set<? extends Element> customMethods = roundEnv.getElementsAnnotatedWith(JavonoCustomMethod.class);
            for (Element custom : customMethods) {
                if (!custom.getEnclosingElement().equals(sketchClass)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] @JavonoCustomMethod methods must be inside the @JavonoSketch class", custom);
                }
            }

            for (Element custom : customMethods) {
                if (!custom.getEnclosingElement().equals(sketchClass)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] @JavonoCustomMethod methods must be inside the @JavonoSketch class", custom);
                }

                // Check that the method is private
                Set<Modifier> modifiers = custom.getModifiers();
                if (!modifiers.contains(Modifier.PRIVATE)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] @JavonoCustomMethod methods must be private", custom);
                }

                // Check that the method is NOT static
                if (modifiers.contains(Modifier.STATIC)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] @JavonoCustomMethod methods must NOT be static", custom);
                }
            }

            for (Element element : roundEnv.getElementsAnnotatedWith(JavonoSketch.class)) {
                if (element.getKind() == ElementKind.CLASS) {
                    TypeElement sketchClazz = (TypeElement) element;
                    validateFields(sketchClazz);
                    validateAllMethodSignatures(sketchClazz);
                    validateNoInnerClasses(sketchClazz);
                }
            }

        }

        return true;
    }


    private void validateNoInnerClasses(TypeElement sketchClass) {
        for (Element enclosed : sketchClass.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.CLASS || enclosed.getKind() == ElementKind.INTERFACE) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] Inner or nested classes are not allowed inside a sketch class.", enclosed);
            }
        }
    }


    private void validateMethodInSketchClass(Element method, Element sketchClass, String annotationName) {
        if (!method.getEnclosingElement().equals(sketchClass)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] " + annotationName + " method must be inside the @JavonoSketch class", method);
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
                boolean isAnnotated = element.getAnnotation(JavonoLoop.class) != null || element.getAnnotation(JavonoSetup.class) != null || element.getAnnotation(JavonoCustomMethod.class) != null;

                if (!isAnnotated) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] Every method inside a @JavonoSketch class must be annotated with @JavonoLoop, @JavonoSetup, or @JavonoCustomMethod.", element);
                }
            }
        }
    }

    private void validateAllMethodSignatures(TypeElement sketchClass) {
        for (Element enclosed : sketchClass.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) enclosed;

                // Skip constructors
                if (method.getSimpleName().toString().equals("<init>")) continue;

                // Validate return type
                TypeMirror returnType = method.getReturnType();
                if (!returnType.getKind().equals(TypeKind.VOID) && !isAllowedType(returnType)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Invalid return type: " + returnType + ". Allowed types are int, float, boolean, char, String, and javono.lib.*", method);
                }

                // Validate parameter types
                for (VariableElement param : method.getParameters()) {
                    if (!isAllowedType(param.asType())) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Invalid parameter type: " + param.asType() + ". Allowed types are int, float, boolean, char, String, and javono.lib.*", param);
                    }
                }
            }
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
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] You aren't allowed to use public, static, protected modifiers", field);
                }


                if (!isAllowedFieldType(fieldType)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[Javono] Field '" + field.getSimpleName() + "' has disallowed type: " + fieldType.toString() + "\n" + "Please try to use int, float, boolean, char, String, and javono.lib.*", field);
                }
            }
        }
    }

    private boolean isAllowedFieldType(TypeMirror type) {
        // Allow primitive types
        if (type.getKind().isPrimitive()) {
            return true;
        }

        // Allow types in javono.lib package
        String typeName = type.toString();
        if (typeName.startsWith("javono.lib.")) {
            return true;
        }

        // Allow java.lang.String (if you want)
        if (typeName.equals("java.lang.String")) {
            return true;
        }

        return false;
    }


    private boolean isAllowedType(TypeMirror type) {
        TypeKind kind = type.getKind();

        // Allow primitives
        if (kind == TypeKind.INT || kind == TypeKind.FLOAT || kind == TypeKind.BOOLEAN || kind == TypeKind.CHAR) {
            return true;
        }

        // Check reference types (like String, or anything in javono.lib.*)
        String typeStr = type.toString();

        if (typeStr.equals("java.lang.String")) return true;
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

    public boolean isJavonoSketchFound() {
        return javonoSketchFound;
    }

    public void setJavonoSketchFound(boolean javonoSketchFound) {
        this.javonoSketchFound = javonoSketchFound;
    }
}
