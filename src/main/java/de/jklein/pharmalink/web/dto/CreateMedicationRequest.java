package de.jklein.pharmalink.web.dto;

// Ein Record ist ideal für ein DTO, das nur Daten transportiert.
public record CreateMedicationRequest(
        String name,
        String dosage,
        String manufacturer,
        String owner,
        double appraisedValue
) {}