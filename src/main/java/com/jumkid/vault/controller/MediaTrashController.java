package com.jumkid.vault.controller;

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.service.MediaFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/trash")
public class MediaTrashController {

    private final MediaFileService fileService;

    @Autowired
    public MediaTrashController(MediaFileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<MediaFile> getAll(){
        return fileService.getTrash();
    }

}
