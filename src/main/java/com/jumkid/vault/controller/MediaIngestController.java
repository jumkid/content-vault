package com.jumkid.vault.controller;

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.service.MediaIngestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/ingest")
public class MediaIngestController {

    private final MediaIngestService mediaIngestService;

    @Autowired
    public MediaIngestController(MediaIngestService mediaIngestService) {
        this.mediaIngestService = mediaIngestService;
    }

    @PostMapping("webPage")
    public MediaFile convertWebPage(@RequestParam String url, @RequestParam(required = false) String title) {
        return mediaIngestService.ingestUrlToImage(title, url);
    }

}

