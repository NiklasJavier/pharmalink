package de.jklein.pharmalinkclient.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginResponse {

    private final String jwt;

    @JsonCreator
    public LoginResponse(@JsonProperty("jwt") String jwt) {
        this.jwt = jwt;
    }

    public String getJwt() {
        return jwt;
    }
}