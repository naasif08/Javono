package javono.main;

import javono.annotations.JavonoCustomMethod;
import javono.annotations.JavonoSetup;
import javono.annotations.JavonoSketch;
import javono.annotations.JavonoLoop;
import javono.builder.JavonoBuilder;

@JavonoSketch
public class Blinky {


    @JavonoSetup
    private void setUp() {

    }


    @JavonoLoop
    private void loop() {

    }

    @JavonoCustomMethod
    private int myCustomMethod2() {
        return 0;
    }


    @JavonoCustomMethod
     private char myCustomMethod() {
        return 'a';
    }

}
