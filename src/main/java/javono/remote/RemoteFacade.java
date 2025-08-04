package javono.remote;


import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class RemoteFacade {
    private static String repoUrl;
    private static String accessToken;
    private static final RemoteFacade INSTANCE = new RemoteFacade();
    private static final GitHubWorkflowRunner gitHubWorkFlowRunner = new GitHubWorkflowRunner(repoUrl, accessToken);
    private static final GitHubUploader gitHubUploader = new GitHubUploader(repoUrl, accessToken);
    private static final GitHubArtifactDownloader gitHubArtifactDownloader = new GitHubArtifactDownloader();

    private RemoteFacade() {
    }


    public static RemoteFacade getInstance() {
        return INSTANCE;
    }

    public Path waitForGithubFirmwareArtifact(Path firmwareDir) throws IOException, InterruptedException {
        return gitHubWorkFlowRunner.waitForFirmwareArtifact(firmwareDir);
    }

    public String pushProjectToGithub(File projectDir) throws IOException, InterruptedException {
        return gitHubUploader.pushProject(projectDir);
    }

    public void downloadGithubArtifact(String artifactUrl, Path outputPath, String token) throws IOException {
        gitHubArtifactDownloader.downloadArtifact(artifactUrl, outputPath, token);
    }


    public static String getAccessToken() {
        return accessToken;
    }

    public static void setAccessToken(String accessToken) {
        RemoteFacade.accessToken = accessToken;
    }

    public static String getRepoUrl() {
        return repoUrl;
    }

    public static void setRepoUrl(String repoUrl) {
        RemoteFacade.repoUrl = repoUrl;
    }
}
