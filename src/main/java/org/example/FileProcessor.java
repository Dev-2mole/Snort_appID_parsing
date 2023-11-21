package org.example;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileProcessor {

    private File[] luaFiles;
    private String destinationDir;

    public FileProcessor(File[] luaFiles, String destinationDir) {
        this.luaFiles = luaFiles;
        this.destinationDir = destinationDir;
    }

    public void searchDirectoryAndComputeMD5() {
        if (luaFiles != null && luaFiles.length > 0) {
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
            File excelFile = new File(resultDir, "MD5HashValues.xlsx");
    
            // Workbook과 Sheet 생성
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("MD5 Hash Values");
    
                // Excel header row
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("Filename");
                headerRow.createCell(1).setCellValue("MD5 Hash");
    
                // 파일 처리 및 Excel 작성
                for (int i = 0; i < luaFiles.length; i++) {
                    File file = luaFiles[i];
                    String md5Hash = calculateMD5(file);
                    Row row = sheet.createRow(i + 1);
                    row.createCell(0).setCellValue(file.getName());
                    row.createCell(1).setCellValue(md5Hash);
                }
    
                // 파일에 작성
                try (FileOutputStream fileOut = new FileOutputStream(excelFile)) {
                    workbook.write(fileOut);
                }
                System.out.println("MD5 Hash Values file " + excelFile.getAbsolutePath());
            } catch (IOException e) {
                System.out.println("Failed to create MD5 Hash Values Excel file");
                e.printStackTrace();
            }
        } else {
            System.out.println("The directory with the specified name does not exist.");
        }
    }
    
    

    private String calculateMD5(File file) {
        try {
            return DigestUtils.md5Hex(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
