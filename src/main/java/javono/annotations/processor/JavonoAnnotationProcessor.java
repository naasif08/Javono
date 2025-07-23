package javono.annotations.processor;

import javono.annotations.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@SupportedAnnotationTypes({
        "javono.annotations.JavonoSketch",
        "javono.annotations.JavonoSetup",
        "javono.annotations.JavonoLoop",
        "javono.annotations.JavonoCustomMethod"
})
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
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "[Javono] Exactly one class must be annotated with @JavonoSketch, found: " + allSketches.size());
            }
            return false;
        }

        // Accumulate sketches found this round
        Set<? extends Element> sketchesThisRound = roundEnv.getElementsAnnotatedWith(JavonoSketch.class);
        allSketches.addAll(sketchesThisRound);

        if (allSketches.size() > 1) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "[Javono] More than one class annotated with @JavonoSketch found.");
            // We continue processing to show more errors, but this is a fatal situation
        }

        if (allSketches.size() == 1) {
            setJavonoSketchFound(true);
            Element sketchClass = allSketches.iterator().next();
            if (!sketchClass.getModifiers().contains(Modifier.PUBLIC)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "[Javono] @JavonoSketch class must be public.");
            }
            if (sketchClass.getModifiers().contains(Modifier.ABSTRACT)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "[Javono] @JavonoSketch class must not be abstract.");
            }

            if (sketchClass instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) sketchClass;

                validateUnannotatedMethods(typeElement);

                // Check for extends (other than Object)
                String superClassName = typeElement.getSuperclass().toString();
                if (!superClassName.equals("java.lang.Object")) {
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "[Javono] Classes annotated with @JavonoSketch cannot extend other classes. Please keep it standalone.",
                            sketchClass);
                }

                // Check for implements
                if (!typeElement.getInterfaces().isEmpty()) {
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "[Javono] Classes annotated with @JavonoSketch cannot implement interfaces. Keep it simple and pure!",
                            sketchClass);
                }
            }


            // Validate @JavonoSetup methods
            Set<? extends Element> setupMethods = roundEnv.getElementsAnnotatedWith(JavonoSetup.class);
            if (setupMethods.size() != 1) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "[Javono] Exactly one method must be annotated with @JavonoSetup, found: " + setupMethods.size());
            }
            for (Element setup : setupMethods) {
                validateMethodInSketchClass(setup, sketchClass, "@JavonoSetup");
                validateVoidMethod(setup, "@JavonoSetup");
                setSetupFound(true);
            }

            // Validate @JavonoLoop methods
            Set<? extends Element> loopMethods = roundEnv.getElementsAnnotatedWith(JavonoLoop.class);
            if (loopMethods.size() != 1) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "[Javono] Exactly one method must be annotated with @JavonoLoop, found: " + loopMethods.size());
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
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "[Javono] @JavonoCustomMethod methods must be inside the @JavonoSketch class",
                            custom);
                }
            }

            for (Element custom : customMethods) {
                if (!custom.getEnclosingElement().equals(sketchClass)) {
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "[Javono] @JavonoCustomMethod methods must be inside the @JavonoSketch class",
                            custom);
                }

                // Check that the method is private
                Set<Modifier> modifiers = custom.getModifiers();
                if (!modifiers.contains(Modifier.PRIVATE)) {
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "[Javono] @JavonoCustomMethod methods must be private",
                            custom);
                }

                // Check that the method is NOT static
                if (modifiers.contains(Modifier.STATIC)) {
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "[Javono] @JavonoCustomMethod methods must NOT be static",
                            custom);
                }
            }


        }

        return true;
    }

    private void validateMethodInSketchClass(Element method, Element sketchClass, String annotationName) {
        if (!method.getEnclosingElement().equals(sketchClass)) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "[Javono] " + annotationName + " method must be inside the @JavonoSketch class",
                    method);
        }
        if (method.getKind() != ElementKind.METHOD) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "[Javono] " + annotationName + " can only be applied to methods",
                    method);
        }
    }

    private void validateVoidMethod(Element method, String annotationName) {
        if (method.getKind() == ElementKind.METHOD) {
            ExecutableElement executable = (ExecutableElement) method;

            // Check return type
            if (executable.getReturnType().getKind() != TypeKind.VOID) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "[Javono] " + annotationName + " method must have a void return type.",
                        method);
            }

            // Check if method is private
            if (!method.getModifiers().contains(Modifier.PRIVATE)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "[Javono] " + annotationName + " method must be private. Keep it secret, keep it safe.",
                        method);
            }

            if (!executable.getParameters().isEmpty()) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "[Javono] " + annotationName + " method must not take any parameters.",
                        method);
            }
            long count = method.getAnnotationMirrors().stream()
                    .filter(mirror -> mirror.getAnnotationType().toString().startsWith("javono.annotations"))
                    .count();
            if (count > 1) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "[Javono] A method must not have multiple Javono annotations.",
                        method);
            }

            if (method.getModifiers().contains(Modifier.STATIC)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "[Javono] @" + annotationName + " methods must not be static.",
                        method);
            }


        }
    }


    // Getters and setters
    private void validateUnannotatedMethods(TypeElement sketchClass) {
        List<? extends Element> enclosedElements = sketchClass.getEnclosedElements();

        for (Element element : enclosedElements) {
            if (element.getKind() == ElementKind.METHOD) {
                boolean isAnnotated = element.getAnnotation(JavonoLoop.class) != null
                        || element.getAnnotation(JavonoSetup.class) != null
                        || element.getAnnotation(JavonoCustomMethod.class) != null;

                if (!isAnnotated) {
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "[Javono] Every method inside a @JavonoSketch class must be annotated with @JavonoLoop, @JavonoSetup, or @JavonoCustomMethod.",
                            element
                    );
                }
            }
        }
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
