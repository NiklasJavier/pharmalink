package de.jklein.pharmalink.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Map;

@Data
public class CreateMedikamentRequestDto {

    @NotBlank(message = "Die Bezeichnung darf nicht leer sein.")
    private String bezeichnung;

    private String infoblattHash;

    private Map<String, Object> ipfsData;
}