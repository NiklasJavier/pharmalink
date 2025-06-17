// src/main/java/de/jklein/fabric/samples/assettransfer/permission/ContractPermission.java
package de.jklein.fabric.samples.assettransfer.permission;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Eine Dokumentations-Annotation, die die Berechtigungsanforderungen für eine Contract-Methode beschreibt.
 * Diese Annotation hat keine funktionale Auswirkung, sondern dient nur der Dokumentation.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD})
public @interface ContractPermission {

    /**
     * Die erforderlichen Rollen für diese Transaktion.
     * @return Array von erforderlichen Rollen (aus RoleConstants)
     */
    String[] roles() default {};

    /**
     * Gibt an, ob der Aufrufer ein genehmigter Akteur sein muss.
     * @return true, wenn der Aufrufer genehmigt sein muss
     */
    boolean requireApproved() default true;

    /**
     * Gibt an, ob der Aufrufer der Ersteller des betroffenen Assets sein muss.
     * @return true, wenn der Aufrufer der Ersteller sein muss
     */
    boolean requireCreator() default false;

    /**
     * Gibt an, ob der Aufrufer der aktuelle Besitzer des betroffenen Assets sein muss.
     * @return true, wenn der Aufrufer der Besitzer sein muss
     */
    boolean requireOwner() default false;

    /**
     * Der erwartete Asset-Status für diese Transaktion.
     * @return Der erwartete Status (aus RoleConstants)
     */
    String expectedStatus() default "";

    /**
     * Freie Beschreibung weiterer Berechtigungsanforderungen.
     * @return Zusätzliche Beschreibung
     */
    String description() default "";
}
