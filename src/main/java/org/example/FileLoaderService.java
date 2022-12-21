package org.example;

import com.google.common.util.concurrent.RateLimiter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class FileLoaderService {
    private static final int DEFAULT_NUMBER_OF_THREADS = 2;
    private static final String DEFAULT_SAVE_DIRECTORY = "D:/Downloads/";
    private static final int DEFAULT_RATE_LIMIT = 50000; // inputStream.read в FileLoader загружает по 1кб за итерацию. rateLimit = 50000 ограничивает загрузку до 50 мб/сек
    private final List<String> urlList;
    private final String saveDirectory;

    private final int numberOfThreads;
    private final int rateLimit;


    public FileLoaderService(String fileName) throws IOException, IllegalArgumentException {
        this(fileName, DEFAULT_SAVE_DIRECTORY, DEFAULT_NUMBER_OF_THREADS, DEFAULT_RATE_LIMIT);
    }

    public FileLoaderService(String fileName, String saveDirectory, int numberOfThreads, int rateLimit) throws IOException, IllegalArgumentException {
        checkArguments(fileName, saveDirectory, numberOfThreads, rateLimit);
        this.urlList = getUrlListFromFile(fileName);
        this.saveDirectory = saveDirectory;
        this.numberOfThreads = numberOfThreads;
        this.rateLimit = rateLimit;
    }

    private static List<String> getUrlListFromFile(String fileName) {
        List<String> filesNameList = null;
        try (Stream<String> lines = Files.lines(Paths.get(fileName))) {
            filesNameList = lines.collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return filesNameList.stream().filter(s -> !s.isBlank()).collect(Collectors.toList());
    }

    private static void checkArguments(String fileName, String saveDirectory, int numberOfThreads, int rateLimit) throws IOException, IllegalArgumentException {
        if (saveDirectory.isBlank() || numberOfThreads <= 0 || rateLimit < 0) {
            throw new IllegalArgumentException("Путь к папке для загрузки не должен быть пустым,\nчисло потоков должно быть больше 0,\n ограничение на скорость должно быть больше или равно 0");
        }
        if (fileName.isBlank() || Files.notExists(Paths.get(fileName))) {
            throw new FileNotFoundException("Ошибка!!! Файл со списком ссылок не найден: " + fileName);
        }
        Path path = Paths.get(saveDirectory);
        if (Files.notExists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                throw new IOException("Ошибка!!! Ошибка создания папки: " + saveDirectory);
            }
        }
    }


    public void load() {
        if (this.urlList.isEmpty()) {
            System.out.println("Список файлов для загрузки пустой.");
            return;
        }
        ExecutorService service = Executors.newFixedThreadPool(this.numberOfThreads);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        RateLimiter limiter = null;
        if (rateLimit != 0) {
            limiter = RateLimiter.create(rateLimit); // ограничивает скорость загрузки
        }
        Iterator<String> iterator = this.urlList.iterator();
        long startTime = System.currentTimeMillis();
        while (iterator.hasNext()) {
            String url = iterator.next();
            futures.add(CompletableFuture.runAsync(new FileLoader(url, this.saveDirectory, limiter), service));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[this.urlList.size()])).join();
        System.out.printf("Затрачено времени %d с. \n", (System.currentTimeMillis() - startTime) / 1000);
        service.shutdown();
    }
}
