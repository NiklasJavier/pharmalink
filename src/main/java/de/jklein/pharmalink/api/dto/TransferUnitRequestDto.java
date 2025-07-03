package de.jklein.pharmalink.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO für die Anfrage zum Transfer einer Unit an einen neuen Besitzer.
 */
@Data
public class TransferUnitRequestDto {

    @NotBlank(message = "Die ID des neuen Besitzers (newOwnerActorId) darf nicht leer sein.")
    private String newOwnerActorId;
}