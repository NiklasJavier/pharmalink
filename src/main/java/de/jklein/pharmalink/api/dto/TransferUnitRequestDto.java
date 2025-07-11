package de.jklein.pharmalink.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TransferUnitRequestDto {

    @NotBlank(message = "Die ID des neuen Besitzers (newOwnerActorId) darf nicht leer sein.")
    private String newOwnerActorId;
}