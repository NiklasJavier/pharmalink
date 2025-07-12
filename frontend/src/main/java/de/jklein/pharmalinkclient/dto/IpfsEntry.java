package de.jklein.pharmalinkclient.dto;

public class IpfsEntry {
    private String key;
    private String value;

    public IpfsEntry() {
    }

    public IpfsEntry(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}