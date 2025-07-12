package de.jklein.pharmalinkclient.dto; 

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;

public class UpdateMedicationStatusRequestDto {
    @NotBlank(message = "Der Status darf nicht leer sein.")
    @Pattern(regexp = "freigegeben|abgelehnt", message = "Der Status muss 'freigegeben' oder 'abgelehnt' sein.")
    private String newStatus;

    public UpdateMedicationStatusRequestDto() {
    }

    public UpdateMedicationStatusRequestDto(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }
}