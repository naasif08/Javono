package javono.utils;

import javono.logger.JavonoLogger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public class FileDownloader {

    public static boolean debug = false;

    public static void downloadWithResume(String urlString, Path destination) throws IOException, InterruptedException {
        File finalFile = destination.toFile();
        File tempFile = new File(finalFile.getAbsolutePath() + ".part");

        int maxRetries = 20;
        int retryCount = 0;
        long expectedTotal = fetchRemoteFileSize(urlString);

        while (true) {
            long existingSize = tempFile.exists() ? tempFile.length() : 0;

            // ✅ Already downloaded
            if (existingSize == expectedTotal) {
                JavonoLogger.success("File already downloaded (" + (existingSize / (1024 * 1024)) + " MB). Finalizing...");
                if (finalFile.exists()) finalFile.delete();
                if (tempFile.renameTo(finalFile)) {
                    JavonoLogger.success("Download complete.");
                } else {
                    throw new IOException("Failed to rename .part file.");
                }
                return;
            }

            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
                if (existingSize > 0) {
                    connection.setRequestProperty("Range", "bytes=" + existingSize + "-");
                    JavonoLogger.info("Resuming download from: " + (existingSize / (1024 * 1024)) + " MB");
                }

                connection.connect();
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                    throw new IOException("Server doesn't support resume. HTTP code: " + responseCode);
                }

                try (InputStream in = connection.getInputStream(); RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {

                    raf.seek(existingSize);
                    byte[] buffer = new byte[65536]; // 64 KB
                    long downloaded = existingSize;
                    long lastPrint = System.currentTimeMillis();

                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        raf.write(buffer, 0, bytesRead);
                        downloaded += bytesRead;

                        if (System.currentTimeMillis() - lastPrint > 1000) {
                            int percent = (int) ((downloaded * 100) / expectedTotal);
                            System.out.print("\r[Javono] Downloading: [" + "▇".repeat(percent / 2) + "_".repeat(50 - (percent / 2)) + "] " + percent + "%");
                            lastPrint = System.currentTimeMillis();
                        }
                    }
                    if (expectedTotal > 0 && downloaded >= expectedTotal) {
                        System.out.print("\r[Javono] Downloading: [" + "▇".repeat(50) + "] 100%\n");
                    }

                    System.out.println(); // New line after progress bar
                }

                // ✅ Check completion
                if (tempFile.length() == expectedTotal) {
                    if (finalFile.exists()) finalFile.delete();
                    if (tempFile.renameTo(finalFile)) {
                        JavonoLogger.success("Download completed successfully.");
                    } else {
                        throw new IOException("Failed to rename temp file.");
                    }
                    return;
                } else {
                    throw new IOException("Download incomplete. Expected " + expectedTotal + ", got " + tempFile.length());
                }
            } catch (IOException e) {
                retryCount++;

                if (tempFile.exists() && tempFile.length() == expectedTotal) {
                    JavonoLogger.success("File is already fully downloaded. Finalizing...");
                    if (finalFile.exists()) finalFile.delete();
                    if (tempFile.renameTo(finalFile)) {
                        JavonoLogger.success("Renamed successfully.");
                        return;
                    }
                }

                if (retryCount >= maxRetries) {
                    throw new IOException("Maximum retries reached.");
                }

                JavonoLogger.warn("Network error or disconnected. Waiting to retry in 5 seconds...");
                Thread.sleep(5000);
                JavonoLogger.info("Retrying download from: " + (tempFile.length() / (1024 * 1024)) + " MB");
            }
        }
    }

    private static long fetchRemoteFileSize(String urlString) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("HEAD");
        conn.connect();
        long size = conn.getContentLengthLong();
        conn.disconnect();
        JavonoLogger.info("Expected total size: " + (size / (1024 * 1024)) + " MB");
        return size;
    }

    private static void log(String msg) {
        if (debug) JavonoLogger.info(msg);
    }

    public static void downloadFile(String urlString, Path destination) throws IOException {
        URL url = new URL(urlString);

        // Ensure parent directories exist
        Files.createDirectories(destination.getParent());

        // Open connection to get content length
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        int responseCode = connection.getResponseCode();

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to get file info. Server returned HTTP " + responseCode);
        }

        long expectedSize = connection.getContentLengthLong();
        if (expectedSize <= 0) {
            JavonoLogger.warn("Warning: Unable to determine expected file size.");
        } else {
            JavonoLogger.info("Expected file size: " + (expectedSize / (1024 * 1024)) + " MB");
        }
        connection.disconnect();

        // Download to temp file first
        Path tempFile = destination.resolveSibling(destination.getFileName() + ".part");

        try (InputStream in = url.openStream(); OutputStream out = Files.newOutputStream(tempFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalRead = 0;
            long lastPrintTime = System.currentTimeMillis();

            JavonoLogger.info("⬇️ Downloading: " + urlString);

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalRead += bytesRead;

                // Print progress every 500ms if expectedSize known
                if (expectedSize > 0 && System.currentTimeMillis() - lastPrintTime > 500) {
                    int percent = (int) ((totalRead * 100) / expectedSize);
                    String bar = buildProgressBar(percent);
                    System.out.print("\r" + bar + " " + percent + "%");
                    lastPrintTime = System.currentTimeMillis();
                }
            }
        }

        // Check downloaded file size
        long actualSize = Files.size(tempFile);
        if (expectedSize > 0 && actualSize != expectedSize) {
            Files.delete(tempFile);
            throw new IOException("Download failed: incomplete file. Expected " + expectedSize + " bytes, got " + actualSize + " bytes.");
        }

        // Rename temp file to final destination
        Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING);
        if (expectedSize > 0) {
            JavonoLogger.info("\r" + buildProgressBar(100) + " 100%\n");
        }
        JavonoLogger.success("Download complete: " + destination);
    }

    private static String buildProgressBar(int percent) {
        int totalBars = 50;
        int filledBars = (percent * totalBars) / 100;
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < filledBars; i++) bar.append("▇");
        for (int i = filledBars; i < totalBars; i++) bar.append("-");
        bar.append("]");
        return bar.toString();
    }
}
