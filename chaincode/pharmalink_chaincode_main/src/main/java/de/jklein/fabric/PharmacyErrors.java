package de.jklein.fabric;

/**
 * Definiert die standardisierten Fehler-Strings für den PharmacyChaincode.
 * Jeder Fehler hat einen eindeutigen Code, der von den Client-Anwendungen
 * interpretiert werden kann.
 */
public enum PharmacyErrors {
    PERMISSION_DENIED("PHARMACY_PERMISSION_DENIED"),
    ACTOR_NOT_FOUND("PHARMACY_ACTOR_NOT_FOUND"),
    ACTOR_ALREADY_EXISTS("PHARMACY_ACTOR_ALREADY_EXISTS"),
    ACTOR_NOT_APPROVED("PHARMACY_ACTOR_NOT_APPROVED"),
    DRUG_INFO_NOT_FOUND("PHARMACY_DRUG_INFO_NOT_FOUND"),
    DRUG_UNIT_NOT_FOUND("PHARMACY_DRUG_UNIT_NOT_FOUND"),
    DRUG_UNIT_NOT_OWNED("PHARMACY_DRUG_UNIT_NOT_OWNED"),
    INVALID_ACTOR_STATUS("PHARMACY_INVALID_ACTOR_STATUS"),
    INVALID_ARGUMENT("PHARMACY_INVALID_ARGUMENT"),
    RICH_QUERY_FAILED("PHARMACY_RICH_QUERY_FAILED");

    private final String code;

    PharmacyErrors(final String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return this.code;
    }
}
