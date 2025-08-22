package javono.builder.impl;

import javono.builder.JavonoBuilder;
import javono.detector.DetectorFacade;
import javono.flasher.FlasherFacade;
import javono.logger.LoggerFacade;
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
        this.projectDir = detectProjectDir();
        try {
            FlasherFacade.getInstance().flashProject(this.projectDir);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public JavonoBuilder clean() {
        try {
            File dirToDelete = detectProjectDir().getParentFile();
            deleteRecursively(dirToDelete);
            LoggerFacade.getInstance().info("Deleted project directory: " + dirToDelete.getAbsolutePath());
            projectDir = null; // reset after deletion
        } catch (IllegalStateException e) {
            LoggerFacade.getInstance().info("Nothing to clean: " + e.getMessage());
        }
        return this;
    }

    private void deleteRecursively(File file) {
        if (!file.exists()) return;

        if (file.isDirectory()) {
            File[] contents = file.listFiles();
            if (contents != null) {
                for (File f : contents) {
                    deleteRecursively(f);
                }
            }
        }
        file.delete();
    }


    @Override
    public JavonoBuilder setOption(String key, String value) {
        return this;
    }

    private File detectProjectDir() {
        // Get current working directory
        File baseDir = new File(System.getProperty("user.dir"));

        // Common location of Javono project
        File javonoDir = new File(baseDir, ".javono/ESP32Project");
        if (javonoDir.exists() && javonoDir.isDirectory()) {
            return javonoDir;
        }

        // Optionally, scan subfolders if needed
        File[] matches = baseDir.listFiles((dir, name) -> name.equals(".javono"));
        if (matches != null && matches.length > 0) {
            File espDir = new File(matches[0], "ESP32Project");
            if (espDir.exists() && espDir.isDirectory()) return espDir;
        }

        throw new IllegalStateException("Cannot detect Javono ESP32 project folder. Run `javono build` first!");
    }

}
