package javono.builder.impl;

import javono.detector.DetectorFacade;
import javono.builder.JavonoBuilder;

import javono.logger.LoggerFacade;
import javono.probuilder.ProjectBuilderFacade;
import javono.remote.RemoteFacade;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RemoteBuilder implements JavonoBuilder {


    private String githubRepoUrl;
    private String githubAccessToken;
    private File projectDir;

    public RemoteBuilder() {

    }

    public RemoteBuilder(String githubRepoUrl, String githubAccessToken) {
        this.githubRepoUrl = githubRepoUrl;
        this.githubAccessToken = githubAccessToken;
    }


    @Override
    public JavonoBuilder build() {
        if (!DetectorFacade.getInstance().isToolPathsInitialized()) {
            DetectorFacade.getInstance().initializeToolPaths();
        }
        try {
            this.projectDir = ProjectBuilderFacade.getInstance().createProject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            ProjectBuilderFacade.getInstance().writeBuildScripts(projectDir, DetectorFacade.getInstance().detectEsp32Port());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public JavonoBuilder flash() {
        try {
            Path firmwareDir = DetectorFacade.getInstance().getDotJavonoDir().toPath().resolve("firmware");
            Files.createDirectories(firmwareDir);

            LoggerFacade.getInstance().info("🚀 Uploading project to GitHub...");

            // 1. Push projectDir to a temp branch
            String commitSHA = RemoteFacade.getInstance().pushProjectToGithub(projectDir);

            // 2. Trigger the GitHub Actions build
            String artifactUrl = String.valueOf(RemoteFacade.getInstance().waitForGithubFirmwareArtifact(Path.of(commitSHA)));

            // 3. Download firmware.bin to .Javono/firmware/
            Path outputFile = firmwareDir.resolve("firmware.bin");
            RemoteFacade.getInstance().downloadGithubArtifact(artifactUrl, outputFile, githubAccessToken);

            LoggerFacade.getInstance().success("Firmware downloaded to: " + outputFile.toAbsolutePath());

        } catch (Exception e) {
            throw new RuntimeException("❌ Remote flashing failed.", e);
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
