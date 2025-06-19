package de.jklein.pharmalink.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetadataDto {
    private String s3Url;
    private String fileName;
    private String originalIpfsCid;
}