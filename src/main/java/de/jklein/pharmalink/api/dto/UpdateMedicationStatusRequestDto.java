package de.jklein.pharmalink.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateMedicationStatusRequestDto {

    @NotBlank(message = "Der neue Status darf nicht leer sein.")
    @Pattern(regexp = "freigegeben|abgelehnt", message = "Status muss 'freigegeben' oder 'abgelehnt' sein.")
    private String newStatus;
}