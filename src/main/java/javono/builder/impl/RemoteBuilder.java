package javono.builder.impl;

import javono.detector.ToolPaths;
import javono.builder.JavonoBuilder;
import javono.detector.PathDetector;
import javono.logger.JavonoLogger;
import javono.probuilder.ProjectCreator;
import javono.probuilder.BatchBuilder;
import javono.remote.GitHubArtifactDownloader;
import javono.remote.GitHubUploader;
import javono.remote.GitHubWorkflowRunner;

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
        if (!ToolPaths.isInitialized()) {
            ToolPaths.init();
        }
        try {
            this.projectDir = ProjectCreator.createProject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        BatchBuilder JavonoBatchBuilder = new BatchBuilder();
        try {
            JavonoBatchBuilder.writeBuildScripts(projectDir, PathDetector.detectEsp32Port());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public JavonoBuilder flash() {
        try {
            Path firmwareDir = ToolPaths.getDotJavonoDir().toPath().resolve("firmware");
            Files.createDirectories(firmwareDir);

            JavonoLogger.info("🚀 Uploading project to GitHub...");

            // 1. Push projectDir to a temp branch
            GitHubUploader uploader = new GitHubUploader(githubRepoUrl, githubAccessToken);
            String commitSHA = uploader.pushProject(projectDir);

            // 2. Trigger the GitHub Actions build
            GitHubWorkflowRunner runner = new GitHubWorkflowRunner(githubRepoUrl, githubAccessToken);
            String artifactUrl = String.valueOf(runner.waitForFirmwareArtifact(Path.of(commitSHA)));

            // 3. Download firmware.bin to .Javono/firmware/
            Path outputFile = firmwareDir.resolve("firmware.bin");
            GitHubArtifactDownloader.downloadArtifact(artifactUrl, outputFile, githubAccessToken);

            JavonoLogger.success("Firmware downloaded to: " + outputFile.toAbsolutePath());

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
