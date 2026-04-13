package uk.gov.justice.laa.ia.datastore.generators;

import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Consumer;
import uk.gov.justice.laa.ia.datastore.entity.IndividualEntity;

/** Generator for setting for an IndividualEntity for tests. */
public class IndividualEntityGenerator {
  /**
   * Creates an individual entity with an Id, used for tests that will not persist to a real data
   * source that will generate the Id.
   */
  public static IndividualEntity createWithId(
      Consumer<IndividualEntity.IndividualEntityBuilder> customizer) {
    return createIndividual(customizer).id(UUID.randomUUID()).build();
  }

  /**
   * Creates an individual entity with an Id, used for tests that will persist to a real data store
   * that will generate the Id.
   */
  public static IndividualEntity createWithoutId(
      Consumer<IndividualEntity.IndividualEntityBuilder> customizer) {
    return createIndividual(customizer).build();
  }

  private static IndividualEntity.IndividualEntityBuilder createIndividual(
      Consumer<IndividualEntity.IndividualEntityBuilder> customizer) {
    var builder =
        IndividualEntity.builder()
            .firstName("Joe")
            .lastName("Bloggs")
            .lastNameAtBirth("Smith")
            .niNumber("AB123456Q")
            .dateOfBirth(LocalDate.of(1990, 01, 01));
    if (customizer != null) {
      customizer.accept(builder);
    }
    return builder;
  }
}
