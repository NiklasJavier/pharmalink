package de.jklein.pharmalink.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddTemperatureReadingRequestDto {

    @NotBlank(message = "Der Temperaturwert darf nicht leer sein.")
    private String temperature;

    @NotBlank(message = "Der Zeitstempel darf nicht leer sein.")
    private String timestamp;
}