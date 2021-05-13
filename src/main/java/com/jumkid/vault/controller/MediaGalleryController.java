package com.jumkid.vault.controller;

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.enums.MediaFileField;
import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.service.MediaFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/gallery")
public class MediaGalleryController {

    private final MediaFileService fileService;

    @Autowired
    public MediaGalleryController(MediaFileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyAuthority('user', 'admin')")
    public MediaFile upload(@NotNull @RequestParam("files") MultipartFile[] files,
                                          @NotNull @RequestParam("title") String title,
                                          @RequestParam(value = "tags", required = false) List<String> tags) {
        MediaFile gallery = MediaFile.builder().title(title).tags(tags).build();

        List<MediaFile> mediaFileList = new ArrayList<>();
        MediaFile mediaFile = null;
        try {
            for (MultipartFile file : files) {
                mediaFile = MediaFile.builder()
                        .filename(file.getOriginalFilename())
                        .size((int)file.getSize())
                        .mimeType(file.getContentType())
                        .build();

                mediaFile.setFile(file.getBytes());

                mediaFileList.add(mediaFile);
            }

            gallery.setChildren(mediaFileList);
            gallery = fileService.addMediaGallery(gallery);
            log.debug("media gallery {} uploaded with {} files", gallery.getTitle(), gallery.getChildren().size());
        } catch (IOException ioe) {
            throw new FileStoreServiceException("Failed to update file ", mediaFile);
        }

        return gallery;
    }

    @PutMapping("{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyAuthority('user', 'admin')")
    public MediaFile update(@PathVariable(value = "id") String galleryId,
                            @RequestParam(value = "childIdList", required = false) List<String> childIdList,
                            @RequestParam(value = "title", required = false) String title,
                            @RequestParam(value = "content", required = false) String content,
                            @RequestParam(value = "tags", required = false) List<String> tags) {
        Map<MediaFileField, Object> fieldValueMap = new HashMap<>();
        if (title != null && !title.isBlank()) { fieldValueMap.put(MediaFileField.TITLE, title); }
        if (content != null && !content.isBlank()) { fieldValueMap.put(MediaFileField.CONTENT, content); }
        if (tags != null && !tags.isEmpty()) { fieldValueMap.put(MediaFileField.TAGS, tags); }
        if (childIdList != null && !childIdList.isEmpty()) {
            List<MediaFile> mediaFileList = childIdList.stream()
                    .map(childId -> MediaFile.builder().uuid(childId).build())
                    .collect(Collectors.toList());
            fieldValueMap.put(MediaFileField.CHILDREN, mediaFileList);
        }

        if (fileService.updateMediaFileFields(galleryId, fieldValueMap)) {
            return fileService.getMediaFile(galleryId);
        } else {
            throw new FileStoreServiceException("Failed to update gallery");
        }
    }

}
