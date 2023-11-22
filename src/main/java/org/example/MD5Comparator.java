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

        System.out.println("Differences found: " + differencesFound); // 로그 추가
        if (differencesFound) {
            try {
                updateNewUrls(differentFiles.keySet(), destinationDir, previousDay);
            } catch (IOException e) {
                System.out.println("An error occurred while updating new URLs: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("MD5 해시값에 차이가 없습니다.");
        }
    }

    private static void updateNewUrls(Set<String> currentFileNames, String destinationDir, String previousDay) throws IOException {
        String dateFolder = new SimpleDateFormat("yyyyMMdd").format(new Date());
        System.out.println("updateNewUrl 메소드 진입");
    
        for (String filename : currentFileNames) {
            String category = getCategoryFromFilename(filename);
            String currentFilePath, previousFilePath;
    
            if (category.equals("unknown")) {
                // unknown 카테고리의 경우, 특별한 파일 경로 사용
                currentFilePath = destinationDir + dateFolder + "\\Result\\unknown.xlsx";
                previousFilePath = destinationDir + previousDay + "\\Result\\unknown.xlsx";
            } else {
                // 다른 카테고리의 경우, 기존 방식대로 파일 경로 설정
                currentFilePath = destinationDir + dateFolder + "\\Result\\" + category + ".xlsx";
                previousFilePath = destinationDir + previousDay + "\\Result\\" + category + ".xlsx";
            }
    
            Set<String> previousUrls = readUrlsFromExcel(previousFilePath);
            Map<String, Integer> currentUrls = readUrlsFromExcelWithRow(currentFilePath);
    
            System.out.println("Previous URLs count: " + previousUrls.size());
            System.out.println("Current URLs count: " + currentUrls.size());
    
            for (Map.Entry<String, Integer> entry : currentUrls.entrySet()) {
                String url = entry.getKey();
                Integer rowNum = entry.getValue();
                if (!previousUrls.contains(url)) {
                    markInExcel(currentFilePath, rowNum, "New");
                }
            }
    
            // 삭제된 URL 확인
            for (String prevUrl : previousUrls) {
                if (!currentUrls.containsKey(prevUrl)) {
                    // 이전 파일에서만 존재하고 현재 파일에서는 없는 URL을 삭제된 것으로 처리
                    // 삭제된 URL을 별도로 처리하는 로직을 여기에 추가합니다.
                    // 예: 삭제된 URL 목록을 다른 시트나 파일에 기록
                    handleDeletedUrl(destinationDir, dateFolder, category, prevUrl);
                }
            }
        }
    }
    
    private static void handleDeletedUrl(String destinationDir, String dateFolder, String category, String url) throws IOException {
        // 삭제된 URL을 다루는 로직
        // 예: 별도의 파일 또는 시트에 삭제된 URL 기록
        // 이 예시에서는 삭제된 URL을 별도의 파일에 기록합니다.
        String deletedUrlsFilePath = destinationDir + dateFolder + "\\DeletedUrls.xlsx";
        appendUrlToExcel(deletedUrlsFilePath, category, url);
    }
    
    private static void appendUrlToExcel(String filePath, String category, String url) throws IOException {
        File file = new File(filePath);
        boolean newFile = !file.exists();
    
        // try-with-resources를 사용하여 자동으로 자원을 닫도록 함
        try (Workbook workbook = newFile ? new XSSFWorkbook() : new XSSFWorkbook(new FileInputStream(file))) {
            Sheet sheet = workbook.getSheet(category);
            if (sheet == null) {
                sheet = workbook.createSheet(category);
            }
    
            // URL이 이미 존재하는지 확인
            if (!isUrlPresent(sheet, url)) {
                int lastRowNum = sheet.getLastRowNum();
                if (newFile || lastRowNum == 0) {
                    Row header = sheet.createRow(0);
                    header.createCell(0).setCellValue("URL");
                    lastRowNum = 1;
                } else {
                    lastRowNum += 1;
                }
    
                Row row = sheet.createRow(lastRowNum);
                Cell cell = row.createCell(0);
                cell.setCellValue(url);
            }
    
            // 파일에 변경 사항을 쓰기
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }
        }
    }
    
    private static boolean isUrlPresent(Sheet sheet, String url) {
        DataFormatter formatter = new DataFormatter();
        for (Row row : sheet) {
            if (formatter.formatCellValue(row.getCell(0)).equals(url)) {
                return true;
            }
        }
        return false;
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

    private static void markInExcel(String filePath, int rowNum, String status) throws IOException {
        // FileInputStream을 try-with-resources 내에서 생성하고 자동으로 닫도록 함
        try (FileInputStream fileInputStream = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(fileInputStream)) {
    
            Sheet sheet = workbook.getSheetAt(0);
            Row row = sheet.getRow(rowNum);
    
            if (row == null) {
                row = sheet.createRow(rowNum);
            }
    
            Cell statusCell = row.getCell(2); // 상태를 표시할 셀, 여기서는 세 번째 열(인덱스 2)라고 가정합니다.
    
            if (statusCell == null) {
                statusCell = row.createCell(2);
            }
    
            statusCell.setCellValue(status); // 셀에 상태를 설정합니다.
    
            // 변경 사항을 파일에 저장합니다.
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