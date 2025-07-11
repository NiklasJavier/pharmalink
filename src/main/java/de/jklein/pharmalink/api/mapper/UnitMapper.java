package de.jklein.pharmalink.api.mapper;

import de.jklein.pharmalink.api.dto.UnitResponseDto;
import de.jklein.pharmalink.domain.Unit;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UnitMapper {

    UnitResponseDto toDto(Unit unit);

    List<UnitResponseDto> toDtoList(List<Unit> units);

    Unit toEntity(UnitResponseDto dto);

    List<Unit> toEntityList(List<UnitResponseDto> dtos);

    default Map<String, String> transferEntryToMap(Unit.TransferEntry entry) {
        if (entry == null) return null;
        return Map.of(
                "fromActorId", entry.getFromActorId(),
                "toActorId", entry.getToActorId(),
                "timestamp", entry.getTimestamp()
        );
    }

    default Map<String, String> temperatureReadingToMap(Unit.TemperatureReading reading) {
        if (reading == null) return null;
        return Map.of(
                "timestamp", reading.getTimestamp(),
                "temperature", reading.getTemperature()
        );
    }

    default List<Map<String, String>> mapTransferHistoryToDto(List<Unit.TransferEntry> entries) {
        if (entries == null) return Collections.emptyList();
        return entries.stream().map(this::transferEntryToMap).collect(Collectors.toList());
    }

    default List<Map<String, String>> mapTemperatureReadingsToDto(List<Unit.TemperatureReading> readings) {
        if (readings == null) return Collections.emptyList();
        return readings.stream().map(this::temperatureReadingToMap).collect(Collectors.toList());
    }

    default Unit.TransferEntry mapToTransferEntry(Map<String, String> map) {
        if (map == null) return null;
        Unit.TransferEntry entry = new Unit.TransferEntry();
        entry.setFromActorId(map.get("fromActorId"));
        entry.setToActorId(map.get("toActorId"));
        entry.setTimestamp(map.get("timestamp"));
        return entry;
    }

    default Unit.TemperatureReading mapToTemperatureReading(Map<String, String> map) {
        if (map == null) return null;
        Unit.TemperatureReading reading = new Unit.TemperatureReading();
        reading.setTimestamp(map.get("timestamp"));
        reading.setTemperature(map.get("temperature"));
        return reading;
    }

    default List<Unit.TransferEntry> mapToTransferEntryList(List<Map<String, String>> list) {
        if (list == null) return Collections.emptyList();
        return list.stream().map(this::mapToTransferEntry).collect(Collectors.toList());
    }

    default List<Unit.TemperatureReading> mapToTemperatureReadingList(List<Map<String, String>> list) {
        if (list == null) return Collections.emptyList();
        return list.stream().map(this::mapToTemperatureReading).collect(Collectors.toList());
    }
}