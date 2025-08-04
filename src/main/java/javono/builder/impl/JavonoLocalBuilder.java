package javono.builder.impl;

import javono.builder.JavonoBuilder;
import javono.detector.DetectorFacade;
import javono.flasher.FlasherFacade;
import javono.probuilder.ProjectBuilderFacade;
import javono.validator.ValidatorFacade;

import java.io.File;
import java.io.IOException;

public class JavonoLocalBuilder implements JavonoBuilder {

    private File projectDir;

    @Override
    public JavonoBuilder build() {

        ValidatorFacade.getInstance().validateProject();

        if (!DetectorFacade.getInstance().isToolPathsInitialized()) {
            DetectorFacade.getInstance().initializeToolPaths();
        }
        try {
            this.projectDir = ProjectBuilderFacade.getInstance().createProject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            ProjectBuilderFacade.getInstance().writeBuildScripts(this.projectDir, DetectorFacade.getInstance().detectEsp32Port());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public JavonoBuilder flash() {
        try {
            FlasherFacade.getInstance().flashProject(projectDir);
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
