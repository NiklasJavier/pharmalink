package de.jklein.pharmalinkclient.dto;

public class MedikamentFilterCriteriaDto {
    private String searchTerm;
    private String statusFilter;
    private boolean filterByCurrentActor;

    public MedikamentFilterCriteriaDto(String searchTerm, String statusFilter) {
        this.searchTerm = searchTerm;
        this.statusFilter = statusFilter;
        this.filterByCurrentActor = false;
    }

    public MedikamentFilterCriteriaDto(String searchTerm, String statusFilter, boolean filterByCurrentActor) {
        this.searchTerm = searchTerm;
        this.statusFilter = statusFilter;
        this.filterByCurrentActor = filterByCurrentActor;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public String getStatusFilter() {
        return statusFilter;
    }

    public boolean isFilterByCurrentActor() {
        return filterByCurrentActor;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
    }

    public void setFilterByCurrentActor(boolean filterByCurrentActor) {
        this.filterByCurrentActor = filterByCurrentActor;
    }

    @Override
    public String toString() {
        return "MedikamentFilterCriteriaDto{" +
                "searchTerm='" + searchTerm + '\'' +
                ", statusFilter='" + statusFilter + '\'' +
                ", filterByCurrentActor=" + filterByCurrentActor +
                '}';
    }
}