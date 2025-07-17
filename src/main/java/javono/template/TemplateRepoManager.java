package javono.template;

import javono.utils.CommandRunner;
import javono.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles cloning and updating a firmware template GitHub repository.
 */
public class TemplateRepoManager {

    private final String repoUrl;
    private final String accessToken;
    private final Path cloneTarget;

    public TemplateRepoManager(String repoUrl, String accessToken, Path cloneTarget) {
        this.repoUrl = repoUrl;
        this.accessToken = accessToken;
        this.cloneTarget = cloneTarget;
    }

    /**
     * Clones the firmware template repo to the target directory.
     *
     * @throws IOException if cloning fails
     */
    public void cloneTemplate() throws IOException {
        if (Files.exists(cloneTarget)) {
            throw new IOException("Target directory already exists: " + cloneTarget);
        }

        Files.createDirectories(cloneTarget.getParent());

        String authUrl = insertToken(repoUrl, accessToken);
        String[] command = { "git", "clone", "--depth", "1", authUrl, cloneTarget.toString() };

        int exit = CommandRunner.runBlocking(command);
        if (exit != 0) {
            throw new IOException("Failed to clone template repository (exit code " + exit + ")");
        }

        // Remove .git to make it a clean project copy
        FileUtils.deleteDirectory(cloneTarget.resolve(".git"));
    }

    /**
     * Updates the already cloned template repo (pulls latest from main).
     * Only works if .git folder exists (optional).
     *
     * @throws IOException if update fails
     */
    public void updateTemplate() throws IOException {
        if (!Files.exists(cloneTarget.resolve(".git"))) {
            throw new IOException("Cannot update — not a Git repo: " + cloneTarget);
        }

        int exit = CommandRunner.runBlocking(new String[] {
                "git", "-C", cloneTarget.toString(), "pull"
        });

        if (exit != 0) {
            throw new IOException("Failed to update template repository (exit code " + exit + ")");
        }
    }

    /**
     * Injects the access token into the GitHub HTTPS clone URL.
     */
    private String insertToken(String url, String token) {
        if (token == null || token.isBlank()) return url;
        return url.replace("https://", "https://" + token + "@");
    }
}
