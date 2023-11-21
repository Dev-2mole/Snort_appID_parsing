package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MD5Comparator {

    public static void compareMD5HashesWithPreviousDay(String previousDayFilePath, String destinationDir) {
        String currentDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String currentDayFilePath = "C:\\Temp\\" + currentDate + "\\Result\\MD5HashValues.xlsx";
    
        Map<String, String> md5HashesCurrent = readMD5HashesFromFile(currentDayFilePath);
        Map<String, String> md5HashesPrevious = readMD5HashesFromFile(previousDayFilePath);
    
        // 여기에서 destinationDir 매개변수 추가
        compareMD5Hashes(md5HashesCurrent, md5HashesPrevious, destinationDir);
    }

    private static Map<String, String> readMD5HashesFromFile(String filePath) {
        Map<String, String> md5Hashes = new HashMap<>();
        try (FileInputStream fileInputStream = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(fileInputStream)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row
                String filename = row.getCell(0).getStringCellValue();
                String md5Hash = row.getCell(1).getStringCellValue();
                md5Hashes.put(filename, md5Hash);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return md5Hashes;
    }

    private static void compareMD5Hashes(Map<String, String> md5HashesCurrent, Map<String, String> md5HashesPrevious, String destinationDir) {
        boolean differencesFound = false;
        Map<String, String> differentFiles = new HashMap<>();
    
        for (String filename : md5HashesCurrent.keySet()) {
            String md5HashCurrent = md5HashesCurrent.get(filename);
            String md5HashPrevious = md5HashesPrevious.getOrDefault(filename, "");
    
            if (!md5HashCurrent.equals(md5HashPrevious)) {
                System.out.println("MD5 hash difference found for file: " + filename);
                differentFiles.put(filename, md5HashCurrent);
                differencesFound = true;
            }
        }
    
        if (!differencesFound) {
            System.out.println("No differences in MD5 hashes found.");
        } else {
            try {
                updateNewUrls(differentFiles, destinationDir);
            } catch (IOException e) {
                System.out.println("An error occurred while updating new URLs: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void updateNewUrls(Map<String, String> differentFiles, String destinationDir) throws IOException {
        String dateFolder = new SimpleDateFormat("yyyyMMdd").format(new Date());
        File dateDir = new File(destinationDir, dateFolder);
        File resultDir = new File(dateDir, "Result");

        for (String category : differentFiles.keySet()) {
            String previousFilePath = destinationDir + File.separator + differentFiles.get(category) + File.separator + "Result" + File.separator + category + ".xlsx";
            String currentFilePath = resultDir.getAbsolutePath() + File.separator + category + ".xlsx";
            Set<String> previousUrls = readUrlsFromExcel(previousFilePath);
            Map<String, Integer> currentUrls = readUrlsFromExcelWithRow(currentFilePath);

            for (String url : currentUrls.keySet()) {
                if (!previousUrls.contains(url)) {
                    // 새로운 URL 발견
                    markNewInExcel(currentFilePath, currentUrls.get(url));
                }
            }
        }
    }

    private static Set<String> readUrlsFromExcel(String filePath) throws IOException {
        Set<String> urls = new HashSet<>();
        try (FileInputStream fileInputStream = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(fileInputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row
                Cell cell = row.getCell(0);
                if (cell != null) {
                    urls.add(cell.getStringCellValue());
                }
            }
        }
        return urls;
    }

    private static Map<String, Integer> readUrlsFromExcelWithRow(String filePath) throws IOException {
        Map<String, Integer> urls = new HashMap<>();
        try (FileInputStream fileInputStream = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(fileInputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row
                Cell cell = row.getCell(0);
                if (cell != null) {
                    urls.put(cell.getStringCellValue(), row.getRowNum());
                }
            }
        }
        return urls;
    }

    private static void markNewInExcel(String filePath, int rowNum) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(fileInputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row row = sheet.getRow(rowNum);
            if (row == null) return;

            Cell cell = row.createCell(2); // "New" 열은 세 번째 열(인덱스 2)로 가정
            cell.setCellValue("New");

            try (FileOutputStream fileOut = new FileOutputStream(new File(filePath))) {
                workbook.write(fileOut);
            }
        }
    }
}