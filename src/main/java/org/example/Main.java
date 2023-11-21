package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.chrome.ChromeDriver;
import java.io.File;
import java.util.Map;

public class Main {

    private static String MD5_HASH_file = "c726cf937d84c651a20f2ac7c528384e";
    private static int check_hash = 0;
    private static ChromeDriver driver = null;      
    private static String localDownloadPath = "C:\\Temp\\snort-openappid.tar.gz";   
    private static String destinationDir = "C:\\Temp";                  
    private static File[] luaFiles = null;
    private static Map<String, String> urls = null;

    public static void main(String[] args) {
        try {
            // WebDriver setup
            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver();

            // 1. UpdateFileChecker.java 호출
            UpdateFileChecker updateFileChecker = new UpdateFileChecker(driver);
            int resultUpdate = updateFileChecker.validateMD5Hash(MD5_HASH_file, new int[]{check_hash});
            System.out.println("Result of UpdateFileChecker: " + resultUpdate);

            // 2. DownloadLinkFinder.java 호출
            DownloadLinkFinder downloadLinkFinder = new DownloadLinkFinder(driver);
            String downloadLink = downloadLinkFinder.findDownloadLink();

            // 3. FileDownloader.java 호출
            FileDownloader fileDownloader = new FileDownloader(downloadLink, check_hash, localDownloadPath);
            fileDownloader.downloadFile();

            // 4. FileExtractor.java 호출
            FileExtractor fileExtractor = new FileExtractor(check_hash, localDownloadPath, destinationDir);
            luaFiles = fileExtractor.extractFile();

            // 5. FileProcessor 호출
            FileProcessor fileProcessor = new FileProcessor(luaFiles);
            fileProcessor.searchDirectoryAndComputeMD5();

            // 6. UrlParser 호출
            UrlParser urlParser = new UrlParser();
            urls = urlParser.parseFileContents(luaFiles);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}
