package de.jklein.pharmalinkclient.dto;

import jakarta.validation.constraints.NotBlank;

public class TransferUnitRequestDto {

    @NotBlank(message = "newOwnerActorId darf nicht leer sein.")
    private String newOwnerActorId;
    
    public TransferUnitRequestDto() {
    }

    public TransferUnitRequestDto(String newOwnerActorId) {
        this.newOwnerActorId = newOwnerActorId;
    }

    public String getNewOwnerActorId() {
        return newOwnerActorId;
    }

    public void setNewOwnerActorId(String newOwnerActorId) {
        this.newOwnerActorId = newOwnerActorId;
    }
}