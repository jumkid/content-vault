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

import com.jumkid.share.security.AccessScope;
import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.controller.dto.MediaFileProp;
import com.jumkid.vault.enums.MediaFileModule;
import com.jumkid.vault.enums.ThumbnailNamespace;
import com.jumkid.vault.exception.FileNotAvailableException;
import com.jumkid.vault.exception.FileNotFoundException;
import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.service.MediaFileService;
import com.jumkid.vault.util.ResponseMediaFileWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Optional;

import static com.jumkid.vault.util.Constants.PROP_FEATURED_ID;

@Slf4j
@RestController
@RequestMapping("/content")
public class MediaContentController {

    private final MediaFileService fileService;

    private final ResponseMediaFileWriter responseMFileWriter;

    private final ThumbnailAPIHelper thumbnailAPIHelper;

    @Autowired
    public MediaContentController(MediaFileService fileService, ResponseMediaFileWriter responseMFileWriter, ThumbnailAPIHelper thumbnailAPIHelper) {
        this.fileService = fileService;
        this.responseMFileWriter = responseMFileWriter;
        this.thumbnailAPIHelper = thumbnailAPIHelper;
    }

    @GetMapping(value = "{id}", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('ADMIN_ROLE')" +
            " || @securityService.isPublic(#mediaFileId)" +
            " || @securityService.isOwner(authentication, #mediaFileId)")
    public String getPlainContent(@PathVariable("id") String mediaFileId,
                                  @RequestParam(required = false) Boolean ignoreTitle)
            throws FileNotAvailableException, FileStoreServiceException, FileNotFoundException {
        return getContent(mediaFileId, ignoreTitle);
    }

    private String getContent(String mediaFileId, Boolean ignoreTitle)
            throws FileNotAvailableException, FileStoreServiceException, FileNotFoundException {
        MediaFile mediaFile = fileService.getMediaFile(mediaFileId);
        StringBuilder sb = new StringBuilder();
        boolean addedTitle = false;
        String title = mediaFile.getTitle();
        if (title != null && (ignoreTitle == null || !ignoreTitle)){
            sb.append(mediaFile.getTitle());
            addedTitle = true;
        }
        String content = mediaFile.getContent();
        if (content != null && !content.isBlank()) {
            if (addedTitle) sb.append("\n");
            sb.append(mediaFile.getContent());
        }

        return sb.toString();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('USER_ROLE', 'ADMIN_ROLE')")
    public MediaFile addTextContent(@RequestParam(required = false) String title,
                                    @RequestParam AccessScope accessScope,
                                    @RequestBody @NotBlank String content) throws FileStoreServiceException {
        MediaFile mediaFile = MediaFile.builder()
                .accessScope(accessScope)
                .title(title).content(content)
                .size((title != null ? title.length() : 0) + (content != null ? content.length() : 0))
                .mimeType(MediaType.TEXT_PLAIN_VALUE)
                .build();
        return fileService.addMediaFile(mediaFile, MediaFileModule.TEXT);
    }

    @PostMapping("/html")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('USER_ROLE', 'ADMIN_ROLE')")
    public MediaFile addHtmlContent(@RequestParam(required = false) String title,
                                    @NotBlank @RequestBody String content,
                                    @RequestParam AccessScope accessScope) throws FileStoreServiceException {
        MediaFile mediaFile = MediaFile.builder()
                .accessScope(accessScope)
                .title(title)
                .content(content)
                .size((title != null ? title.length() : 0) + (content != null ? content.length() : 0))
                .mimeType(MediaType.TEXT_HTML_VALUE)
                .build();
        return fileService.addMediaFile(mediaFile, MediaFileModule.HTML);
    }

    @GetMapping(value="/stream/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('ADMIN_ROLE')" +
            " || @securityService.isPublic(#mediaFileId)" +
            " || @securityService.isOwner(authentication, #mediaFileId)")
    public void stream(@PathVariable("id") String mediaFileId,
                       HttpServletRequest request, HttpServletResponse response)
            throws FileNotAvailableException, FileStoreServiceException, FileNotFoundException {
        MediaFile mediaFile = fileService.getMediaFile(mediaFileId);
        String mimeType = mediaFile.getMimeType();

        if(mimeType != null && (mimeType.startsWith("audio") || mimeType.startsWith("video"))){
            log.debug("stream media content");

            try (FileChannel fc = fileService.getFileChannel(mediaFileId)) {
                if (fc != null) {
                    response = responseMFileWriter.stream(mediaFile, fc, request, response);
                } else {
                    log.error("File is blank. There is nothing to stream");
                    throw new FileNotFoundException(mediaFileId);
                }
            } catch (IOException | FileNotFoundException ex) {
                log.error("failed to stream file resource {}", ex.getMessage());
            } finally {
                try{
                    response.flushBuffer();
                } catch (Exception e) {
                    log.error("fatal response issue {}", e.getMessage());
                }
            }

        } else {
            Optional<byte[]> optional = fileService.getFileSource(mediaFileId);
            if (optional.isPresent()) {
                responseMFileWriter.write(mediaFile, optional.get(), response);
                return;
            } else {
                log.info("File is blank. Streams the content in metadata instead.");
                responseMFileWriter.write(mediaFile, response);
            }
        }

        throw new FileStoreServiceException(mediaFileId);
    }

    @GetMapping(value="/thumbnail/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('ADMIN_ROLE')" +
            " || @securityService.isPublic(#mediaFileId)" +
            " || @securityService.isOwner(authentication, #mediaFileId)")
    public void thumbnail(@PathVariable("id") String mediaFileId,
                          @RequestParam(value = "size", required = false) ThumbnailNamespace thumbnailNamespace,
                          HttpServletResponse response)
            throws FileNotAvailableException, FileNotFoundException, FileStoreServiceException {
        final ThumbnailNamespace size = thumbnailNamespace == null ? ThumbnailNamespace.SMALL : thumbnailNamespace;

        MediaFile mediaFile = fileService.getMediaFile(mediaFileId);

        String targetMediaFileId = mediaFileId;
        if (mediaFile.getModule().equals(MediaFileModule.GALLERY)
                && mediaFile.getProps() != null && mediaFile.getChildren()!= null) {
            //get feature image as thumbnail
            MediaFileProp featuredIdProp = mediaFile.getProps()
                    .stream()
                    .filter(prop -> prop.getName().equals(PROP_FEATURED_ID))
                    .findFirst()
                    .orElseGet(() -> null);
            targetMediaFileId = featuredIdProp != null ? featuredIdProp.getTextValue() : mediaFileId;
        }

        thumbnailAPIHelper.response(targetMediaFileId, size, response);
    }

}
