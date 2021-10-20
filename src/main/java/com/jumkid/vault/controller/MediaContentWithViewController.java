package com.jumkid.vault.controller;

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.enums.ThumbnailNamespace;
import com.jumkid.vault.exception.FileNotFoundException;
import com.jumkid.vault.service.MediaFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
public class MediaContentWithViewController {

    private final MediaFileService fileService;

    @Autowired
    public MediaContentWithViewController(MediaFileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping(value = "/html/{id}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public String getHtml(Map<String, Object> model, @PathVariable("id") String mediaFileId,
                          @RequestParam(required = false) Boolean ignoreTitle) {
        MediaFile mediaFile = fileService.getMediaFile(mediaFileId);
        model.put("mediafile", mediaFile);

        return "content-html";
    }

    @GetMapping(value = "/content-thumbnail/{id}", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public byte[] thumbnail(@PathVariable("id") String mediaFileId,
                          @RequestParam(value = "size", required = false) ThumbnailNamespace thumbnailNamespace){
        if (thumbnailNamespace == null) thumbnailNamespace = ThumbnailNamespace.MEDIUM;
        Optional<byte[]> optional = fileService.getThumbnail(mediaFileId, thumbnailNamespace);
        if (optional.isPresent()) {
            return optional.get();
        } else {
            log.warn("File thumbnail {} is unavailable", mediaFileId);
            throw new FileNotFoundException(mediaFileId);
        }
    }

}
