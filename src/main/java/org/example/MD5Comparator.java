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

    public static void compareMD5HashesWithPreviousDay(String previousDay) {
        String currentDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String currentDayFilePath = "C:\\Temp\\" + currentDate + "\\Result\\MD5HashValues.xlsx";
        String previousDayFilePath = "C:\\Temp\\" + previousDay + "\\Result\\MD5HashValues.xlsx";
        String destinationDir = "C:\\Temp\\";  // 파일이 저장되는 기본 경로
    
        Map<String, String> md5HashesCurrent = readMD5HashesFromFile(currentDayFilePath);
        Map<String, String> md5HashesPrevious = readMD5HashesFromFile(previousDayFilePath);
    
        compareMD5Hashes(md5HashesCurrent, md5HashesPrevious, destinationDir, previousDay);
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

    private static void compareMD5Hashes(Map<String, String> md5HashesCurrent, Map<String, String> md5HashesPrevious, String destinationDir, String previousDay) {
        boolean differencesFound = false;
        Map<String, String> differentFiles = new HashMap<>();

        for (String filename : md5HashesCurrent.keySet()) {
            String md5HashCurrent = md5HashesCurrent.get(filename);
            String md5HashPrevious = md5HashesPrevious.getOrDefault(filename, "");

            if (!md5HashCurrent.equals(md5HashPrevious)) {
                System.out.println("MD5 hash difference found for file: " + filename);
                differentFiles.put(filename, getCategoryFromFilename(filename));
                differencesFound = true;
            }
        }

        if (differencesFound) {
            try {
                updateNewUrls(differentFiles, destinationDir, previousDay);
            } catch (IOException e) {
                System.out.println("An error occurred while updating new URLs: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("No differences in MD5 hashes found.");
        }
    }

    private static void updateNewUrls(Map<String, String> differentFiles, String destinationDir, String previousDay) throws IOException {
        String dateFolder = new SimpleDateFormat("yyyyMMdd").format(new Date());
    
        for (String category : differentFiles.keySet()) {
            // 예: "client", "content_group", "payload", "service", "ssl_host_group"
            String currentFilePath = destinationDir + dateFolder + "\\Result\\" + differentFiles.get(category) + ".xlsx";
            String previousFilePath = destinationDir + previousDay + "\\Result\\" + differentFiles.get(category) + ".xlsx";
    
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

    private static String getCategoryFromFilename(String filename) {
        if (filename.startsWith("client")) {
            return "client";
        } else if (filename.startsWith("content_group")) {
            return "content_group";
        } else if (filename.startsWith("payload")) {
            return "payload";
        } else if (filename.startsWith("service")) {
            return "service";
        } else if (filename.startsWith("ssl_host_group")) {
            return "ssl_host_group";
        } else {
            return "unknown"; // 파일명이 어떤 카테고리에도 속하지 않는 경우
        }
    }    
}