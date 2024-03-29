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
        Map<String, Workbook> workbooks = new HashMap<>();
        workbooks.put("client", new XSSFWorkbook());
        workbooks.put("content_group", new XSSFWorkbook());
        workbooks.put("payload", new XSSFWorkbook());
        workbooks.put("service", new XSSFWorkbook());
        workbooks.put("ssl_host_group", new XSSFWorkbook());

        workbooks.forEach((key, workbook) -> {
            Sheet sheet = workbook.createSheet("URLs");

            // 헤더 행 생성
            String[] headers = {"URL", "Last Check Date", "New", "Success/Failure", "Response Code/Error", 
                                "Redirection URL", "Redirection Response/Error", "Source Code Name"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // 데이터 행 추가
            int rowNum = 1;
            for (Map.Entry<String, String> entry : urls.entrySet()) {
                if (entry.getValue().startsWith(key)) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(entry.getKey()); // URL
                    row.createCell(1).setCellValue((String) null); // Last Check Date
                    row.createCell(2).setCellValue((String) null); // New
                    row.createCell(3).setCellValue((String) null); // Success/Failure
                    row.createCell(4).setCellValue((String) null); // Response Code/Error
                    row.createCell(5).setCellValue((String) null); // Redirection URL
                    row.createCell(6).setCellValue((String) null); // Redirection Response/Error
                    row.createCell(7).setCellValue(entry.getValue()); // Source Code Name
                }
            }

            // 파일에 작성
            try (FileOutputStream fileOut = new FileOutputStream(new File(resultDir, key + ".xlsx"))) {
                workbook.write(fileOut);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        System.out.println("URLs Excel files have been successfully created in: " + resultDir.getAbsolutePath());
    }
}