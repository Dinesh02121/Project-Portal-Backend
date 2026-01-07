package projectPortal.com.Entity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil {

    public static void unzip(String zipFilePath, String destDir) throws IOException {

        File dir = new File(destDir);
        if (!dir.exists()) dir.mkdirs();

        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry;

        while ((entry = zis.getNextEntry()) != null) {
            File newFile = new File(destDir, entry.getName());

            if (entry.isDirectory()) {
                newFile.mkdirs();
            } else {
                new File(newFile.getParent()).mkdirs();
                Files.copy(zis, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        zis.close();
    }
}
