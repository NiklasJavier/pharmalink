package de.jklein.pharmalink.api.mapper;

import de.jklein.pharmalink.api.dto.UnitResponseDto;
import de.jklein.pharmalink.domain.Unit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.stream.Collectors; // Import für Collectors hinzufügen

/**
 * MapStruct-Mapper zur Umwandlung zwischen Unit-Domain-Objekten und DTOs.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE) // ReportingPolicy.IGNORE ist gut, wenn nicht alle Felder gemappt werden sollen
public interface UnitMapper {

    // NEU: isConsumed und consumedRefId werden direkt gemappt, wenn Feldnamen übereinstimmen.
    // ipfsData wird im Service befüllt, daher ignorieren wir es hier beim Mapping von Domain zu DTO.
    @Mapping(target = "ipfsData", ignore = true)
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