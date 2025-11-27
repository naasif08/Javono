# Javono

**Javono — Java on Native Embedded Operation** 

Javono (currently under development) is a lightweight toolchain that lets you write embedded firmware for microcontrollers using familiar Java syntax. It follows the intuitive setup() and loop() structure, inspired by the Arduino programming model.

Javono converts a restricted Java class into efficient C code suitable for bare-metal environments, without relying on a JVM. The current target is the ESP32 (ESP-WROOM-32).

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
public class SerialExample {

    @JavonoEmbeddedInit
    private void setUp() {
    }


    @JavonoEmbeddedLoop
    private void loop() {
    }
}
```

```java
import javono.builder.impl.JavonoLocalBuilder;

public class Main {

    public static void main(String arg[]) {
    
        new JavonoLocalBuilder()
                .build()
                .flash();
    }
}
```

## License

## 📄 License

Javono is licensed under the **Apache License 2.0**.

This means you are free to use, modify, distribute, and integrate the code in both open-source and commercial projects, as long as you include the required notices and do not hold the authors liable.

For full details, see the [LICENSE](./LICENSE) file in this repository.

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](./LICENSE)



---

Created by Abdullah Omar Nasif
