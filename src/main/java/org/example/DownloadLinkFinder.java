package org.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.List;

public class DownloadLinkFinder {

    private ChromeDriver driver;

    public DownloadLinkFinder(ChromeDriver driver) {
        this.driver = driver;
    }

    public String findDownloadLink() throws InterruptedException {
        driver.get("https://snort.org/downloads");
        Thread.sleep(2000);

        WebElement webBodyElement = driver.findElement(By.className("background_grey"));
        List<WebElement> webElementList = webBodyElement.findElements(By.tagName("a"));
        String downloadLink = null;

        for (WebElement element : webElementList) {
            String href = element.getAttribute("href");
            if (href != null && element.getText().equals("snort-openappid.tar.gz")) {
                downloadLink = href;
                System.out.println("링크: " + href);
            }
        }

        return downloadLink;
    }
}