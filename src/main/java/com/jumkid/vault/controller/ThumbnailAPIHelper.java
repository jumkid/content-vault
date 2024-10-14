package com.jumkid.vault.controller;

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.enums.ThumbnailNamespace;
import com.jumkid.vault.exception.FileNotAvailableException;
import com.jumkid.vault.exception.FileNotFoundException;
import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.service.MediaFileService;
import com.jumkid.vault.util.ResponseMediaFileWriter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class ThumbnailAPIHelper {

    private final MediaFileService fileService;

    private final ResponseMediaFileWriter responseMFileWriter;

    public ThumbnailAPIHelper(MediaFileService fileService, ResponseMediaFileWriter responseMFileWriter) {
        this.fileService = fileService;
        this.responseMFileWriter = responseMFileWriter;
    }

    public void response(String mediaFileId, ThumbnailNamespace thumbnailNamespace, HttpServletResponse response)
            throws FileNotAvailableException, FileNotFoundException, FileStoreServiceException {
        Optional<byte[]> optional = fileService.getThumbnail(mediaFileId, thumbnailNamespace);
        if (optional.isPresent()) {
            MediaFile mediaFile = MediaFile.builder()
                    .uuid(mediaFileId)
                    .mimeType("image/png")
                    .build();
            responseMFileWriter.write(mediaFile, optional.get(), response);
        } else {
            MediaFile mediaFile = fileService.getMediaFile(mediaFileId);
            optional = fileService.getFileSource(mediaFileId);
            if (optional.isPresent()) {
                responseMFileWriter.write(mediaFile, optional.get(), response);
            } else if (mediaFile != null) {
                responseMFileWriter.write(mediaFile, response);
            } else {
                throw new FileNotFoundException(mediaFileId);
            }
        }
    }

}
