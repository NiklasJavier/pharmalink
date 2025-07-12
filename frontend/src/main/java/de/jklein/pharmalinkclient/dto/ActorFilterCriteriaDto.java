package de.jklein.pharmalinkclient.dto;

public class ActorFilterCriteriaDto {
    private String searchTerm;

    public ActorFilterCriteriaDto(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }
}