package javono.builder.impl;

import javono.builder.JavonoBuilder;
import javono.detector.PathDetector;
import javono.detector.ToolPaths;
import javono.flasher.Esp32Flasher;
import javono.probuilder.BatchBuilder;
import javono.probuilder.ProjectCreator;

import java.io.File;
import java.io.IOException;

public class LocalBuilder implements JavonoBuilder {

    private File projectDir;

    @Override
    public void buildProject() {
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
    }

    @Override
    public void flashFirmware() {
        Esp32Flasher esp32Flasher = new Esp32Flasher();
        try {
            esp32Flasher.flashProject(projectDir);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void clean() {

    }

    @Override
    public void setOption(String key, String value) {

    }
}
