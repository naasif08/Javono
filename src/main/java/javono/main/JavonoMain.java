package javono.main;


import javono.builder.JavonoBuilder;
import javono.builder.impl.LocalBuilder;
import javono.detector.PathDetector;
import javono.detector.ToolPaths;

import java.io.IOException;


public class JavonoMain {

    public static void main(String[] args) {

        try {
            JavonoBootstrap.runFirstTimeSetupLocal();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        JavonoBuilder builder = new LocalBuilder();
        builder.buildProject();
        builder.flashFirmware();
    }
}
