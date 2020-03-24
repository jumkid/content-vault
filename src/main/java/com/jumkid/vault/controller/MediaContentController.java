package com.jumkid.vault.controller;

/*
 * This software is written by Jumkid and subject
 * to a contract between Jumkid and its customer.
 *
 * This software stays property of Jumkid unless differing
 * arrangements between Jumkid and its customer apply.
 *
 *
 * (c)2019 Jumkid Innovation All rights reserved.
 */

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.exception.FileNotfoundException;
import com.jumkid.vault.service.MediaFileService;
import com.jumkid.vault.util.ResponseMediaFileWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import java.nio.channels.FileChannel;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/content")
public class MediaContentController {

    private final MediaFileService fileService;

    private final ResponseMediaFileWriter responseMFileWriter;

    @Autowired
    public MediaContentController(MediaFileService fileService, ResponseMediaFileWriter responseMFileWriter) {
        this.fileService = fileService;
        this.responseMFileWriter = responseMFileWriter;
    }

    @GetMapping("/plain/{id}")
    @ResponseStatus(HttpStatus.OK)
    public String getPlain(@PathVariable("id") String id, @RequestParam(required = false) Boolean ignoreTitle,
                           @RequestParam(required = false) Boolean ignoreContent){
        MediaFile mediaFile = fileService.getMediaFile(id);
        StringBuilder sb = new StringBuilder();
        boolean addedTitle = false;
        if (mediaFile != null) {
            if (ignoreTitle == null || !ignoreTitle){
                sb.append(mediaFile.getTitle());
                addedTitle = true;
            }
            if (ignoreContent == null || !ignoreContent) {
                if (addedTitle) sb.append("\n\n");
                sb.append(mediaFile.getContent());
            }

            return sb.toString();
        } else {
            throw new FileNotfoundException(id);
        }
    }

    @PostMapping("/plain")
    @ResponseStatus(HttpStatus.OK)
    public MediaFile addPlain(@NotBlank @RequestParam String title, @RequestParam(required = false) String content) {
        MediaFile mediaFile = MediaFile.builder()
                .title(title).content(content)
                .size(title.length() + (content == null ? 0 : content.length()))
                .mimeType(MediaType.TEXT_PLAIN_VALUE)
                .build();
        return fileService.addMediaFile(mediaFile, null);
    }

    @GetMapping(value = "/html/{id}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public String getHtml(@PathVariable("id") String id) {
        Optional<byte[]> optional = fileService.getFileSource(id);
        if (optional.isPresent()) {
            return new String(optional.get());
        } else {
            throw new FileNotfoundException(id);
        }
    }

    @GetMapping(value="/stream/{id}")
    public void stream(@PathVariable("id") String id, HttpServletRequest request, HttpServletResponse response){
        try {
            MediaFile mediaFile = fileService.getMediaFile(id);

            if(mediaFile.getMimeType().startsWith("audio") || mediaFile.getMimeType().startsWith("video")){
                log.debug("stream media content");
                try (FileChannel fc = fileService.getFileChannel(id)) {
                    if (fc != null) {
                        response = responseMFileWriter.stream(mediaFile, fc, request, response);
                    } else {
                        log.error("File channel is blank. There is nothing to stream");
                        throw new FileNotfoundException(id);
                    }
                }

            } else {
                Optional<byte[]> optional = fileService.getFileSource(id);
                if (optional.isPresent()) {
                    response = responseMFileWriter.write(mediaFile, optional.get(), response);
                } else {
                    log.error("File channel is blank. There is nothing to stream");
                    throw new FileNotfoundException(id);
                }
            }
        } catch (Exception e) {
            log.error("failed to stream file resource {}", e.getMessage());
        } finally {
            try{
                response.flushBuffer();
            }catch(Exception e){
                log.error("failed to get file resource {}", e.getMessage());
            }
        }

    }

}
