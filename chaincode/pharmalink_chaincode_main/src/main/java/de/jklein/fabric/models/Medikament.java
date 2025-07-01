package de.jklein.fabric.models;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@DataType()
public final class Medikament {

    @Property()
    private String medId;

    @Property()
    private String herstellerId;

    @Property()
    private String bezeichnung;

    @Property()
    private String infoblattHash;

    @Property()
    private String ipfsLink;

    @Property()
    private String status;

    @Property()
    private Map<String, String> tags;

    @Property()
    private String docType;

    @Property()
    private String approvedById;

    public Medikament() {
        this.status = "angelegt";
        this.tags = new TreeMap<>();
        this.docType = "medikament";
    }

    public Medikament(@JsonProperty("medId") final String medId,
                      @JsonProperty("herstellerId") final String herstellerId,
                      @JsonProperty("bezeichnung") final String bezeichnung,
                      @JsonProperty("ipfsLink") final String ipfsLink) {
        this.medId = medId;
        this.herstellerId = herstellerId;
        this.bezeichnung = bezeichnung;
        this.ipfsLink = ipfsLink;
        this.status = "angelegt";
        this.tags = new TreeMap<>();
        this.docType = "medikament";
    }

    public String getMedId() {
        return medId;
    }

    public void setMedId(final String newMedId) {
        this.medId = newMedId;
    }

    public String getHerstellerId() {
        return herstellerId;
    }

    public void setHerstellerId(final String newHerstellerId) {
        this.herstellerId = newHerstellerId;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    public void setBezeichnung(final String newBezeichnung) {
        this.bezeichnung = newBezeichnung;
    }

    public String getInfoblattHash() {
        return infoblattHash;
    }

    public void setInfoblattHash(final String newInfoblattHash) {
        this.infoblattHash = newInfoblattHash;
    }

    public String getIpfsLink() {
        return ipfsLink;
    }

    public void setIpfsLink(final String newIpfsLink) {
        this.ipfsLink = newIpfsLink;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String newStatus) {
        this.status = newStatus;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(final Map<String, String> newTags) {
        this.tags = newTags;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(final String newDocType) {
        this.docType = newDocType;
    }

    public String getApprovedById() {
        return approvedById;
    }

    public void setApprovedById(final String newApprovedById) {
        this.approvedById = newApprovedById;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Medikament that = (Medikament) o;
        return Objects.equals(getMedId(), that.getMedId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMedId());
    }

    @Override
    public String toString() {
        return "Medikament{"
                + "medId='" + medId + '\''
                + ", herstellerId='" + herstellerId + '\''
                + ", bezeichnung='" + bezeichnung + '\''
                + ", infoblattHash='" + infoblattHash + '\''
                + ", ipfsLink='" + ipfsLink + '\''
                + ", status='" + status + '\''
                + ", tags=" + tags
                + ", docType='" + docType + '\''
                + ", approvedById='" + approvedById + '\''
                + '}';
    }
}
