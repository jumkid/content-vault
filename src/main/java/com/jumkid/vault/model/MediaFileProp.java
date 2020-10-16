package com.jumkid.vault.model;

import com.jumkid.vault.enums.MediaFilePropType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Data
@Builder
public class MediaFileProp {

    public String name;

    public String value;

    public MediaFilePropType dataType;

}
