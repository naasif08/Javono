package javono.remote;

import javono.logger.JavonoLogger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class GitHubUploader {

    private final String repoUrl;
    private final String accessToken;

    public GitHubUploader(String repoUrl, String accessToken) {
        this.repoUrl = repoUrl;
        this.accessToken = accessToken;
    }

    public String pushProject(File projectDir) throws IOException, InterruptedException {
        // Step 1: Create a temporary directory to clone into
        Path tempDir = Files.createTempDirectory("Javono-github-upload-");
        String tempPath = tempDir.toAbsolutePath().toString();
        JavonoLogger.info("📂 Cloning repo to temp: " + tempPath);

        // Step 2: Clone the repo using token auth
        String authUrl = repoUrl.replace("https://", "https://" + accessToken + "@");
        runCommand(tempDir.getParent().toFile(), "git", "clone", authUrl, tempPath);

        // Step 3: Copy project files into repo
        File gitRepoRoot = new File(tempPath);
        copyFolder(projectDir.toPath(), gitRepoRoot.toPath());

        // Step 4: Commit and push
        String branchName = "Javono-build-" + UUID.randomUUID().toString().substring(0, 8);
        runCommand(gitRepoRoot, "git", "checkout", "-b", branchName);
        runCommand(gitRepoRoot, "git", "add", ".");
        runCommand(gitRepoRoot, "git", "commit", "-m", "Javono: Automated build push");
        runCommand(gitRepoRoot, "git", "push", "-u", "origin", branchName);

        // Step 5: Return commit SHA (optional, dummy for now)
        return branchName;
    }

    private void runCommand(File dir, String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(dir);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                JavonoLogger.info("  » " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed: " + String.join(" ", command));
        }
    }

    private void copyFolder(Path src, Path dest) throws IOException {
        Files.walk(src).forEach(source -> {
            try {
                Path target = dest.resolve(src.relativize(source));
                if (Files.isDirectory(source)) {
                    if (!Files.exists(target)) Files.createDirectory(target);
                } else {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}

