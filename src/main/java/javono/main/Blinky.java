package javono.main;

import javono.annotations.JavonoCustomMethod;
import javono.annotations.JavonoSetup;
import javono.annotations.JavonoSketch;
import javono.annotations.JavonoLoop;

@JavonoSketch
public class Blinky {



    @JavonoSetup
    void myMethod() {

    }

    @JavonoLoop
    void myMethodLoop() {

        myCustomMethod();
    }

    @JavonoCustomMethod
    private void myCustomMethod2() {
    }

    @JavonoCustomMethod
    private void myCustomMethod() {
    }

}
