package com.jumkid.vault.controller;

import com.jumkid.share.security.AccessScope;
import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.controller.dto.MediaFileProp;
import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.service.MediaFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.*;

import static com.jumkid.vault.util.Constants.PROP_FEATURED_ID;

@Slf4j
@RestController
@RequestMapping("/gallery")
public class MediaGalleryController {

    private final MediaFileService mediaService;

    @Autowired
    public MediaGalleryController(MediaFileService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('USER_ROLE', 'ADMIN_ROLE')")
    public MediaFile add(@NotNull @RequestParam String title,
                         @RequestParam AccessScope accessScope,
                         @RequestParam(required = false) String content,
                         @RequestParam(required = false) List<String> tags,
                         @RequestParam(required = false) MultipartFile[] files) {
        MediaFile gallery = MediaFile.builder()
                .accessScope(accessScope)
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
                            .accessScope(accessScope)
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

        gallery = mediaService.addMediaGallery(gallery);

        return gallery;
    }

    @PostMapping("/clone/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('USER_ROLE', 'ADMIN_ROLE')" +
            " && (@securityService.isPublic(#galleryId) || @securityService.isOwner(authentication, #galleryId))")
    public MediaFile clone(@NotNull @PathVariable("id") String galleryId,
                           @NotNull @RequestParam(required = false) String title) {
        return mediaService.cloneMediaGallery(galleryId, title);
    }

    @PostMapping(value = "{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyAuthority('USER_ROLE', 'ADMIN_ROLE')" +
            " && @securityService.isOwner(authentication, #galleryId)")
    public MediaFile updateItems(@NotNull @PathVariable("id") String galleryId,
                            @RequestParam(value = "featuredId", required = false) String featuredId,
                            @RequestParam(value = "mediaFileIds", required = false) List<String> childIds,
                            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        MediaFile partialMediaFile = MediaFile.builder().uuid(galleryId).build();

        boolean hasUpdate = false;
        if (childIds != null && !childIds.isEmpty()) {
            List<MediaFile> mediaFileList = childIds.stream()
                    .map(childId -> MediaFile.builder().uuid(childId).build())
                    .toList();

            partialMediaFile.setChildren(mediaFileList);

            hasUpdate = true;
        }

        if (files != null) {
            MediaFile galleryMeta = mediaService.getMediaFile(galleryId);

            List<MediaFile> childList = processNewChildren(files, galleryMeta.getAccessScope());
            if (partialMediaFile.getChildren() != null) {
                partialMediaFile.getChildren().addAll(childList);
            } else {
                partialMediaFile.setChildren(childList);
            }
            hasUpdate = true;
        }

        if (featuredId != null) {
            partialMediaFile.setProps(List.of(MediaFileProp.builder()
                    .name(PROP_FEATURED_ID).textValue(featuredId)
                    .build()));
            hasUpdate = true;
        }

        if (hasUpdate) return mediaService.updateMediaGallery(galleryId, partialMediaFile);
        else return null;
    }

    @PutMapping("{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('ADMIN_ROLE')" +
            " || (hasAuthority('USER_ROLE') && @securityService.isOwner(authentication, #galleryId))")
    public MediaFile update(@PathVariable(value = "id") String galleryId,
                            @NotNull @RequestBody MediaFile partialMediaFile) {
        return mediaService.updateMediaGallery(galleryId, partialMediaFile);
    }

    private List<MediaFile> processNewChildren(MultipartFile[] files, AccessScope accessScope){
        List<MediaFile> newChildList = new ArrayList<>();
        try {
            if (files != null) {
                for (MultipartFile file : files) {
                    MediaFile newChild = MediaFile.builder()
                            .accessScope(accessScope)
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

}
