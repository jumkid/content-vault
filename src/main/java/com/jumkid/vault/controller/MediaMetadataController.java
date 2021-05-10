package com.jumkid.vault.controller;

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.exception.FileNotFoundException;
import com.jumkid.vault.service.MediaFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/metadata")
public class MediaMetadataController {

    private final MediaFileService fileService;

    @Autowired
    public MediaMetadataController(MediaFileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('user', 'admin')")
    public List<MediaFile> searchMetadata(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) Integer size){
        if (q == null || q.isBlank()) q = "*";
        return fileService.searchMediaFile(q, size);
    }

    @GetMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    public MediaFile getMetadata(@PathVariable("id") String mediaFileId){
        MediaFile mediaFile = fileService.getMediaFile(mediaFileId);
        if (mediaFile != null) {
            return mediaFile;
        } else {
            throw new FileNotFoundException(mediaFileId);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public MediaFile addMetadata(@NotNull @Valid @RequestBody MediaFile mediaFile){
        return fileService.addMediaFile(mediaFile, null);
    }

    @PutMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('admin')" +
            " || (hasAnyAuthority('user') && @securityService.isOwner(authentication, #mediaFileId))")
    public MediaFile updateMetadata(@PathVariable("id") String mediaFileId, @NotNull @RequestBody MediaFile mediaFile){
        return fileService.updateMediaFile(mediaFileId, mediaFile, null);
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('admin')" +
            " || (hasAnyAuthority('user') && @securityService.isOwner(authentication, #mediaFileId))")
    public void deleteMetadata(@PathVariable("id") String mediaFileId) {
        fileService.trashMediaFile(mediaFileId);
    }

}
