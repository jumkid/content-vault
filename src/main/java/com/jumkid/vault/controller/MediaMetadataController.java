package com.jumkid.vault.controller;

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.exception.FileNotFoundException;
import com.jumkid.vault.service.MediaFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
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
    public List<MediaFile> getAllMetadata(){
        return fileService.getAll();
    }

    @GetMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    public MediaFile getMetadata(@PathVariable("id") String id){
        MediaFile mediaFile = fileService.getMediaFile(id);
        if (mediaFile != null) {
            return mediaFile;
        } else {
            throw new FileNotFoundException(id);
        }
    }

    @PutMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('user', 'admin')")
    public MediaFile updateMetadata(@PathVariable("id") String id, @NotNull @RequestBody MediaFile mediaFile){
        setUserInfo(mediaFile);
        return fileService.updateMediaFile(id, mediaFile, null);
    }

    private void setUserInfo(MediaFile mediaFile) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        if (userDetails != null) mediaFile.setModifiedBy(userDetails.getUsername());
        mediaFile.setModificationDate(LocalDateTime.now());
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('user', 'admin')")
    public void deleteMetadata(@PathVariable("id") String id) {
        fileService.trashMediaFile(id);
    }

}
