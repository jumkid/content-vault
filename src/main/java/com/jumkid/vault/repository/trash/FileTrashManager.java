package com.jumkid.vault.repository.trash;

import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.repository.FilePathManager;
import com.jumkid.vault.util.FileUtils;
import com.jumkid.vault.util.FileZipUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
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

    public void moveToTrash(Path filePath, String mediaFileId) throws FileStoreServiceException {
        try{
            checkTrashPath();
            Path trashTargetPath = Paths.get(filePathManager.getDataHomePath(),
                    filePathManager.getTrashPath(), mediaFileId);
            //archive file to target path
            fileZipUtils.zip(filePath, trashTargetPath);
            //delete the original file
            FileUtils.deleteDirectoryStream(filePath);
        } catch (IOException ioe) {
            throw new FileStoreServiceException("Failed to move file to trash " + filePath);
        }
    }

    public void emptyTrash() throws FileStoreServiceException {
        try {
            Path trashPath = Paths.get(filePathManager.getDataHomePath(), filePathManager.getTrashPath());
            FileUtils.deleteDirectoryStream(trashPath);
        } catch (Exception ex) {
            throw new FileStoreServiceException("Failed to empty the trash ");
        }
    }

    private void checkTrashPath() throws IOException {
        Path trashPath = Paths.get(filePathManager.getDataHomePath(), filePathManager.getTrashPath());
        if (!Files.exists(trashPath)) {
            Files.createDirectory(trashPath);
        }
    }

}
