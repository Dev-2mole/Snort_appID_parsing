package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.chrome.ChromeDriver;
import java.io.File;
import java.util.Map;

public class Main {

    private static String MD5_HASH_file = "c726cf937d84c651a20f2ac7c528384e";
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

            // 1. UpdateFileChecker.java 호출 : Snort 홈페이지의 OpenAppId의 신규 파일이 올라왔는지 MD5비교를 통해 확인
            UpdateFileChecker updateFileChecker = new UpdateFileChecker(driver);
            int[] check_hash = new int[1]; // check_hash 배열 초기화
            updateFileChecker.validateMD5Hash(MD5_HASH_file, check_hash);
            // 신규 파일이 업데이트 되었을 경우 다운로드, 파싱작업, MD5값 확인
            if (check_hash[0] == 1) {
                System.out.println("신규 파일이 업데이트 되었습니다. 다운로드 및 데이터 확인을 진행합니다.");
                // 2. DownloadLinkFinder.java 호출 : Snort 홈페이지에서 OpenAppId 다운로드 링크 가져오기
                DownloadLinkFinder downloadLinkFinder = new DownloadLinkFinder(driver);
                String downloadLink = downloadLinkFinder.findDownloadLink();

                // 3. FileDownloader.java 호출 : 파일 다운로드 진행
                FileDownloader fileDownloader = new FileDownloader(downloadLink, localDownloadPath);
                fileDownloader.downloadFile();

                // 4. FileExtractor.java 호출 : 다운로드한 파일의 압축 해제
                FileExtractor fileExtractor = new FileExtractor(localDownloadPath, destinationDir);
                luaFiles = fileExtractor.extractFile();

                // 5. FileProcessor 호출 : 각각의 MD5를 저장
                FileProcessor fileProcessor = new FileProcessor(luaFiles, destinationDir);
                fileProcessor.searchDirectoryAndComputeMD5();
    
                // 6. UrlParser 호출
                UrlParser urlParser = new UrlParser();
                urls = urlParser.parseFileContents(luaFiles);
                UrlParser.saveUrlAsExcel(urls, destinationDir);

                // 7. MD5 해시 비교를 위한 파일 경로 설정
                String previousDay = "20231121";  // 예시 경로

                // 7. MD5 해시 비교 수행
                // 만약 인자로 주어지는 값이 없으면 그냥 null과 비교함 -> Try catch로 예외처리 해야할듯
                // 신규 URL 찾는 로직을 이 메소드에 넣고있는데 죽을거같다 살려줘
                MD5Comparator.compareMD5HashesWithPreviousDay(previousDay);

                // 8. 다운로드한 tar.gz파일 삭제
                DeleteDownloadedFile.deleteDownloadedFile("C:\\Temp\\snort-openappid.tar.gz");
            }
            else{
                System.out.println("최신버전의 파일입니다. 기존파일 검증을 수행합니다.");
            }
            // X.(아무 인자도 받지 않았을 때) 가장 최신의 파일을 찾아서, 해당 파일을 열고, 전체 검증
            // Y. MD5 비교를 통해 신규 파일이 있을 경우, 해당 신규 URL만 검증 진행
            // Z. (인자로 폴더명 숫자를 받음 ) 해당 파일을 열어서 성공 부분 or 실패부분을 검증하는 로직 필요



        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}
