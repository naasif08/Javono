package javono.builder.impl;

import javono.builder.JavonoBuilder;
import javono.detector.PathDetector;
import javono.detector.ToolPaths;
import javono.flasher.Esp32Flasher;
import javono.probuilder.BatchBuilder;
import javono.probuilder.ProjectCreator;
import javono.validator.SketchValidator;

import java.io.File;
import java.io.IOException;

public class JavonoLocalBuilder implements JavonoBuilder {

    private File projectDir;

    @Override
    public JavonoBuilder build() {

        SketchValidator.getInstance().validateProject();

        if (!ToolPaths.isInitialized()) {
            ToolPaths.init();
        }
        try {
            this.projectDir = ProjectCreator.createProject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        BatchBuilder batchBuilder = new BatchBuilder();
        try {
            batchBuilder.writeBuildScripts(this.projectDir, PathDetector.detectEsp32Port());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public JavonoBuilder flash() {
        Esp32Flasher esp32Flasher = new Esp32Flasher();
        try {
            esp32Flasher.flashProject(projectDir);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public JavonoBuilder clean() {
        return this;
    }

    @Override
    public JavonoBuilder setOption(String key, String value) {
        return this;
    }
}
