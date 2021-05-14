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
import java.util.*;
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

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('user', 'admin')")
    public MediaFile add(@NotNull @RequestParam("title") String title,
                         @RequestParam(value = "content", required = false) String content,
                         @RequestParam(value = "tags", required = false) List<String> tags,
                         @RequestParam(value = "files", required = false) MultipartFile[] files) {
        MediaFile gallery = MediaFile.builder()
                                    .title(title)
                                    .content(content)
                                    .tags(tags)
                                    .build();

        List<MediaFile> mediaFileList = new ArrayList<>();
        MediaFile mediaFile = null;
        try {
            if (files != null) {
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
                log.debug("media gallery {} created with {} children", gallery.getTitle(), gallery.getChildren().size());
            }
        } catch (IOException ioe) {
            throw new FileStoreServiceException("Failed to update file ", mediaFile);
        }

        gallery = fileService.addMediaGallery(gallery);

        return gallery;
    }

    @PutMapping("{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyAuthority('user', 'admin')")
    public MediaFile update(@PathVariable(value = "id") String galleryId,
                            @RequestParam(value = "mediaFileIds", required = false) List<String> childIds,
                            @RequestParam(value = "files", required = false) MultipartFile[] files,
                            @RequestParam(value = "title", required = false) String title,
                            @RequestParam(value = "content", required = false) String content,
                            @RequestParam(value = "tags", required = false) List<String> tags) {
        Map<MediaFileField, Object> fieldValueMap = new EnumMap<>(MediaFileField.class);
        if (title != null && !title.isBlank()) { fieldValueMap.put(MediaFileField.TITLE, title); }
        if (content != null && !content.isBlank()) { fieldValueMap.put(MediaFileField.CONTENT, content); }
        if (tags != null && !tags.isEmpty()) { fieldValueMap.put(MediaFileField.TAGS, tags); }
        //reset all children by the given list
        if (childIds != null && !childIds.isEmpty()) {
            List<MediaFile> mediaFileList = childIds.stream()
                    .map(childId -> MediaFile.builder().uuid(childId).build())
                    .collect(Collectors.toList());
            fieldValueMap.put(MediaFileField.CHILDREN, mediaFileList);
        }
        List<MediaFile> newChildList = processNewChildren(files);
        if (!newChildList.isEmpty()) { mergeNewChildren(fieldValueMap, newChildList); }

        if (fileService.updateMediaFileFields(galleryId, fieldValueMap)) {
            return fileService.getMediaFile(galleryId);
        } else {
            throw new FileStoreServiceException("Failed to update gallery");
        }
    }

    private List<MediaFile> processNewChildren(MultipartFile[] files){
        List<MediaFile> newChildList = new ArrayList<>();
        try {
            if (files != null) {
                for (MultipartFile file : files) {
                    MediaFile newChild = MediaFile.builder()
                            .filename(file.getOriginalFilename())
                            .size((int)file.getSize())
                            .mimeType(file.getContentType())
                            .build();
                    newChild.setFile(file.getBytes());
                    newChildList.add(newChild);
                }
            }
        } catch (IOException ioe) {
            log.error("Failed to update file");
        }
        return newChildList;
    }

    private void mergeNewChildren(Map<MediaFileField, Object> fieldValueMap, List<MediaFile> newChildList) {
        if (fieldValueMap.containsKey(MediaFileField.CHILDREN)) {
            ((List<MediaFile>)fieldValueMap.get(MediaFileField.CHILDREN)).addAll(newChildList);
        } else if (!newChildList.isEmpty()) {
            fieldValueMap.put(MediaFileField.CHILDREN, newChildList);
        }
    }

}
