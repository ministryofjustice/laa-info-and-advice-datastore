package uk.gov.justice.laa.ia.datastore;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to indicate that the annotated element should be excluded from code coverage analysis.
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD, CONSTRUCTOR})
public @interface ExcludeFromCodeCoverage {
  /**
   * Reason for excluding the element from code coverage analysis.
   *
   * @return the reason as a string
   */
  String reason() default "";
}
