package org.example;

import java.io.File;

public class DeleteDownloadedFile {
    public static void deleteDownloadedFile(String filePath) {
        File file = new File(filePath);
        if (file.delete()) {
            System.out.println("File deleted successfully: " + filePath);
        } else {
            System.out.println("Failed to delete the file: " + filePath);
        }
    }
}
