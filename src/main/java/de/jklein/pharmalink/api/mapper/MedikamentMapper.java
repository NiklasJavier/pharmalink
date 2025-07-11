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

    MedikamentResponseDto toDto(Medikament medikament);

    default List<MedikamentResponseDto> toDtoList(List<Medikament> medikamente) {
        if (medikamente == null) {
            return null;
        }
        return medikamente.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    Medikament toEntity(MedikamentResponseDto dto);

    default List<Medikament> toEntityList(List<MedikamentResponseDto> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}