package javono.remote;


import javono.logger.LoggerFacade;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class GitHubWorkflowRunner {

    private final String repoUrl;
    private final String accessToken;
    private final String owner;
    private final String repoName;

    public GitHubWorkflowRunner(String repoUrl, String accessToken) {
        this.repoUrl = repoUrl;
        this.accessToken = accessToken;
        String[] parts = extractRepoOwnerAndName(repoUrl);
        this.owner = parts[0];
        this.repoName = parts[1];
    }

    /**
     * Waits and downloads the latest firmware artifact (`firmware.bin`) from the GitHub Actions workflow.
     *
     * @param firmwareDir Where to store the firmware.bin file (e.g., .Javono/firmware)
     * @return Path to the downloaded firmware.bin
     * @throws IOException          if network or download fails
     * @throws InterruptedException if polling is interrupted
     */
    public Path waitForFirmwareArtifact(Path firmwareDir) throws IOException, InterruptedException {
        final int MAX_ATTEMPTS = 20;
        final int POLL_INTERVAL_MS = 10_000; // 10 seconds
        LoggerFacade.getInstance().info("Waiting for GitHub Actions to publish firmware artifact...");

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                Path binFile = downloadFirmwareBin(firmwareDir);
                LoggerFacade.getInstance().info("✅ Firmware artifact is ready!");
                return binFile;
            } catch (FileNotFoundException e) {
                System.out.printf("【Javono】Attempt %d/%d: Not ready yet, retrying in %d sec...\n",
                        attempt, MAX_ATTEMPTS, POLL_INTERVAL_MS / 1000);
                Thread.sleep(POLL_INTERVAL_MS);
            }
        }

        throw new FileNotFoundException("❌ Timeout: Firmware artifact not found after waiting.");
    }


    public void uploadProjectToRepo(Path projectDir) throws IOException {
        // Optional: Zip the project and push via Git CLI
        // Currently placeholder — you can implement `git push` if the repo is already cloned
        LoggerFacade.getInstance().info("🚀 [TODO] Push project to GitHub: " + repoUrl);
    }

    public void triggerBuild() {
        // Assuming a GitHub Actions workflow is already set to trigger on `push`
        LoggerFacade.getInstance().info("🔁 Waiting for GitHub Actions to finish build...");
    }

    public Path downloadFirmwareBin(Path outputDir) throws IOException {
        String api = String.format(
                "https://api.github.com/repos/%s/%s/actions/artifacts",
                owner, repoName
        );

        HttpURLConnection conn = (HttpURLConnection) new URL(api).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Accept", "application/vnd.github+json");

        if (conn.getResponseCode() != 200) {
            throw new IOException("❌ Failed to fetch artifacts: " + conn.getResponseMessage());
        }

        // TODO: Parse response JSON for correct artifact ID
        // Example placeholder logic:
        int artifactId = extractLatestArtifactId(conn.getInputStream());

        // Download artifact ZIP
        String downloadUrl = String.format(
                "https://api.github.com/repos/%s/%s/actions/artifacts/%d/zip",
                owner, repoName, artifactId
        );

        HttpURLConnection downloadConn = (HttpURLConnection) new URL(downloadUrl).openConnection();
        downloadConn.setRequestMethod("GET");
        downloadConn.setRequestProperty("Authorization", "Bearer " + accessToken);
        downloadConn.setRequestProperty("Accept", "application/vnd.github+json");

        if (downloadConn.getResponseCode() != 200) {
            throw new IOException("❌ Artifact download failed: " + downloadConn.getResponseMessage());
        }

        Files.createDirectories(outputDir);
        Path zipPath = outputDir.resolve("firmware_artifact.zip");

        try (InputStream in = downloadConn.getInputStream()) {
            Files.copy(in, zipPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Extract bin file
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".bin")) {
                    Path binPath = outputDir.resolve("firmware.bin");
                    Files.copy(zis, binPath, StandardCopyOption.REPLACE_EXISTING);
                    return binPath;
                }
            }
        }

        throw new FileNotFoundException("❌ No .bin file found in artifact ZIP.");
    }

    private int extractLatestArtifactId(InputStream jsonStream) throws IOException {
        // You can use Gson or Jackson for real JSON parsing.
        // Here’s just a placeholder:
        LoggerFacade.getInstance().info("📝 [TODO] Parse artifact JSON response...");
        return 123456; // Replace with real artifact ID
    }

    private String[] extractRepoOwnerAndName(String url) {
        // Example: https://github.com/abdullah-nasif/esp32-bin-upload
        String[] parts = url.replace("https://github.com/", "").split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid GitHub repo URL: " + url);
        }
        return parts;
    }
}

