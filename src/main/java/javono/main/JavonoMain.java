package javono.main;


import javono.builder.impl.JavonoLocalBuilder;

public class JavonoMain {

    public static void main(String arg[]) {
        new JavonoLocalBuilder()
                .build()
                .flash();

    }
}
