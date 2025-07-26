package javono.builder;

public interface JavonoBuilder {

    JavonoBuilder build();         // replaces buildProject()
    JavonoBuilder flash();         // replaces flashFirmware()
    JavonoBuilder clean();
    JavonoBuilder setOption(String key, String value);
}
