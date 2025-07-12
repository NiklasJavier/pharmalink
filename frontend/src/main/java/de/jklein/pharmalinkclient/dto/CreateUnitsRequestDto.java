package de.jklein.pharmalinkclient.dto; 
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class CreateUnitsRequestDto {
    @NotBlank(message = "Die Chargenbezeichnung darf nicht leer sein.")
    private String chargeBezeichnung;

    @Min(value = 1, message = "Die Anzahl muss mindestens 1 sein.")
    private int anzahl;

    private Map<String, Object> ipfsData;

    public CreateUnitsRequestDto() {
    }

    public CreateUnitsRequestDto(String chargeBezeichnung, int anzahl, Map<String, Object> ipfsData) {
        this.chargeBezeichnung = chargeBezeichnung;
        this.anzahl = anzahl;
        this.ipfsData = ipfsData;
    }

    public String getChargeBezeichnung() {
        return chargeBezeichnung;
    }

    public void setChargeBezeichnung(String chargeBezeichnung) {
        this.chargeBezeichnung = chargeBezeichnung;
    }

    public int getAnzahl() {
        return anzahl;
    }

    public void setAnzahl(int anzahl) {
        this.anzahl = anzahl;
    }

    public Map<String, Object> getIpfsData() {
        return ipfsData;
    }

    public void setIpfsData(Map<String, Object> ipfsData) {
        this.ipfsData = ipfsData;
    }
}