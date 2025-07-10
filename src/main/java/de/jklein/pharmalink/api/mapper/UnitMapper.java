package de.jklein.pharmalink.api.mapper;

import de.jklein.pharmalink.api.dto.UnitResponseDto;
import de.jklein.pharmalink.domain.Unit;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MapStruct-Mapper zur Umwandlung zwischen Unit-Domain-Objekten und DTOs.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UnitMapper {

    // Die Annotation @Mapping(target = "ipfsData", ignore = true) wurde entfernt.
    // MapStruct wird das Feld "ipfsData" jetzt automatisch mappen.
    UnitResponseDto toDto(Unit unit);

    /**
     * Wandelt eine Liste von Unit-Domain-Objekten in eine Liste von UnitResponseDtos um.
     *
     * @param units Die Liste der Domain-Objekte.
     * @return Die Liste der entsprechenden DTOs.
     */
    default List<UnitResponseDto> toDtoList(List<Unit> units) {
        if (units == null) {
            return null;
        }
        return units.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Wandelt ein UnitResponseDto in ein Unit-Domain-Objekt um.
     * MapStruct versucht, alle passenden Felder zu mappen.
     *
     * @param dto Das DTO.
     * @return Das entsprechende Domain-Objekt.
     */
    Unit toEntity(UnitResponseDto dto);

    /**
     * NEU: Wandelt eine Liste von UnitResponseDtos in eine Liste von Unit-Domain-Objekten um.
     *
     * @param dtos Die Liste der DTOs.
     * @return Die Liste der entsprechenden Domain-Objekte.
     */
    default List<Unit> toEntityList(List<UnitResponseDto> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}