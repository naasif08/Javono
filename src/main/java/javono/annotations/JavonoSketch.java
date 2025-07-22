package javono.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)   // <-- MUST be CLASS or RUNTIME for processors
@Target(ElementType.TYPE)
public @interface JavonoSketch {
}

