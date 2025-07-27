package javono.main;


import javono.builder.impl.LocalBuilder;

public class JavonoMain {

    public static void main(String arg[]) {
        new LocalBuilder()
                .build()
                .flash();

    }
}
