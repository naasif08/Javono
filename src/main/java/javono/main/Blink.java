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
    private void setup() throws InterruptedException {

            Thread.sleep(100);

    }

    @JavonoEmbeddedLoop
    private void loop() {
        GPIO.Test2();
        System.out.println();
    }

    @JavonoEmbeddedUserMethod
    private void test(){}


}
