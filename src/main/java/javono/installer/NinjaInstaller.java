package javono.installer;
import javono.detector.PathDetector;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class NinjaInstaller {

    private static final String NINJA_URL = "https://github.com/ninja-build/ninja/releases/latest/download/ninja-linux.zip";
    private static final String NINJA_DIR = "tools/ninja";
    private static final String NINJA_BINARY_NAME = "ninja";

    public static Path installNinja(Path javonoRoot) throws IOException, InterruptedException {
        Path ninjaDir = javonoRoot.resolve(NINJA_DIR);
        Path ninjaBinary = ninjaDir.resolve(NINJA_BINARY_NAME);

        if (Files.exists(ninjaBinary)) {
            System.out.println("[Javono] Ninja already installed at: " + ninjaBinary);
            return ninjaBinary;
        }

        System.out.println("[Javono] Ninja not found. Installing...");

        // Create ninja dir if needed
        if (!Files.exists(ninjaDir)) {
            Files.createDirectories(ninjaDir);
        }

        // Download ninja zip
        Path zipPath = ninjaDir.resolve("ninja-linux.zip");
        downloadFile(NINJA_URL, zipPath);

        // Extract ninja binary from zip
        unzipSingleFile(zipPath, ninjaDir, NINJA_BINARY_NAME);

        // Delete zip file
        Files.deleteIfExists(zipPath);

        // Make ninja executable
        ninjaBinary.toFile().setExecutable(true);

        System.out.println("[Javono] Ninja installed successfully at: " + ninjaBinary);

        return ninjaBinary;
    }

    private static void downloadFile(String urlStr, Path outputPath) throws IOException {
        System.out.println("[Javono] Downloading Ninja from: " + urlStr);
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "Javono-Installer");
        try (InputStream in = connection.getInputStream();
             OutputStream out = Files.newOutputStream(outputPath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        System.out.println("[Javono] Download completed: " + outputPath);
    }

    private static void unzipSingleFile(Path zipPath, Path outputDir, String fileName) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            boolean found = false;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().equals(fileName)) {
                    Path outputPath = outputDir.resolve(fileName);
                    try (OutputStream out = Files.newOutputStream(outputPath)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                    }
                    found = true;
                    break;
                }
                zis.closeEntry();
            }
            if (!found) {
                throw new IOException("Ninja binary not found inside the zip.");
            }
        }
    }

    public static void main(String[] args) {
        try {
            installNinja(Path.of(PathDetector.detectIdfPath()));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    // Optional: method to add ninja path to environment or your Javono config
}

