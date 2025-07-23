package javono.main;

import javono.annotations.JavonoCustomMethod;
import javono.annotations.JavonoSetup;
import javono.annotations.JavonoSketch;
import javono.annotations.JavonoLoop;

@JavonoSketch
public class Blinky {


    @JavonoSetup
    void myMethod() {

        myCustomMethod();
    }

    @JavonoLoop
    void myMethodLoop() {

    }

    @JavonoCustomMethod
    private void myCustomMethod2() {
    }

    @JavonoCustomMethod
    private void myCustomMethod() {
    }

}
