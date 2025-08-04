package javono.utils;

import java.io.IOException;
import java.nio.file.Path;

public class UtilsFacade {

    private static final UtilsFacade INSTANCE = new UtilsFacade();
    private static final ZipExtractor zipExtractor = new ZipExtractor();
    private static final TerminalLauncher terminalLauncher = new TerminalLauncher();
    private static final FileUtils fileUtils = new FileUtils();
    private static final FileDownloader fileDownloader = new FileDownloader();
    private static final CommandRunner commandRunner = new CommandRunner();
    private static final PythonEnvChecker pythonEnvChecker = new PythonEnvChecker();


    private UtilsFacade() {
    }


    public static UtilsFacade getInstance() {
        return INSTANCE;
    }

    public void extractZip(Path zipFilePath, Path outputFolder) throws IOException {
        zipExtractor.extract(zipFilePath, outputFolder);
    }

    public void openSudoCommandInTerminal(String command) throws IOException, InterruptedException {
        terminalLauncher.openSudoCommandInTerminal(command);
    }

    public void deleteDirectory(Path path) {
        fileUtils.deleteDirectory(path);
    }

    public void downloadWithResume(String urlString, Path destination) throws IOException, InterruptedException {
        fileDownloader.downloadWithResume(urlString, destination);
    }

    public int runBlocking(String[] command) throws IOException {
        return commandRunner.runBlocking(command);
    }

    public void warnAndInstallIfMissing() {
        pythonEnvChecker.warnAndInstallIfMissing();
    }
}
