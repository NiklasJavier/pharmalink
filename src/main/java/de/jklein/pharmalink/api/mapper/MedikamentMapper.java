package de.jklein.pharmalink.api.mapper;

import de.jklein.pharmalink.api.dto.MedikamentResponseDto;
import de.jklein.pharmalink.domain.Medikament;
import org.mapstruct.Mapper;

/**
 * MapStruct-Mapper zur Umwandlung zwischen Medikament-Domain-Objekten und DTOs.
 *
 * Die Annotation @Mapper(componentModel = "spring") sorgt dafür, dass MapStruct
 * eine Implementierung dieser Schnittstelle als Spring Bean generiert, die dann
 * per Dependency Injection verwendet werden kann.
 */
@Mapper(componentModel = "spring")
public interface MedikamentMapper {

    /**
     * Wandelt ein Medikament-Domain-Objekt in ein MedikamentResponseDto um.
     * Da die Feldnamen übereinstimmen, erledigt MapStruct die Zuweisung automatisch.
     *
     * @param medikament Das Domain-Objekt aus der Service-Schicht.
     * @return Das entsprechende DTO für die API-Antwort.
     */
    MedikamentResponseDto toDto(Medikament medikament);

}