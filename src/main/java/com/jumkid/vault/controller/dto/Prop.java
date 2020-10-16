package com.jumkid.vault.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jumkid.vault.enums.MediaFilePropType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Prop {

    public String name;

    public String value;

    @JsonProperty("type")
    public MediaFilePropType dataType;
}
