package javono.main;

import javono.annotations.*;
import javono.annotations.JavonoEmbeddedLoop;
import javono.builder.JavonoBuilder;
import javono.builder.impl.RemoteBuilder;
import javono.lib.GPIO;

import java.lang.annotation.Annotation;


@JavonoEmbeddedSketch
public class Blinky {

    private int number = 10;
    private float numAnInt;
    private boolean numAnBoolean;
    private char numAnChar;
    private String string;
    private GPIO gpio;

    @JavonoEmbeddedInit
    private void setUp() {
    }


    @JavonoEmbeddedLoop
    private void loop() {
    }

    @JavonoEmbeddedUserMethod
    private GPIO myCustomMethod2() {

        return new GPIO();
    }


    @JavonoEmbeddedUserMethod
    private int myCustomMethod() {
        return 0;
    }

}
