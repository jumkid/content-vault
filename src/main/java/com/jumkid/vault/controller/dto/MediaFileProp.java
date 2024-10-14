package com.jumkid.vault.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder @Data @NoArgsConstructor @AllArgsConstructor
public class MediaFileProp implements Serializable {

    private String name;

    private String textValue;
    private LocalDateTime dateValue;
    private Number numberValue;

}
