package de.jklein.pharmalink.api.mapper;

import de.jklein.pharmalink.api.dto.UnitResponseDto;
import de.jklein.pharmalink.domain.Unit;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct-Mapper zur Umwandlung zwischen Unit-Domain-Objekten und DTOs.
 */
@Mapper(componentModel = "spring")
public interface UnitMapper {

    /**
     * Wandelt ein Unit-Domain-Objekt in ein UnitResponseDto um.
     *
     * @param unit Das Domain-Objekt.
     * @return Das entsprechende DTO.
     */
    UnitResponseDto toDto(Unit unit);

    /**
     * Wandelt eine Liste von Unit-Domain-Objekten in eine Liste von UnitResponseDtos um.
     *
     * @param units Die Liste der Domain-Objekte.
     * @return Die Liste der entsprechenden DTOs.
     */
    List<UnitResponseDto> toDtoList(List<Unit> units);
}