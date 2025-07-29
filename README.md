# Javono

**Javono — Java on Native Embedded Operation** 

Javono (currently under development) is a lightweight toolchain that lets you write embedded firmware for microcontrollers using familiar Java syntax. It follows the intuitive setup() and loop() structure, inspired by the Arduino programming model.

Javono converts a restricted subset of Java into efficient C code suitable for bare-metal environments, without relying on a JVM. The current target is the ESP32 (ESP-WROOM-32).

## Features

- Write Java code with `setup()` and `loop()` style methods  
- Support for multiple embedded platforms  
- Simple and extensible framework  
- Efficient Java-to-C integration under the hood

## How It Works

Javono lets you write embedded programs in simplified Java using two familiar methods: `setup()` and `loop()` — just like Arduino. Your Java code is then converted into equivalent C code, which can be compiled and flashed to a microcontroller (e.g., ESP32) using the ESP-IDF toolchain.

### Example

```java
import javono.lib.GPIO;
import javono.lib.Javono;

@JavonoEmbeddedSketch
public class BlinkExample {

    @JavonoEmbeddedInit
    private void setUp() {
    }


    @JavonoEmbeddedLoop
    private void loop() {
    }
}
```

```java
import javono.builder.impl.LocalBuilder;

public class JavonoMain {

    public static void main(String arg[]) {
        new LocalBuilder()
                .build()
                .flash();
    }
}
```

## License

This project is licensed under the MIT License.  
See [LICENSE](LICENSE) for full license details.  
See the [NOTICE](NOTICE) file for attribution and licensing guidelines.

---

Created by Abdullah Omar Nasif
