package com.jumkid.vault.controller;

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.service.MediaFileService;
import com.jumkid.vault.service.mapper.MediaFileMapper;
import com.jumkid.vault.util.ResponseMediaFileWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/file")
public class MediaUploadDownloadController {

    private final MediaFileService fileService;

    private final ResponseMediaFileWriter responseMFileWriter;

    private final MediaFileMapper mediaFileMapper;

    @Autowired
    public MediaUploadDownloadController(MediaFileService fileService, ResponseMediaFileWriter responseMFileWriter, MediaFileMapper mediaFileMapper) {
        this.fileService = fileService;
        this.responseMFileWriter = responseMFileWriter;
        this.mediaFileMapper = mediaFileMapper;
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MediaFile upload(@NotNull @RequestParam("file") MultipartFile file, HttpServletRequest httpRequest){
        MediaFile mediaFile = null;
        try {
            String title = httpRequest.getParameter("title");
            mediaFile = MediaFile.builder()
                    .title(title != null ? title : file.getName())
                    .filename(file.getOriginalFilename())
                    .size((int)file.getSize())
                    .mimeType(file.getContentType())
                    .build();

            setUserInfo(mediaFile);

            mediaFile = fileService.addMediaFile(mediaFile, file.getBytes());
            log.info("media file {} uploaded", mediaFile.getFilename());
            return mediaFile;
        } catch (IOException ioe) {
            throw new FileStoreServiceException("Failed to update file", mediaFile);
        }
    }

    private void setUserInfo(MediaFile mediaFile) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            mediaFile.setCreatedBy(userDetails.getUsername());
        } else {
            mediaFile.setCreatedBy(auth.getPrincipal().toString());
        }
    }

    @GetMapping("/download/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void download(@PathVariable("id") String id, HttpServletResponse response){
        MediaFileMetadata mediaFileMetadata = null;
        Optional opt = fileService.getFileSource(id);
        try {
            if(opt.isPresent()) {
                mediaFileMetadata = fileService.getMediaFileMetadata(id);
                byte[] bytes = (byte[])opt.get();
                responseMFileWriter.writeForDownload(mediaFileMetadata, bytes, response);
            }
        } catch (IOException ioe) {
            throw new FileStoreServiceException("Failed to download file", mediaFileMapper.metadataToDto(mediaFileMetadata));
        }
    }

}
