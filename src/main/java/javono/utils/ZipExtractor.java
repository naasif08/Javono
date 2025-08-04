package javono.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility to extract ZIP archives to a target directory.
 */
class ZipExtractor {

    /**
     * Extracts a ZIP archive to the given output directory.
     *
     * @param zipFilePath  path to .zip archive
     * @param outputFolder path where contents will be extracted
     * @throws IOException if extraction fails
     */
    void extract(Path zipFilePath, Path outputFolder) throws IOException {
        try (InputStream fis = Files.newInputStream(zipFilePath); ZipInputStream zis = new ZipInputStream(fis)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path resolvedPath = outputFolder.resolve(entry.getName()).normalize();

                // Prevent Zip Slip vulnerability
                if (!resolvedPath.startsWith(outputFolder)) {
                    throw new IOException("Entry is outside the target dir: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    Files.createDirectories(resolvedPath.getParent());
                    Files.copy(zis, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
                }

                zis.closeEntry();
            }
        }
    }
}
