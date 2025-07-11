package de.jklein.pharmalink.api.mapper;

import de.jklein.pharmalink.api.dto.ActorResponseDto;
import de.jklein.pharmalink.domain.Actor;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ActorMapper {

    ActorResponseDto toDto(Actor actor);

    Actor toEntity(ActorResponseDto dto);

    default List<ActorResponseDto> toDtoList(List<Actor> actors) {
        if (actors == null) {
            return null;
        }
        return actors.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    default List<Actor> toEntityList(List<ActorResponseDto> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}