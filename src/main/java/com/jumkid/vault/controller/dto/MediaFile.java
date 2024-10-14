package com.jumkid.vault.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.*;
import com.jumkid.share.security.AccessScope;
import com.jumkid.share.service.dto.GenericDTO;
import com.jumkid.vault.enums.MediaFileModule;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import lombok.experimental.SuperBuilder;

import java.util.List;


@SuperBuilder
@Data @NoArgsConstructor @EqualsAndHashCode(of = {"uuid", "title"}, callSuper = false)
@JsonInclude(Include.NON_NULL)
public class MediaFile extends GenericDTO {

    private String uuid;

    private String filename;

    private String mimeType;

    private Integer size;

    @NotBlank
    private String title;

    private String content;

    private Boolean activated;

    private MediaFileModule module;

    private AccessScope accessScope;

    @JsonIgnore
    private byte[] file;

    private List<String> tags;

    private List<MediaFile> children;

    private List<MediaFileProp> props;
}
