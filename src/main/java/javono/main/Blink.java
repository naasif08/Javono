package javono.main;


import javono.annotations.JavonoEmbeddedInit;
import javono.annotations.JavonoEmbeddedLoop;
import javono.annotations.JavonoEmbeddedSketch;
import javono.annotations.JavonoEmbeddedUserMethod;
import javono.lib.*;
import javono.logger.LoggerFacade;

import java.util.ArrayList;
import java.util.List;

@JavonoEmbeddedSketch
public class Blink {

    private GPIO gpio;
    private JavonoString string;


    @JavonoEmbeddedInit
    private void setup() {

        JavonoString javonoString = new JavonoString("ddkmk");

    }

    @JavonoEmbeddedLoop
    private void loop() {

    }

    @JavonoEmbeddedUserMethod
    private int mymethod() {

        return 0;
    }


}
