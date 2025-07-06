package de.jklein.pharmalink.api.mapper;

import de.jklein.pharmalink.api.dto.MedikamentResponseDto;
import de.jklein.pharmalink.domain.Medikament;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping; // Import für @Mapping hinzufügen
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MedikamentMapper {

    // NEU: ipfsData wird nicht mehr ignoriert, da es jetzt im Domain-Objekt ist
    // MapStruct mappt es automatisch, wenn die Feldnamen übereinstimmen.
    MedikamentResponseDto toDto(Medikament medikament);

    // Methode zur Umwandlung einer Liste von Medikament in eine Liste von MedikamentResponseDto
    default List<MedikamentResponseDto> toDtoList(List<Medikament> medikamente) {
        if (medikamente == null) {
            return null;
        }
        return medikamente.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Methode zur Umwandlung eines MedikamentResponseDto in ein Medikament-Objekt
    Medikament toEntity(MedikamentResponseDto dto);

    // Methode zur Umwandlung einer Liste von MedikamentResponseDto in eine Liste von Medikament
    default List<Medikament> toEntityList(List<MedikamentResponseDto> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}