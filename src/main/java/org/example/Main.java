package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class Main {
    private static String MD5_HASH_file = "c726cf937d84c651a20f2ac7c528384e";
    private static int check_hash = 0;
    private static ChromeDriver driver = null;      // 로컬 컴퓨터로 다운 받을 파일 경로
    private static String localDownloadPath = "C:\\Temp\\snort-openappid.tar.gz";   // 압축 해제된 파일을 저장할 디렉토리
    private static String destinationDir = "C:\\Temp";                  // 압축 해제 후 url 담은 소스파일 이름들 저장해놓을 변수
    private static File[] luaFiles = null;
    private static Map<String, String> urls = null;             // 중복 배제 위한 key에 URL value에 소스파일명

    // snort 사이트에서 파일 다운로드하는 링크 추출
    private static String getFileDownloadLinkUrl() throws InterruptedException {
        driver = new ChromeDriver();
        driver.get("https://snort.org/downloads");
        Thread.sleep(2000);

        WebElement webBodyElement = driver.findElement(By.className("background_grey"));
        List<WebElement> webElementList = webBodyElement.findElements(By.tagName("a"));
        String downloadLink = null;
        for(WebElement element : webElementList) {
            String href = element.getAttribute("href");
            if (href != null && element.getText().equals("snort-openappid.tar.gz")) {
                downloadLink = href;
                System.out.println("링크: " + href);
            }
        }
        driver.quit();
        return downloadLink;
    }


    // HASH값 검증 프로토콜
    private static int getfilemd5hash(String MD5_HASH_file, int[] check_hash) throws InterruptedException {
        driver = new ChromeDriver();

        driver.get("https://snort.org/downloads/openappid/md5s");
        Thread.sleep(2000);
        WebElement preTag = driver.findElement(By.tagName("pre"));
        String preText = preTag.getText();
        String[] lines = preText.split("\n");
        for (String line : lines) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length == 2) {
                String md5 = parts[0];
                String fileName = parts[1];
                System.out.println("MD5: " + md5 + ", File Name: " + fileName);
                if (md5.equals(MD5_HASH_file)) {
                    System.out.println("해시값이 동일합니다!");
                    check_hash[0] = 1;
                }
            }
        }
        return 0;
    }

    // 위에서 얻은 링크를 통해 파일 다운로드
    private static boolean downloadFile(String downloadLink, int check_hash) throws IOException, SocketException {
        System.out.println(check_hash);
        boolean result = false;  // result 변수를 여기서 선언
        // hash 프로토콜 만족할 경우 생략
        if (check_hash == 1){
            System.out.println("업데이트 파일 없음. 다운로드 생략");
            result = true;
        }
        else if (check_hash == 0){
            URL url = new URL(downloadLink);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

            // 연결 설정 (옵션)
            httpURLConnection.setRequestMethod("GET");
            int responseCode = httpURLConnection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 파일 다운로드
                BufferedInputStream in = new BufferedInputStream(httpURLConnection.getInputStream());
                FileOutputStream fileOutputStream = new FileOutputStream(localDownloadPath);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
                fileOutputStream.close();
                in.close();
                System.out.println("파일 다운로드 완료: " + localDownloadPath);
                result = true;
            } else {
                System.out.println("HTTP 오류: " + responseCode);
            }
            httpURLConnection.disconnect();
        }
        return result;
    }


    // 다운로드 받은 압축파일 해제
    private static boolean unTarZipFile(int check_hash) throws IOException {
        boolean result = false;  // result 변수를 여기서 선언
        // hash 프로토콜 만족할 경우 생략
        if (check_hash == 1){
            System.out.println("업데이트 파일 없음. 압축 해제 생략");
            result = true;
        }
        else if (check_hash == 0){
            // 압축 해제할 tar.gz 파일 경로
            FileInputStream fileInputStream = new FileInputStream(localDownloadPath);
            GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(fileInputStream);
            TarArchiveInputStream tarInputStream = new TarArchiveInputStream(gzipInputStream);
            TarArchiveEntry tarEntry;
            while ((tarEntry = tarInputStream.getNextTarEntry()) != null) {
                if (!tarEntry.isDirectory()) {
                    File outputFile = new File(destinationDir, tarEntry.getName());
                    outputFile.getParentFile().mkdirs();

                    try (OutputStream outputFileStream = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = tarInputStream.read(buffer)) != -1) {
                            outputFileStream.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
            tarInputStream.close();
            gzipInputStream.close();
            fileInputStream.close();
            result = true;
            System.out.println("압축 해제가 완료되었습니다.");
        }
        return result;
    }


    // 압축 해제한 폴더 안에 있는 파일 탐색
    // 압축 해제한 폴더 안에 있는 파일 탐색
    private static void searchDirectoryAndComputeMD5() {
        File luaDirectory = new File("C:\\Temp\\odp\\lua");
        if (luaDirectory.exists()) {
            luaFiles = luaDirectory.listFiles();
            System.out.println("size : " + luaFiles.length);

            // 엑셀 파일 생성
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("MD5 Hash Values");

            // 엑셀 헤더 추가
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Filename");
            headerRow.createCell(1).setCellValue("MD5 Hash");

            for (int i = 0; i < luaFiles.length; i++) {
                File file = luaFiles[i];
                System.out.println("filename : " + file.getName());

                // MD5 해시값 계산
                String md5Hash = calculateMD5(file);

                // 결과를 엑셀에 추가
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(file.getName());
                row.createCell(1).setCellValue(md5Hash);

                System.out.println("MD5 Hash: " + md5Hash);
            }

            // 엑셀 파일 저장
            try (FileOutputStream fileOut = new FileOutputStream("C:\\Temp\\MD5HashValues.xlsx")) {
                workbook.write(fileOut);
                workbook.close();
                System.out.println("MD5 Hash Values 엑셀 파일이 생성되었습니다.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("해당 이름을 가진 디렉터리가 존재하지 않습니다");
        }
    }

    // 파일의 MD5 해시 계산
    private static String calculateMD5(File file) {
        try {
            return DigestUtils.md5Hex(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    // 파일 탐색 후 각 소스파일 안의 소스코드 파싱
    private static void parsingFileContents() throws IOException, IndexOutOfBoundsException {
        for (File file : luaFiles) {
            System.out.println("filename : " + file.getName());
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String codes = "", line = "";
            int brackets = 0;
            boolean inCommentBlock = false; // 여러 줄 주석 내부에 있는지 여부
            while ((line = bufferedReader.readLine()) != null) {
                if (!inCommentBlock) {
                    // 주석 블록 내부가 아닌 경우만 처리
                    if (line.contains("--")) {
                        // 단일 라인 주석인 경우 처리
                        continue;
                    } else if (line.contains("--[[") && line.contains("]]")) {
                        // 주석 블록이 한 줄에 있는 경우 처리
                        continue;
                    } else if (line.contains("--[[") && !line.contains("]]")) {
                        // 여러 줄 주석 블록 시작
                        inCommentBlock = true;
                        continue;
                    } else if (line.contains("]]")) {
                        // 여러 줄 주석 블록 끝
                        inCommentBlock = false;
                        continue;
                    }

                    if (line.contains("gUrlPatternList")
                            || line.contains("addHttpPattern(")
                            || line.contains("addAppUrl(")
                            || line.contains("gSSLHostPatternList")) {
                        if (line.contains("{"))
                            brackets++;
                        if (line.contains("}"))
                            brackets--;
                        codes = codes.concat(line);
                        // 보통 중괄호가 하나라도 있으면 여러 줄에 걸쳐서 URL이 존재
                        if (brackets >= 1) {
                            while (brackets >= 1) {
                                line = bufferedReader.readLine();
                                if (line == null)
                                    continue;
                                if (line.contains("{"))
                                    brackets++;
                                if (line.contains("}"))
                                    brackets--;
                                codes = codes.concat(line);
                            }
                        }
                        getURL(codes, file.getName());
                        codes = "";
                    }
                }
            }
        }
        System.out.println("url 개수 : " + urls.size());
    }




    private static void getURL(String codes, String filename) {
        String tmp = new String(codes);
        // 코드 패턴이 gUrlPatternList인 경우, { 기준으로 문자열 분리 후, "로 다시 분리해서 url 추출
        if (tmp.contains("gUrlPatternList")) {
            String[] result1 = tmp.split("\\{");
            for (int i = 2; i < result1.length; i++) {
                String[] result2 = result1[i].split("\"");
                if (result2.length >= 5) {
                    urls.put(result2[5] + "//" + result2[1] + result2[3], filename);
                    System.out.println(result2[5] + "//" + result2[1] + result2[3]);
                }
            }
        } else if (tmp.contains("gSSLHostPatternList")) {
            String[] result1 = tmp.split("\\{");
            for (int i = 2; i < result1.length; i++) {
                String[] result2 = result1[i].split("'");
                urls.put("https://" + result2[1], filename);
                System.out.println("https://" + result2[1]);
            }
        }
        // 코드 패턴이 그 외인 경우, "만으로 문자열 분리해서 url 추출
        else {
            String[] lines = tmp.split("\\r?\\n");  // 라인 단위로 분리
            for (String line : lines) {
                // 단일 라인 주석인 경우 건너뛰기
                if (line.contains("--")) {
                    continue;
                }

                if (line.contains("\"")) {
                    String[] result = line.split("\"");
                    if (result.length >= 5) {
                        for (int idx = 5; idx + 4 <= result.length; idx += 8) {
                            urls.put(result[idx] + "//" + result[idx - 4] + result[idx - 2], filename);
                            System.out.println(result[idx] + "//" + result[idx - 4] + result[idx - 2]);

                            if (result[idx + 1].equals("https:"))
                                idx++;
                        }
                    }
                } else if (line.contains("'")) {
                    String[] result = line.split("'");
                    if (result.length >= 5) {
                        for (int idx = 5; idx + 4 <= result.length; idx += 8) {
                            urls.put(result[idx] + "//" + result[idx - 4] + result[idx - 2], filename);
                            if (result[idx + 1].equals("https:"))
                                idx++;
                        }
                    }
                }
            }
        }
    }


    private static void saveUrlAsExcel() throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
                destinationDir+"\\urls.csv", false));

        Iterator<String> keys = urls.keySet().iterator();
        Iterator<String> values = urls.values().iterator();

        System.out.println("url 개수 : "+urls.size());
        for(int i=0;i<urls.size();i++){
            bufferedWriter.write(keys.next()+", "+values.next());
            bufferedWriter.newLine();
        }
        bufferedWriter.flush();
        bufferedWriter.close();
    }


    public static void main(String[] args) {
        //WebDriverManager.chromedriver().setup();
        //driver = new ChromeDriver();
        //driver.get("http://office365.com");

        try {
            // 1. 파일 다운로드 링크 획득
            String downloadLink = getFileDownloadLinkUrl();

            // 2. 파일의 MD5 해시 검증
            int[] hashCheckResult = new int[1];
            getfilemd5hash(MD5_HASH_file, hashCheckResult);

            // 3. 파일 다운로드 및 압축 해제
            if (hashCheckResult[0] == 1) {
                boolean downloadResult = downloadFile(downloadLink, hashCheckResult[0]);
                if (downloadResult) {
                    boolean unzipResult = unTarZipFile(hashCheckResult[0]);
                    if (unzipResult) {
                        // 4. 압축 해제된 폴더 안에서 파일 검색
                        searchDirectoryAndComputeMD5();
                        System.out.println("MD5 추출 끝 일단 종료");

                        // 5. 파일 내용 파싱 및 URL 추출
                        urls = new HashMap<>();
                        parsingFileContents();

                        // 6. URL을 파일에 저장
                        saveUrlAsExcel();
                    }
                }
            }
        } catch (SocketException e4) {
            e4.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e2) {
            if (driver != null)
                driver.quit();
            System.out.println("인덱스 범위 벗어남");
        } catch (RuntimeException e3) {
            e3.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (driver != null)
                driver.quit();
        }
    }
}