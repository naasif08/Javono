package javono.utils;

import javono.detector.OS;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

class FileUtils {

    public static Path getJavonoRootFolder() {
        String home = System.getProperty("user.home");
        OS os = OS.detect();

        return switch (os) {
            case WINDOWS -> Paths.get("C:", "Javono");
            case LINUX, MACOS -> Paths.get(home, "Javono");
            default -> throw new UnsupportedOperationException("Unsupported OS for Javono root");
        };
    }

    public static void ensureDirectory(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create directory: " + dir, e);
        }
    }

    public void deleteDirectory(Path path) {
        if (!Files.exists(path)) return;

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete directory: " + path, e);
        }
    }


    // You can add other file utility methods here too...
}
