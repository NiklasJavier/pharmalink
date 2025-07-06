package de.jklein.pharmalink.api.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation zur Markierung von Controller-Methoden, die für das API-Audit geloggt werden sollen.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditableApi {
    // Optional: Zusätzliche Informationen, die in der Annotation selbst definiert werden könnten
    String description() default "";
}