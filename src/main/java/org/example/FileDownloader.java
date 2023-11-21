package org.example;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.SocketException;

public class FileDownloader {

    private String downloadLink;
    private String localDownloadPath;

    public FileDownloader(String downloadLink, String localDownloadPath) {
        this.downloadLink = downloadLink;
        this.localDownloadPath = localDownloadPath;
    }

    public boolean downloadFile() throws IOException, SocketException {
        boolean result = false;

        URL url = new URL(downloadLink);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        // Connection settings (options)
        httpURLConnection.setRequestMethod("GET");
        int responseCode = httpURLConnection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // File download
            try (BufferedInputStream in = new BufferedInputStream(httpURLConnection.getInputStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(localDownloadPath)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
            }
            System.out.println("파일 다운로드 완료: " + localDownloadPath);
            result = true;
        } else {
            System.out.println("HTTP 오류: " + responseCode);
        }
        httpURLConnection.disconnect();
        
        return result;
    }
}
