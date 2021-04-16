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
import com.jumkid.vault.enums.ThumbnailNamespace;
import com.jumkid.vault.exception.FileNotFoundException;
import com.jumkid.vault.service.MediaFileService;
import com.jumkid.vault.util.ResponseMediaFileWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public MediaFile addContent(@NotNull @Valid @RequestBody MediaFile mediaFile){
        setCreationInfo(mediaFile);
        return fileService.addMediaFile(mediaFile, null);
    }

    @GetMapping("/plain/{id}")
    @ResponseStatus(HttpStatus.OK)
    public String getPlain(@PathVariable("id") String id, @RequestParam(required = false) Boolean ignoreTitle,
                           @RequestParam(required = false) Boolean ignoreContent){
        MediaFile mediaFile = fileService.getMediaFile(id);
        StringBuilder sb = new StringBuilder();
        boolean addedTitle = false;
        if (ignoreTitle == null || !ignoreTitle){
            sb.append(mediaFile.getTitle());
            addedTitle = true;
        }
        if (ignoreContent == null || !ignoreContent) {
            if (addedTitle) sb.append("\n\n");
            sb.append(mediaFile.getContent());
        }

        return sb.toString();
    }

    @PostMapping("/plain")
    @ResponseStatus(HttpStatus.OK)
    public MediaFile addPlain(@NotBlank @RequestParam String title, @RequestParam(required = false) String content) {
        MediaFile mediaFile = MediaFile.builder()
                .title(title).content(content)
                .size(title.length() + (content == null ? 0 : content.length()))
                .mimeType(MediaType.TEXT_PLAIN_VALUE)
                .build();
        setCreationInfo(mediaFile);
        return fileService.addMediaFile(mediaFile, null);
    }

    @GetMapping(value = "/html/{id}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public String getHtml(@PathVariable("id") String id) {
        Optional<byte[]> optional = fileService.getFileSource(id);
        if (optional.isPresent()) {
            return new String(optional.get());
        } else {
            throw new FileNotFoundException(id);
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
                        throw new FileNotFoundException(id);
                    }
                }

            } else {
                Optional<byte[]> optional = fileService.getFileSource(id);
                if (optional.isPresent()) {
                    response = responseMFileWriter.write(mediaFile, optional.get(), response);
                } else {
                    log.error("File channel is blank. There is nothing to stream");
                    throw new FileNotFoundException(id);
                }
            }
        } catch (Exception e) {
            log.error("failed to stream file resource {}", e.getMessage());
        } finally {
            try{
                response.flushBuffer();
            } catch (Exception e) {
                log.error("failed to get file resource {}", e.getMessage());
            }
        }

    }

    @GetMapping(value="/thumbnail/{id}")
    public void thumbnail(@PathVariable("id") String id,
                          @RequestParam(value = "size", required = false) ThumbnailNamespace thumbnailNamespace,
                          HttpServletResponse response){
        if (thumbnailNamespace == null) thumbnailNamespace = ThumbnailNamespace.MEDIUM;
        Optional<byte[]> optional = fileService.getThumbnail(id, thumbnailNamespace);
        if (optional.isPresent()) {
            MediaFile mediaFile = MediaFile.builder()
                                            .uuid(id)
                                            .mimeType("image/png")
                                            .build();
            responseMFileWriter.write(mediaFile, optional.get(), response);
        } else {
            log.warn("File thumbnail {} is unavailable", id);
            throw new FileNotFoundException(id);
        }
    }

    private void setCreationInfo(MediaFile mediaFile) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            mediaFile.setCreatedBy(userDetails.getUsername());
        } else {
            mediaFile.setCreatedBy(auth.getPrincipal().toString());
        }
    }

}
