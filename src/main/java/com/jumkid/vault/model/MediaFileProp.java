package com.jumkid.vault.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class MediaFileProp {

    private String name;

    private String textValue;

    private LocalDateTime dateValue;

    private Integer numberValue;

}
