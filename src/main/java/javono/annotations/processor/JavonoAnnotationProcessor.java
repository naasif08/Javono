package javono.annotations.processor;

import javono.annotations.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
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
                        "Exactly one class must be annotated with @JavonoSketch, found: " + allSketches.size());
            }
            return false;
        }

        // Accumulate sketches found this round
        Set<? extends Element> sketchesThisRound = roundEnv.getElementsAnnotatedWith(JavonoSketch.class);
        allSketches.addAll(sketchesThisRound);

        if (allSketches.size() > 1) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "More than one class annotated with @JavonoSketch found.");
            // We continue processing to show more errors, but this is a fatal situation
        }

        if (allSketches.size() == 1) {
            setJavonoSketchFound(true);
            Element sketchClass = allSketches.iterator().next();

            // Validate @JavonoSetup methods
            Set<? extends Element> setupMethods = roundEnv.getElementsAnnotatedWith(JavonoSetup.class);
            if (setupMethods.size() != 1) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Exactly one method must be annotated with @JavonoSetup, found: " + setupMethods.size());
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
                        "Exactly one method must be annotated with @JavonoLoop, found: " + loopMethods.size());
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
                            "@JavonoCustomMethod methods must be inside the @JavonoSketch class",
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
                    annotationName + " method must be inside the @JavonoSketch class",
                    method);
        }
        if (method.getKind() != ElementKind.METHOD) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    annotationName + " can only be applied to methods",
                    method);
        }
    }

    private void validateVoidMethod(Element method, String annotationName) {
        if (method.getKind() == ElementKind.METHOD) {
            ExecutableElement executable = (ExecutableElement) method;
            if (executable.getReturnType().getKind() != TypeKind.VOID) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        annotationName + " method must have void return type",
                        method);
            }
        }
    }

    // Getters and setters

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
