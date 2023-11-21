package org.example;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class FileExtractor {

    private String localDownloadPath;
    private String destinationDir;

    public FileExtractor( String localDownloadPath, String destinationDir) {
        this.localDownloadPath = localDownloadPath;
        this.destinationDir = destinationDir;
    }

    public File[] extractFile() throws IOException {
        List<File> extractedFiles = new ArrayList<>();

        // 오늘 날짜를 YYYYMMDD 형식으로 가져오기
        String dateFolder = new SimpleDateFormat("yyyyMMdd").format(new Date());
        File dateDir = new File(destinationDir, dateFolder);
        if (!dateDir.exists()) {
            dateDir.mkdirs();
        }
        try (FileInputStream fileInputStream = new FileInputStream(localDownloadPath);
             GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(fileInputStream);
             ArchiveInputStream tarInputStream = new TarArchiveInputStream(gzipInputStream)) {
            ArchiveEntry entry;
            while ((entry = tarInputStream.getNextEntry()) != null) {
                if (entry instanceof TarArchiveEntry) {
                    TarArchiveEntry tarEntry = (TarArchiveEntry) entry;
                    if (!tarEntry.isDirectory()) {
                        File outputFile = new File(dateDir, tarEntry.getName());
                        outputFile.getParentFile().mkdirs();
                        extractedFiles.add(outputFile);
                        try (OutputStream outputFileStream = new FileOutputStream(outputFile)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = tarInputStream.read(buffer)) != -1) {
                                outputFileStream.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                }
            }
            System.out.println("압축 해제가 완료되었습니다.");
        }

        return extractedFiles.toArray(new File[0]);
    }
}
