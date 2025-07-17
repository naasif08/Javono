package javono.main;



import javono.builder.JavonoBuilder;
import javono.builder.impl.LocalBuilder;
import javono.installer.JavonoBootstrap;

import java.io.IOException;



public class JavonoMain {

    public static void main(String[] args) {
        JavonoBuilder builder = new LocalBuilder();
        builder.buildProject();
        builder.flashFirmware();
    }
}
