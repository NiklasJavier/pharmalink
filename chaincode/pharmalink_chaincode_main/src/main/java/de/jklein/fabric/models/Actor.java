package de.jklein.fabric.models;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.Objects;

@DataType()
public final class Actor {

    @Property()
    private String actorId;

    @Property()
    private String bezeichnung;

    @Property()
    private String role;

    @Property()
    private String email;

    @Property()
    private String ipfsLink;

    @Property()
    private String docType;

    public Actor() {
    }

    public Actor(@JsonProperty("actorId") final String actorId,
                 @JsonProperty("bezeichnung") final String bezeichnung,
                 @JsonProperty("role") final String role,
                 @JsonProperty("email") final String email,
                 @JsonProperty("ipfsLink") final String ipfsLink) {
        this.actorId = actorId;
        this.bezeichnung = bezeichnung;
        this.role = role;
        this.email = email;
        this.ipfsLink = ipfsLink;
        this.docType = "actor";
    }

    public Actor(@JsonProperty("actorId") final String actorId,
                 @JsonProperty("role") final String role) {
        this.actorId = actorId;
        this.role = role;
        this.docType = "actor";
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(final String newActorId) {
        this.actorId = newActorId;
    }

    public String getBezeichnung() {
        return bezeichnung; }

    public void setBezeichnung(final String newBezeichnung) {
        this.bezeichnung = newBezeichnung; }

    public String getRole() {
        return role;
    }

    public void setRole(final String newRole) {
        this.role = newRole;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String newEmail) {
        this.email = newEmail;
    }

    public String getIpfsLink() {
        return ipfsLink;
    }

    public void setIpfsLink(final String newIpfsLink) {
        this.ipfsLink = newIpfsLink;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(final String newDocType) {
        this.docType = newDocType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Actor actor = (Actor) o;
        return Objects.equals(getActorId(), actor.getActorId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getActorId());
    }

    @Override
    public String toString() {
        return "Actor{"
                + "actorId='" + actorId + '\''
                + ", role='" + role + '\''
                + ", email='" + email + '\''
                + ", ipfsLink='" + ipfsLink + '\''
                + ", docType='" + docType + '\''
                + '}';
    }
}
