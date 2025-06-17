// src/main/java/de/jklein/fabric/samples/assettransfer/model/Actor.java
package de.jklein.fabric.samples.assettransfer.model;

import com.owlike.genson.annotation.JsonProperty;
import de.jklein.fabric.samples.assettransfer.permission.RoleConstants;
import java.util.Objects; //

/**
 * Repräsentiert einen registrierten Akteur in der Lieferkette (Hersteller, Großhändler, Apotheke, Behörde).
 */
public final class Actor implements IAsset { //

    private final String actorId;
    private final String actorName;
    private final String roleType; // Entspricht RoleConstants.HERSTELLER etc.
    private final String status; // Entspricht RoleConstants.PENDING_APPROVAL, APPROVED etc.
    private final String publicKey; // Base64-kodierter öffentlicher Schlüssel des Akteurs-Zertifikats

    public Actor(@JsonProperty("actorId") final String actorId,
                 @JsonProperty("actorName") final String actorName,
                 @JsonProperty("roleType") final String roleType,
                 @JsonProperty("status") final String status,
                 @JsonProperty("publicKey") final String publicKey) {
        this.actorId = actorId;
        this.actorName = actorName;
        this.roleType = roleType;
        this.status = status;
        this.publicKey = publicKey;
    }

    @Override
    public String getKey() {
        return actorId;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String getRegulatoryStatus() {
        // Akteure haben keinen direkten regulatorischen Status in diesem Sinne,
        // aber ihr 'status' kann als regulatorisch relevant interpretiert werden.
        return status;
    }

    @Override
    public boolean isLockedByRegulator() {
        // Ein Akteur ist "gesperrt", wenn sein Status nicht APPROVED ist.
        return !RoleConstants.APPROVED.equals(this.status);
    }

    @Override
    public String getAssociatedActorId() {
        return actorId; // Der Akteur ist sein eigener assoziierter Akteur
    }

    public String getActorId() {
        return actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public String getRoleType() {
        return roleType;
    }

    public String getPublicKey() {
        return publicKey;
    }

    /**
     * Prüft, ob der Akteur den Status 'APPROVED' hat.
     * @return true, wenn der Akteur freigegeben ist, sonst false.
     */
    public boolean isApproved() {
        return RoleConstants.APPROVED.equals(this.status);
    }

    /**
     * Prüft, ob der Akteur eine bestimmte Rolle besitzt.
     * @param role Die zu prüfende Rolle.
     * @return true, wenn der Akteur die Rolle hat, sonst false.
     */
    public boolean hasRole(final String role) {
        return this.roleType.equalsIgnoreCase(role);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        Actor other = (Actor) obj;
        return Objects.deepEquals(
                new String[]{getActorId(), getActorName(), getRoleType(), getStatus(), getPublicKey()},
                new String[]{other.getActorId(), other.getActorName(), other.getRoleType(), other.getStatus(), other.getPublicKey()}
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(getActorId(), getActorName(), getRoleType(), getStatus(), getPublicKey());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [actorId=" + actorId
                + ", actorName=" + actorName + ", roleType=" + roleType + ", status=" + status + "]";
    }
}
