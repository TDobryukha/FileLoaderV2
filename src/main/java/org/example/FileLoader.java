package org.example;

import com.google.common.util.concurrent.RateLimiter;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

class FileLoader implements Runnable {
    private final String url;
    private final String saveDirectory;
    RateLimiter rateLimiter; // допускается null. в таком случае ограничений скорости нет

    public FileLoader(String url, String saveDirectory, RateLimiter rateLimiter) {
        this.url = url;
        this.saveDirectory = saveDirectory;
        this.rateLimiter = rateLimiter;
    }


    @Override
    public void run() {
        try (BufferedInputStream inputStream = new BufferedInputStream(new URL(this.url).openStream());
             FileOutputStream fileOS = new FileOutputStream(this.saveDirectory + this.url.substring(this.url.lastIndexOf("/")))) {
            System.out.println("Началась загрузка файла: " + this.url);
            byte[] data = new byte[1024];
            int byteContent;
            while ((byteContent = inputStream.read(data, 0, 1024)) != -1) {
                if (rateLimiter != null) {
                    this.rateLimiter.acquire();
                }
                fileOS.write(data, 0, byteContent);
            }
            System.out.println("Файл загружен:           " + this.url);
        } catch (IOException e) {
            System.out.println("Ошибка загрузки:  " + this.url);
            e.printStackTrace();
        }
    }
}