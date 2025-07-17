package javono.builder;

public interface JavonoBuilder {

    void buildProject();

    void flashFirmware();

    void clean();

    void setOption(String key, String value);
}
