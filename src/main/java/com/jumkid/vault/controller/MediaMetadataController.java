package com.jumkid.vault.controller;

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.exception.FileNotfoundException;
import com.jumkid.vault.service.MediaFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
    public List<MediaFile> getAll(){
        return fileService.getAll();
    }

    @GetMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    public MediaFile getMetadata(@PathVariable("id") String id){
        MediaFile mediaFile = fileService.getMediaFile(id);
        if (mediaFile != null) {
            return mediaFile;
        } else {
            throw new FileNotfoundException(id);
        }
    }

    @PutMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    public MediaFile updateMetadata(@PathVariable("id") String id, @NotNull @RequestBody MediaFile mediaFile){
        return fileService.updateMediaFile(id, mediaFile, null);
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMetadata(@PathVariable("id") String id) {
        fileService.deleteMediaFile(id);
    }

}
