package de.jklein.pharmalink.api.mapper;

import de.jklein.pharmalink.api.dto.ActorResponseDto;
import de.jklein.pharmalink.domain.Actor;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ActorMapper {

    // Die Annotation @Mapping(target = "ipfsData", ignore = true) wurde entfernt.
    // MapStruct wird das Feld "ipfsData" jetzt automatisch mappen.
    ActorResponseDto toDto(Actor actor);

    // Wandelt ein ActorResponseDto in ein Actor-Domain-Objekt um.
    Actor toEntity(ActorResponseDto dto);

    // Methode zur Umwandlung einer Liste von Actor-Domain-Objekten in eine Liste von ActorResponseDto
    default List<ActorResponseDto> toDtoList(List<Actor> actors) {
        if (actors == null) {
            return null;
        }
        return actors.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Methode zur Umwandlung einer Liste von ActorResponseDto in eine Liste von Actor-Domain-Objekten
    default List<Actor> toEntityList(List<ActorResponseDto> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}