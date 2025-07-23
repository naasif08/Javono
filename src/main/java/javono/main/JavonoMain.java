package javono.main;


import javono.annotations.JavonoLoop;
import javono.annotations.JavonoSketch;
import javono.builder.JavonoBuilder;
import javono.builder.impl.LocalBuilder;
import javono.detector.PathDetector;
import javono.detector.ToolPaths;
import javono.validator.SketchValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class JavonoMain {

    public static void main(String[] args) {
        SketchValidator  sketchValidator = new SketchValidator();
        try {
            sketchValidator.validateProject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        try {
//            JavonoBootstrap.runFirstTimeSetupLocal();
//        } catch (IOException | InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//        JavonoBuilder builder = new LocalBuilder();
//        builder.buildProject();
//        builder.flashFirmware();
    }
}
