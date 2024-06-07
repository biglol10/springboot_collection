package com.alibou.booknetwork.file;

import com.alibou.booknetwork.book.Book;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.io.File.separator;
import static java.lang.System.currentTimeMillis;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageService {
    @Value("${application.file.upload.photos-output-path}")
    private String fileUploadPath;

    public String saveFile(@NonNull MultipartFile sourceFile,
                           @NonNull Integer userId) { // String is the file path

        // using File.separator to make the code platform independent and avoid caring about slashe
        final String fileUploadSubPath = "users" + separator + userId;
        return uploadFile(sourceFile, fileUploadSubPath);
    }

    private String uploadFile(@NonNull MultipartFile sourceFile,
                              @NonNull String fileUploadSubPath) {
        final String finalUploadPath = fileUploadPath + separator + fileUploadSubPath;
        File targetFoler = new File(finalUploadPath);
        if (!targetFoler.exists()) {
            boolean folderCreated = targetFoler.mkdirs(); // mkdirs creates the folder and all its sub folders
            if (!folderCreated) {
                log.warn("Failed to create the target folder");
                return null;
            }
        }

        final String fileExtension = getFileExtension(sourceFile.getOriginalFilename());
        // ./upload/users/1/3123123123.jpg
        String targetFilePath = finalUploadPath + separator + currentTimeMillis() + "." + fileExtension;

        Path targetPath = Paths.get(targetFilePath); // create the path object
        try {
            Files.write(targetPath, sourceFile.getBytes()); // write the file to the target path
            log.info("File saved to " + targetFilePath);
            return targetFilePath;
        } catch (IOException e) {
            log.error("File war not savaed", e);
        }
        return null;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1) { // does not have an extension
            return "";
        }

        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }
}
