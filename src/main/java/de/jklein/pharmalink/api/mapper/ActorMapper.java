package de.jklein.pharmalink.api.mapper;

import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.api.dto.ActorResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ActorMapper {
    ActorResponseDto toDto(Actor actor);
}