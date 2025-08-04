package javono.detector;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

class GitPathFinder {

    // This method returns the path to git executable either inside esp-idf portable folder (Windows),
    // or system git path by checking 'which' command on Linux/macOS
    public String findGitPath(Path espIdfRoot) {
        String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

        try {
            if (os.contains("win")) {
                // On Windows, search for git.exe inside esp-idf folder (cmd or bin folder)
                Path toolsDir = espIdfRoot.resolve("cmd");
                if (!Files.exists(toolsDir)) {
                    toolsDir = espIdfRoot.resolve("bin");
                }

                if (Files.exists(toolsDir)) {
                    try (Stream<Path> stream = Files.walk(toolsDir)) {
                        Optional<Path> gitPath = stream
                                .filter(Files::isRegularFile)
                                .filter(path -> {
                                    String fileName = path.getFileName().toString().toLowerCase();
                                    boolean isGitExecutable = fileName.equals("git.exe");
                                    return isGitExecutable;
                                })
                                .findFirst();

                        if (gitPath.isPresent()) {
                            return gitPath.get().toString();
                        }
                    }
                }

                // Fallback: try system git (just "git" assuming on PATH)
                if (isGitAvailable("git")) {
                    return "git";
                }

                return null;

            } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                // For Linux/macOS, check system git location by 'which git' command
                String gitPath = getSystemGitPath();
                if (gitPath != null && !gitPath.isEmpty()) {
                    return gitPath;
                }

                // fallback: just "git" command (assuming on PATH)
                if (isGitAvailable("git")) {
                    return "git";
                }

                return null;
            } else {
                // Unsupported OS
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isGitAvailable(String gitCommand) {
        try {
            ProcessBuilder pb = new ProcessBuilder(gitCommand, "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static String getSystemGitPath() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("which", "git");
        Process process = pb.start();

        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            process.waitFor();
            return line;
        }
    }
}

