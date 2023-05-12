package com.jumkid.vault.controller;

import com.jumkid.share.security.AccessScope;
import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.enums.MediaFileModule;
import com.jumkid.vault.exception.FileNotFoundException;
import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.service.MediaFileService;
import com.jumkid.vault.service.mapper.MediaFileMapper;
import com.jumkid.vault.util.ResponseMediaFileWriter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/file")
public class MediaUploadDownloadController {

    private final MediaFileService fileService;

    private final ResponseMediaFileWriter responseMFileWriter;

    private final MediaFileMapper mediaFileMapper;

    @Autowired
    public MediaUploadDownloadController(MediaFileService fileService, ResponseMediaFileWriter responseMFileWriter,
                                         MediaFileMapper mediaFileMapper) {
        this.fileService = fileService;
        this.responseMFileWriter = responseMFileWriter;
        this.mediaFileMapper = mediaFileMapper;
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyAuthority('USER_ROLE', 'ADMIN_ROLE')")
    public MediaFile upload(@NotNull @RequestParam("file") MultipartFile file,
                            @RequestParam(required = false) String title,
                            @RequestParam(required = false) String content,
                            @RequestParam(required = false) List<String> tags,
                            @RequestParam AccessScope accessScope) {
        MediaFile mediaFile = null;
        try {
            mediaFile = MediaFile.builder()
                    .accessScope(accessScope)
                    .title(title != null ? title : file.getName())
                    .content(content)
                    .filename(file.getOriginalFilename())
                    .size((int)file.getSize())
                    .mimeType(file.getContentType())
                    .tags(tags)
                    .build();

            mediaFile.setFile(file.getBytes());
            setUserInfo(mediaFile);

            mediaFile = fileService.addMediaFile(mediaFile, MediaFileModule.FILE);
            log.debug("media file {} uploaded", mediaFile.getFilename());
            return mediaFile;
        } catch (IOException ioe) {
            throw new FileStoreServiceException("Failed to update file", mediaFile);
        }
    }

    @PostMapping("/multipleUpload")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyAuthority('USER_ROLE', 'ADMIN_ROLE')")
    public List<MediaFile> multipleUpload(@NotNull @RequestParam("files") MultipartFile[] files,
                                          @RequestParam(required = false) List<String> tags,
                                          @RequestParam AccessScope accessScope) {
        MediaFile mediaFile = null;
        List<MediaFile> mediaFileList = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                mediaFile = MediaFile.builder()
                        .accessScope(accessScope)
                        .title(file.getName())
                        .filename(file.getOriginalFilename())
                        .size((int)file.getSize())
                        .mimeType(file.getContentType())
                        .tags(tags)
                        .build();

                mediaFile.setFile(file.getBytes());
                setUserInfo(mediaFile);

                mediaFile = fileService.addMediaFile(mediaFile, MediaFileModule.FILE);
                mediaFileList.add(mediaFile);
                log.debug("media file {} uploaded", mediaFile.getFilename());
            }
        } catch (IOException ioe) {
            throw new FileStoreServiceException("Failed to update file ", mediaFile);
        }
        return mediaFileList;
    }

    private void setUserInfo(MediaFile mediaFile) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        if (userDetails != null) {
            mediaFile.setCreatedBy(userDetails.getUsername());
        } else {
            mediaFile.setCreatedBy(auth.getPrincipal().toString());
        }
    }

    @GetMapping("/download/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('GUEST_ROLE', 'USER_ROLE', 'ADMIN_ROLE')" +
            " && (@securityService.isPublic(#mediaFileId) || @securityService.isOwner(authentication, #mediaFileId))")
    public void download(@PathVariable("id") String mediaFileId, HttpServletResponse response){
        MediaFileMetadata mediaFileMetadata = null;
        Optional<byte[]> opt = fileService.getFileSource(mediaFileId);
        try {
            if(opt.isPresent()) {
                mediaFileMetadata = fileService.getMediaFileMetadata(mediaFileId);
                byte[] bytes = opt.get();
                responseMFileWriter.writeForDownload(mediaFileMetadata, bytes, response);
            } else {
                throw new FileNotFoundException(mediaFileId);
            }
        } catch (IOException ioe) {
            throw new FileStoreServiceException("Failed to download file", mediaFileMapper.metadataToDto(mediaFileMetadata));
        }
    }

}
