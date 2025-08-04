package javono.installer;


import java.io.IOException;
import java.nio.file.Path;

public class InstallerFacade {
    private static final InstallerFacade INSTANCE = new InstallerFacade();
    private static final CH340Installer ch340Installer = new CH340Installer();
    private static final EspIdfInstaller espIdfInstaller = new EspIdfInstaller();
    private static final EspIdfInstallerUnix espIdfInstallerUnix = new EspIdfInstallerUnix();
    private static final EspIdfScriptInstaller espIdfScriptInstaller = new EspIdfScriptInstaller();
    private static final GitInstaller gitInstaller = new GitInstaller();
    private static final PythonInstaller pythonInstaller = new PythonInstaller();


    private InstallerFacade() {
    }


    public static InstallerFacade getInstance() {
        return INSTANCE;
    }

    public void installEsp32DeviceDriver() {
        ch340Installer.installDriver();
    }

    public boolean isEspIdfInstalled() {
        return espIdfInstaller.isIdfInstalled();
    }

    public boolean isEspIdfInstalledForWindows() {
        return espIdfInstaller.isInstalledForWindows();
    }

    public boolean isEspIdfInstalledForLinux() {
        return espIdfInstaller.isInstalledForLinux();
    }

    public boolean isEspIdfInstalledForMac() {
        return espIdfInstaller.isInstalledForMac();
    }

    public Path getJavonoFolder() {
        return espIdfInstaller.getJavonoFolder();
    }

    public void downloadAndInstallEspIdf() throws IOException {
        espIdfInstaller.downloadAndInstall();
    }

    public void installEspIdfForLinux() {
        espIdfInstallerUnix.installForLinux();
    }

    public void installEspIdfForMacOS() {
        espIdfInstallerUnix.installForMacOS();
    }

    public void runInstallScript() throws IOException, InterruptedException {
        espIdfScriptInstaller.runInstallScript();
    }

    public void ensureGitInstalled() {
        gitInstaller.ensureGitInstalled();
    }

    public void ensureMinicondaInstalled() throws IOException, InterruptedException {
        pythonInstaller.ensureMinicondaInstalled();
    }
}

