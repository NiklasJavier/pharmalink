// src/main/java/de/jklein/fabric/samples/assettransfer/model/Tag.java
package de.jklein.fabric.samples.assettransfer.model;

import com.owlike.genson.annotation.JsonProperty;
import java.util.Objects; //

/**
 * Repräsentiert ein allgemeines Tag, das Assets zugeordnet werden kann.
 * Kann als Klassifizierungstag oder regulatorischer Tag verwendet werden.
 */
public final class Tag {

    private final String tagName; // Name des Tags (z.B. "OTC", "Rueckruf")
    private final String tagValue; // Optionaler Wert des Tags (z.B. "true", "GrundXYZ")
    private final String actorId; // Akteur-ID des Setzers des Tags
    private final String timestamp; // Zeitstempel, wann der Tag gesetzt wurde
    private final boolean isBlocking; // Gibt an, ob dieser Tag eine Funktion blockiert (speziell für Regulatorische Tags)

    public Tag(@JsonProperty("tagName") final String tagName,
               @JsonProperty("tagValue") final String tagValue,
               @JsonProperty("actorId") final String actorId,
               @JsonProperty("timestamp") final String timestamp,
               @JsonProperty("isBlocking") final boolean isBlocking) {
        this.tagName = tagName;
        this.tagValue = tagValue;
        this.actorId = actorId;
        this.timestamp = timestamp;
        this.isBlocking = isBlocking;
    }

    public String getTagName() {
        return tagName;
    }

    public String getTagValue() {
        return tagValue;
    }

    public String getActorId() {
        return actorId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public boolean isBlocking() {
        return isBlocking;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        Tag other = (Tag) obj;
        return isBlocking == other.isBlocking
                && Objects.deepEquals(
                        new String[] {getTagName(), getTagValue(), getActorId(), getTimestamp()},
                        new String[] {other.getTagName(), other.getTagValue(), other.getActorId(), other.getTimestamp()}
                );
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTagName(), getTagValue(), getActorId(), getTimestamp(), isBlocking);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [tagName=" + tagName
                + ", tagValue=" + tagValue + ", actorId=" + actorId + ", timestamp=" + timestamp + ", isBlocking=" + isBlocking + "]";
    }
}
