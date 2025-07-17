package javono.template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * GitHubWorkflowBuilder generates a GitHub Actions workflow file for building
 * ESP32 firmware using ESP-IDF and uploading the final .bin to a Spring Boot backend.
 */
public class GitHubWorkflowBuilder {

    private final String backendUploadUrl;
    private final String deviceId;
    private final String authToken;

    public GitHubWorkflowBuilder(String backendUploadUrl, String deviceId, String authToken) {
        this.backendUploadUrl = backendUploadUrl;
        this.deviceId = deviceId;
        this.authToken = authToken;
    }

    /**
     * Generates the GitHub Actions YAML content as a String.
     *
     * @return workflow YAML
     */
    public String generateWorkflowYml() {
        return """
        name: Build and Upload ESP32 Firmware

        on:
          push:
            branches: [ main ]

        jobs:
          build:
            runs-on: ubuntu-latest

            steps:
            - name: Checkout repository
              uses: actions/checkout@v3

            - name: Set up Python
              uses: actions/setup-python@v4
              with:
                python-version: '3.11'

            - name: Install ESP-IDF tools
              run: |
                sudo apt update
                sudo apt install -y git wget flex bison gperf python3 python3-pip python3-setuptools cmake ninja-build ccache libffi-dev libssl-dev dfu-util libusb-1.0-0
                git clone --recursive https://github.com/espressif/esp-idf.git
                cd esp-idf
                ./install.sh
                echo "source $GITHUB_WORKSPACE/esp-idf/export.sh" >> $GITHUB_ENV

            - name: Build Firmware
              run: |
                source esp-idf/export.sh
                idf.py build

            - name: Upload Firmware to Backend
              run: |
                curl -X POST %s \\
                  -F "deviceId=%s" \\
                  -F "authToken=%s" \\
                  -F "firmware=@build/your_firmware.bin" \\
                  --fail
        """.formatted(
                backendUploadUrl,
                deviceId,
                authToken
        );
    }

    /**
     * Writes the workflow YAML to .github/workflows/build.yml
     *
     * @param projectRoot root directory of the user's firmware repo
     * @throws IOException if write fails
     */
    public void writeTo(Path projectRoot) throws IOException {
        Path workflowDir = projectRoot.resolve(".github/workflows");
        Files.createDirectories(workflowDir);
        Path workflowFile = workflowDir.resolve("build.yml");

        Files.writeString(workflowFile, generateWorkflowYml(), StandardCharsets.UTF_8);
    }
}
