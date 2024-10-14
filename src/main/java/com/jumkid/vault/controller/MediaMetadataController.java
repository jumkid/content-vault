package com.jumkid.vault.controller;

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.enums.MediaFileModule;
import com.jumkid.vault.exception.FileNotAvailableException;
import com.jumkid.vault.exception.FileNotFoundException;
import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.service.MediaFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
    @PreAuthorize("hasAnyAuthority('USER_ROLE', 'ADMIN_ROLE')")
    public List<MediaFile> searchMetadata(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) Integer size) throws FileStoreServiceException {
        if (q == null || q.isBlank()) q = "*";
        return fileService.searchMediaFile(q, size);
    }

    @GetMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('ADMIN_ROLE')" +
            " || @securityService.isPublic(#mediaFileId) || @securityService.isOwner(authentication, #mediaFileId)")
    public MediaFile getMetadata(@PathVariable("id") String mediaFileId)
            throws FileNotAvailableException, FileNotFoundException, FileStoreServiceException {
        return fileService.getMediaFile(mediaFileId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('USER_ROLE', 'ADMIN_ROLE')")
    public MediaFile addMetadata(@NotNull @Valid @RequestBody MediaFile mediaFile,
                                 @NotNull MediaFileModule mediaFileModule) throws FileStoreServiceException {
        return fileService.addMediaFile(mediaFile, mediaFileModule);
    }

    @PutMapping(value = "{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('ADMIN_ROLE')" +
            " || (hasAuthority('USER_ROLE') && @securityService.isOwner(authentication, #mediaFileId))")
    public MediaFile updateMetadata(@PathVariable("id") String mediaFileId,
                                    @NotNull @RequestBody MediaFile partialMediaFile)
            throws FileStoreServiceException, FileNotFoundException {
        return fileService.updateMediaFile(mediaFileId, partialMediaFile, null);
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ADMIN_ROLE')" +
            " || (hasAuthority('USER_ROLE') && @securityService.isOwner(authentication, #mediaFileId))")
    public Integer deleteMetadata(@PathVariable("id") String mediaFileId) throws FileStoreServiceException {
        return fileService.trashMediaFile(mediaFileId);
    }

}
