package javono.main;

import javono.annotations.*;
import javono.annotations.JavonoLoop;
import javono.lib.GPIO;


@JavonoSketch
public class Blinky {

    private int number = 10;
    private float numAnInt;
    private boolean numAnBoolean;
    private char numAnChar;
    private String string;
    private GPIO gpio;

    @JavonoSetup
    private void setUp() {
    }


    @JavonoLoop
    private void loop() {

    }

    @JavonoCustomMethod
    private GPIO myCustomMethod2() {
        return new GPIO();
    }


    @JavonoCustomMethod
    private int myCustomMethod() {
        return 0;
    }
}
