package com.jumkid.vault.repository;

import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.util.FileZipUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FileTrashManager {

    private final FilePathManager filePathManager;

    private final FileZipUtils fileZipUtils;

    public FileTrashManager(FilePathManager filePathManager, FileZipUtils fileZipUtils) {
        this.filePathManager = filePathManager;
        this.fileZipUtils = fileZipUtils;
    }

    public void moveToTrash(Path filePath, String id) {
        //move file to trash
        Path trashTargetPath = Paths.get(filePathManager.getDataHomePath(), filePathManager.getTrashPath(), id);
        try{
            //Files.move(filePath, trashPath, StandardCopyOption.ATOMIC_MOVE);
            fileZipUtils.zip(filePath, trashTargetPath);
        }catch(IOException ioe){
            throw new FileStoreServiceException("Failed to move file to trash " + filePath);
        }
    }

}
