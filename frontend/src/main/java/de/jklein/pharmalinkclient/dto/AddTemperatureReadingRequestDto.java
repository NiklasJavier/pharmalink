package de.jklein.pharmalinkclient.dto; 

import jakarta.validation.constraints.NotBlank;

public class AddTemperatureReadingRequestDto {
    @NotBlank(message = "Temperature darf nicht leer sein.")
    private String temperature;

    @NotBlank(message = "Timestamp darf nicht leer sein.")
    private String timestamp;

    public AddTemperatureReadingRequestDto() {
    }

    public AddTemperatureReadingRequestDto(String temperature, String timestamp) {
        this.temperature = temperature;
        this.timestamp = timestamp;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}