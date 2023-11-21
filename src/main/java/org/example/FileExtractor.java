package org.example;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

public class FileExtractor {

    private int checkHash;
    private String localDownloadPath;
    private String destinationDir;

    public FileExtractor(int checkHash, String localDownloadPath, String destinationDir) {
        this.checkHash = checkHash;
        this.localDownloadPath = localDownloadPath;
        this.destinationDir = destinationDir;
    }

     public File[] extractFile() throws IOException {
        List<File> extractedFiles = new ArrayList<>();

        if (checkHash == 1) {
            System.out.println("업데이트 파일 없음. 압축 해제 생략");
        } else if (checkHash == 0) {
            try (FileInputStream fileInputStream = new FileInputStream(localDownloadPath);
                 GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(fileInputStream);
                 ArchiveInputStream tarInputStream = new TarArchiveInputStream(gzipInputStream)) {

                ArchiveEntry entry;
                while ((entry = tarInputStream.getNextEntry()) != null) {
                    if (entry instanceof TarArchiveEntry) {
                        TarArchiveEntry tarEntry = (TarArchiveEntry) entry;
                        if (!tarEntry.isDirectory()) {
                            File outputFile = new File(destinationDir, tarEntry.getName());
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
        }

        return extractedFiles.toArray(new File[0]);
    }
}
