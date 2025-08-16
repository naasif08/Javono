package javono.probuilder;

import javono.detector.DetectorFacade;
import javono.detector.OS;
import javono.logger.LoggerFacade;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class ProjectCreator {

    private String PROJECT_DIR = "Null Project";
    private String setupMethod = """
            void setup(){
              Javono_serial_init();
            }
            """;
    private String loopMethod = """
            void loop(){
              const char* line = Javono_serial_read();                
              Javono_serial_write(line); 
              Javono_serial_write("hello world"); 
              Javono_serial_write("new world"); 
              sleep(0.5);
            }
            """;

    public File createProject() throws IOException {
        File projectDir = DetectorFacade.getInstance().getProjectDir("ESP32Project");
        this.PROJECT_DIR = projectDir.getAbsolutePath();
        Path mainDir = projectDir.toPath().resolve("main");
        Files.createDirectories(mainDir);
        LoggerFacade.getInstance().info("Created project directory: " + projectDir.getAbsolutePath());
        if (OS.detect().isWindows()) {
            writeFilesWindows();
        } else {
            writeFilesUnix();
        }
        return projectDir;
    }

    private void writeFilesWindows() throws IOException {
        writeTextFile(this.PROJECT_DIR + "\\CMakeLists.txt", cmakeListsTxtContent());
        writeTextFile(this.PROJECT_DIR + "\\LICENSE", licenseContent());
        writeTextFile(this.PROJECT_DIR + "\\README.md", readmeContent());
        writeTextFile(this.PROJECT_DIR + "\\.clangd", clangdContent());
        writeTextFile(this.PROJECT_DIR + "\\.clang-format", clangFormatContent());
        writeTextFile(this.PROJECT_DIR + "\\.cproject", cprojectContent());
        writeTextFile(this.PROJECT_DIR + "\\.project", projectContent());
        writeTextFile(this.PROJECT_DIR + "\\.gitignore", gitignoreContent());

        writeTextFile(this.PROJECT_DIR + "\\main\\CMakeLists.txt", mainCMakeListsTxtContent());
        writeTextFile(this.PROJECT_DIR + "\\main\\Kconfig.projbuild", kconfigProjbuildContent());
        writeTextFile(this.PROJECT_DIR + "\\main\\Javono_serial.h", serialHeaderfile());
        writeTextFile(this.PROJECT_DIR + "\\main\\Javono_serial.c", serialSourceFile());
        writeTextFile(this.PROJECT_DIR + "\\main\\main.c", mainCContent());

        LoggerFacade.getInstance().info("Created all project files.");
    }

    private void writeFilesUnix() throws IOException {
        writeTextFile(this.PROJECT_DIR + "//CMakeLists.txt", cmakeListsTxtContent());
        writeTextFile(this.PROJECT_DIR + "//LICENSE", licenseContent());
        writeTextFile(this.PROJECT_DIR + "//README.md", readmeContent());
        writeTextFile(this.PROJECT_DIR + "//.clangd", clangdContent());
        writeTextFile(this.PROJECT_DIR + "//.clang-format", clangFormatContent());
        writeTextFile(this.PROJECT_DIR + "//.cproject", cprojectContent());
        writeTextFile(this.PROJECT_DIR + "//.project", projectContent());
        writeTextFile(this.PROJECT_DIR + "//.gitignore", gitignoreContent());

        writeTextFile(this.PROJECT_DIR + "//main//CMakeLists.txt", mainCMakeListsTxtContent());
        writeTextFile(this.PROJECT_DIR + "//main//Kconfig.projbuild", kconfigProjbuildContent());
        writeTextFile(this.PROJECT_DIR + "//main//Javono_serial.h", serialHeaderfile());
        writeTextFile(this.PROJECT_DIR + "//main//Javono_serial.c", serialSourceFile());
        writeTextFile(this.PROJECT_DIR + "//main//main.c", mainCContent());

        LoggerFacade.getInstance().info("Created all project files.");
    }

    private void writeTextFile(String path, String content) throws IOException {
        Files.write(Paths.get(path), content.getBytes());
        LoggerFacade.getInstance().success("Created: " + path);
    }

    private String cmakeListsTxtContent() {
        return """
                cmake_minimum_required(VERSION 3.16)
                include($ENV{IDF_PATH}/tools/cmake/project.cmake)
                project(ESP32Project)
                """;
    }

    private String licenseContent() {
        return "/* MIT License - Example */\n";
    }

    private String readmeContent() {
        return "# ESP32Project\nMinimal ESP-IDF project created by Java\n";
    }

    private String clangdContent() {
        return "CompileFlags:\n  Add: [-I${workspaceFolder}/main]\n";
    }

    private String clangFormatContent() {
        return """
                BasedOnStyle: Google
                IndentWidth: 4
                """;
    }

    private String cprojectContent() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!-- Minimal .cproject file -->\n";
    }

    private String projectContent() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!-- Minimal .project file -->\n";
    }

    private String gitignoreContent() {
        return """
                # Ignore build output folder
                /build/
                
                # Ignore SDK config file
                /sdkconfig
                
                # Ignore Eclipse project files (if used)
                .cproject
                .project
                
                # Ignore temporary files
                *.log
                *.tmp
                
                # Ignore compiled object files and binaries
                *.o
                *.elf
                *.bin
                *.map
                
                # Ignore Python bytecode files
                __pycache__/
                *.pyc
                
                # Ignore VSCode settings
                .vscode/
                
                # Ignore any local user-specific files
                *.user
                """;
    }

    private String mainCMakeListsTxtContent() {
        return """
                idf_component_register(SRCS "main.c" Javono_serial.c
                                       INCLUDE_DIRS "")
                """;
    }

    private String kconfigProjbuildContent() {
        return "# Minimal Kconfig.projbuild\n";
    }

    private String serialHeaderfile() {
        return """
                 #ifndef Javono_SERIAL_H
                  #define Javono_SERIAL_H
                  \s
                  /**
                   * @brief Initializing UART0.
                   * @return Pointer to a static null-terminated string. Overwritten on next call.
                   */\s
                  void Javono_serial_init();\s
                
                  /**
                   * @brief Reads a line from UART0.
                   * @return Pointer to a static null-terminated string. Overwritten on next call.
                   */
                  const char *Javono_serial_read();
                
                  /**
                   * @brief Sends a null-terminated string to UART0.
                   */
                  void Javono_serial_write(const char *message);
                
                  #endif // Javono_SERIAL_H
                
                """;
    }

    private String serialSourceFile() {
        return """
                #include "Javono_serial.h"
                #include "driver/uart.h"
                #include "freertos/FreeRTOS.h"
                #include "freertos/task.h"
                #include <string.h>
                
                #define BUF_SIZE 512
                #define UART_NUM UART_NUM_0              
                
                static char internal_buffer[BUF_SIZE];              
                
                bool Javono_serial_read_line(char *out, size_t maxLen) {
                    int index = 0;
                    char c;
                    while (true) {
                        int len = uart_read_bytes(UART_NUM_0, (uint8_t *)&c, 1, 100 / portTICK_PERIOD_MS);
                        if (len > 0) {
                            if (c == '\\n') {
                                out[index] = '\\0';
                                return true;
                            }
                            if (c != '\\r' && index < maxLen - 1) {
                                out[index++] = c;
                            }
                        }
                    }
                }
                
                
                void Javono_serial_init() {                \s
                    static bool initialized = false;
                    if (initialized) return;
                
                    uart_config_t uart_config = {
                        .baud_rate = 115200,
                        .data_bits = UART_DATA_8_BITS,
                        .parity = UART_PARITY_DISABLE,
                        .stop_bits = UART_STOP_BITS_1,
                        .flow_ctrl = UART_HW_FLOWCTRL_DISABLE
                    };
                
                    uart_param_config(UART_NUM_0, &uart_config);
                    uart_set_pin(UART_NUM_0, UART_PIN_NO_CHANGE, UART_PIN_NO_CHANGE,
                                 UART_PIN_NO_CHANGE, UART_PIN_NO_CHANGE);
                    uart_driver_install(UART_NUM_0, BUF_SIZE * 2, 0, 0, NULL, 0);
                
                    // Block here until "flash" is received
                    char buffer[64];
                    while (true) {
                        if (Javono_serial_read_line(buffer, sizeof(buffer))) {
                            if (strcmp(buffer, "flash") == 0) {
                
                                uart_write_bytes(UART_NUM_0, "thunder\\n", strlen("thunder\\n"));
                                uart_wait_tx_done(UART_NUM_0, pdMS_TO_TICKS(50));
                                break;
                            }
                        }
                    }
                
                    initialized = true;
                }
                
                const char* Javono_serial_read() {
                    static char buffer[BUF_SIZE];
                    static int index = 0;
                
                    // Send "ready" to host to indicate ESP32 is ready for a command
                
                    uart_write_bytes(UART_NUM, "Javono_read\\n", strlen("Javono_read\\n"));
                     uart_wait_tx_done(UART_NUM, pdMS_TO_TICKS(50));
                
                    while (1) {
                        uint8_t ch;
                        int len = uart_read_bytes(UART_NUM_0, &ch, 1, portMAX_DELAY); // wait forever
                
                        if (len > 0) {
                            if (ch == '\\r') continue; // skip CR
                
                            if (ch == '\\n') {
                                buffer[index] = '\\0';
                                index = 0;
                
                                // Handle internal Javono command
                                if (strcmp(buffer, "flash") == 0) {
                                    Javono_serial_write("thunder");
                                    return NULL; // Skip this one, ask for next
                                }
                
                                return buffer;
                            }
                
                            if (index < BUF_SIZE - 1) {
                                buffer[index++] = ch;
                            }
                        }
                    }
                }
                
                
                void Javono_serial_write(const char *message) {
                    if (message && strlen(message) > 0) {
                        char buffer[512];  // Adjust size as needed
                        snprintf(buffer, sizeof(buffer), "%s\\n", message);
                
                        uart_write_bytes(UART_NUM, buffer, strlen(buffer));
                        uart_wait_tx_done(UART_NUM, pdMS_TO_TICKS(50));
                    }
                }
                
                
                """;
    }

    private String mainCContent() {
        return """
                #include "esp_log.h"
                #include "Javono_serial.h"
                #include <stdbool.h>
                #include <stdio.h>
                #include <string.h>
                #include <sys/unistd.h>
                #include <unistd.h>                              
                
                void setup(void);
                void loop(void);
                
                void app_main(void) {
                  setup();
                  while (true) {                                      
                  loop();               
                  }
                }
                """ + this.setupMethod + "\n" + """
                """ + this.loopMethod + "\n" + """
                
                """;
    }

    public String getSetupMethod() {
        return setupMethod;
    }

    public void setSetupMethod(String setupMethod) {
        this.setupMethod = setupMethod;
    }
}
