package org.example;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileProcessor {

    private File[] luaFiles;

    public FileProcessor(File[] luaFiles) {
        this.luaFiles = luaFiles;
    }

    public void searchDirectoryAndComputeMD5() {
        if (luaFiles != null && luaFiles.length > 0) {
            // Excel file creation
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("MD5 Hash Values");

            // Excel header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Filename");
            headerRow.createCell(1).setCellValue("MD5 Hash");

            for (int i = 0; i < luaFiles.length; i++) {
                File file = luaFiles[i];
                System.out.println("filename : " + file.getName());

                // MD5 hash calculation
                String md5Hash = calculateMD5(file);

                // Adding results to Excel
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(file.getName());
                row.createCell(1).setCellValue(md5Hash);

                System.out.println("MD5 Hash: " + md5Hash);
            }

            // Saving Excel file
            try (FileOutputStream fileOut = new FileOutputStream("C:\\Temp\\MD5HashValues.xlsx")) {
                workbook.write(fileOut);
                workbook.close();
                System.out.println("MD5 Hash Values Excel file has been created.");
            } catch (IOException e) {
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
