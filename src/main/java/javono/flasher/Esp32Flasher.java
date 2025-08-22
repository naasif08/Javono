package javono.flasher;


import javono.logger.LoggerFacade;

import java.io.*;
import java.util.Locale;

class Esp32Flasher {

    public void flashProject(File projectDir) throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        ProcessBuilder pb = getProcessBuilder(projectDir, os);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(System.out::println);
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Flashing failed with exit code: " + exitCode);
        } else {
            LoggerFacade.getInstance().success("Flashing finished successfully.");
        }
    }

    private static ProcessBuilder getProcessBuilder(File projectDir, String os) throws FileNotFoundException {
        ProcessBuilder pb;

        if (os.contains("win")) {
            File batchFile = new File(projectDir, "esp32_build_flash.bat");
            if (!batchFile.exists()) {
                throw new FileNotFoundException("Batch file not found: " + batchFile.getAbsolutePath());
            }
            pb = new ProcessBuilder("cmd.exe", "/c", batchFile.getName());
            pb.directory(projectDir);
        } else {
            File shellFile = new File(projectDir, "esp32_build_flash.sh");
            if (!shellFile.exists()) {
                throw new FileNotFoundException("Shell script not found: " + shellFile.getAbsolutePath());
            }
            pb = new ProcessBuilder("bash", shellFile.getName());
            pb.directory(projectDir);
        }

        pb.redirectErrorStream(true);
        return pb;
    }


}
