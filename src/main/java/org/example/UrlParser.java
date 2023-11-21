package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.HashMap;
//import java.util.Iterator;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;


public class UrlParser {

    private Map<String, String> urls;

    public UrlParser() {
        this.urls = new HashMap<>();
    }

    public Map<String, String> getUrls() {
        return urls;
    }

    public Map<String, String> parseFileContents(File[] luaFiles) {
        for (File file : luaFiles) {
            //System.out.println("filename : " + file.getName());
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
                parseBufferedReader(bufferedReader, file.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("url 개수 : " + urls.size());
        return urls; 
    }
    

    private void parseBufferedReader(BufferedReader bufferedReader, String filename) throws IOException {
        StringBuilder codes = new StringBuilder();
        int brackets = 0;
        boolean inCommentBlock = false;

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (!inCommentBlock) {
                if (line.contains("--")) {
                    continue;
                } else if (line.contains("--[[") && line.contains("]]")) {
                    continue;
                } else if (line.contains("--[[") && !line.contains("]]")) {
                    inCommentBlock = true;
                    continue;
                } else if (line.contains("]]")) {
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
                    codes.append(line);

                    if (brackets >= 1) {
                        while (brackets >= 1) {
                            line = bufferedReader.readLine();
                            if (line == null)
                                continue;
                            if (line.contains("{"))
                                brackets++;
                            if (line.contains("}"))
                                brackets--;
                            codes.append(line);
                        }
                    }
                    getURL(codes.toString(), filename);
                    codes.setLength(0);
                }
            }
        }
    }

    private void getURL(String codes, String filename) {
        String tmp = new String(codes);

        if (tmp.contains("gUrlPatternList")) {
            String[] result1 = tmp.split("\\{");
            for (int i = 2; i < result1.length; i++) {
                String[] result2 = result1[i].split("\"");
                if (result2.length >= 5) {
                    urls.put(result2[5] + "//" + result2[1] + result2[3], filename);
                    //System.out.println(result2[5] + "//" + result2[1] + result2[3]);
                }
            }
        } else if (tmp.contains("gSSLHostPatternList")) {
            String[] result1 = tmp.split("\\{");
            for (int i = 2; i < result1.length; i++) {
                String[] result2 = result1[i].split("'");
                urls.put("https://" + result2[1], filename);
                //System.out.println("https://" + result2[1]);
            }
        } else {
            String[] lines = tmp.split("\\r?\\n");
            for (String line : lines) {
                if (line.contains("--")) {
                    continue;
                }

                if (line.contains("\"") || line.contains("'")) {
                    String[] result = line.split("\"|'");
                    if (result.length >= 5) {
                        for (int idx = 5; idx + 4 <= result.length; idx += 8) {
                            urls.put(result[idx] + "//" + result[idx - 4] + result[idx - 2], filename);
                            //System.out.println(result[idx] + "//" + result[idx - 4] + result[idx - 2]);

                            if ("https:".equals(result[idx + 1]))
                                idx++;
                        }
                    }
                }
            }
        }
    }

    public static void saveUrlAsExcel(Map<String, String> urls, String destinationDir) throws IOException {
        System.out.println("URL 엑셀 저장 시작합니당");
        // 오늘 날짜를 YYYYMMDD 형식으로 가져오기
        String dateFolder = new SimpleDateFormat("yyyyMMdd").format(new Date());
        File dateDir = new File(destinationDir, dateFolder);
        if (!dateDir.exists()) {
            dateDir.mkdirs();
        }

        // Result 하위 폴더 생성
        File resultDir = new File(dateDir, "Result");
        if (!resultDir.exists()) {
            resultDir.mkdirs();
        }

        // Excel 파일 경로 설정
        File excelFile = new File(resultDir, "urls.xlsx");

        System.out.println("URLs Excel file will be created at: " + excelFile.getAbsolutePath()); // 저장 경로 출력

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("URLs");

        // Header
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("URL");
        headerRow.createCell(1).setCellValue("Filename");

        // Data
        int rowNum = 1;
        for (Map.Entry<String, String> entry : urls.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }

        // 파일에 작성
        try (FileOutputStream fileOut = new FileOutputStream(excelFile)) {
            workbook.write(fileOut);
        } finally {
            workbook.close();
        }
        System.out.println("URLs Excel file has been successfully created at: " + excelFile.getAbsolutePath()); // 실제 저장된 경로 출력
    }
}