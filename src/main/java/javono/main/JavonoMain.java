package javono.main;


import javono.bootstrap.JavonoBootstrap;
import javono.builder.impl.JavonoLocalBuilder;

public class JavonoMain {

    public static void main(String arg[]) {

        System.out.println("Running");
         new JavonoLocalBuilder().build();

    }
}
