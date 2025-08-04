package javono.flasher;


import javono.logger.LoggerFacade;

import java.io.File;
import java.io.IOException;

public class FlasherFacade {
    private static final FlasherFacade INSTANCE = new FlasherFacade();
    private static final Esp32Flasher flasher = new Esp32Flasher();

    private FlasherFacade() {
    }

    public static FlasherFacade getInstance() {
        return INSTANCE;
    }

    public void flashProject(File projectDir) throws IOException, InterruptedException {
        flasher.flashProject(projectDir);
    }

}
