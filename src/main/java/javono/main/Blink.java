package javono.main;


import javono.annotations.JavonoEmbeddedInit;
import javono.annotations.JavonoEmbeddedLoop;
import javono.annotations.JavonoEmbeddedSketch;
import javono.annotations.JavonoEmbeddedUserMethod;
import javono.lib.*;

@JavonoEmbeddedSketch
public class Blink {

    private GPIO gpio;

    @JavonoEmbeddedInit
    private void setup(){


    }

    @JavonoEmbeddedLoop
    private void loop() {
        GPIO.Test2();
    }

    @JavonoEmbeddedUserMethod
    private void test(){}


}
