package javono.probuilder;

import javono.validator.ValidatorFacade;

import java.io.File;
import java.io.IOException;

public class ProjectBuilderFacade {

    private static final ProjectBuilderFacade INSTANCE = new ProjectBuilderFacade();
    private static final BatchBuilder batchBuilder = new BatchBuilder();
    private static final ProjectCreator projectCreator = new ProjectCreator();


    private ProjectBuilderFacade() {
    }


    public static ProjectBuilderFacade getInstance() {
        return INSTANCE;
    }

    public void writeBuildScripts(File projectDir, String comPort) throws IOException {
        batchBuilder.writeBuildScripts(projectDir, comPort);
    }

    public File createProject() throws IOException {
        return projectCreator.createProject();
    }

}
