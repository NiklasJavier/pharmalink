package de.jklein.pharmalink.dto;

import java.util.Objects;

/**
 * (chaincode/pharmalink_chaincode_main/src/main/java/de/jklein/fabric/models/Actor.java).
 */
public class ActorDto {

    private String actorId;
    private String email;
    private String role;
    private String ipfsLink;

    public ActorDto() {
    }

    public ActorDto(String actorId, String email, String role, String ipfsLink) {
        this.actorId = actorId;
        this.email = email;
        this.role = role;
        this.ipfsLink = ipfsLink;
    }

    public String getActorId() {
        return actorId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getIpfsLink() {
        return ipfsLink;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setIpfsLink(String ipfsLink) {
        this.ipfsLink = ipfsLink;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActorDto actorDto = (ActorDto) o;
        return Objects.equals(actorId, actorDto.actorId) &&
                Objects.equals(email, actorDto.email) &&
                Objects.equals(role, actorDto.role) &&
                Objects.equals(ipfsLink, actorDto.ipfsLink);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actorId, email, role, ipfsLink);
    }

    @Override
    public String toString() {
        return "ActorDto{" +
                "actorId='" + actorId + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", ipfsLink='" + ipfsLink + '\'' +
                '}';
    }
}