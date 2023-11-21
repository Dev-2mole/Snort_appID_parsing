package org.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

public class UpdateFileChecker {

    private ChromeDriver driver;

    public UpdateFileChecker(ChromeDriver driver) {
        this.driver = driver;
    }

    public int validateMD5Hash(String MD5_HASH_file, int[] check_hash) throws InterruptedException {
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
}

