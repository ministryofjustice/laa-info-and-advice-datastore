package uk.gov.justice.laa.ia.datastore.generator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Consumer;
import uk.gov.justice.laa.ia.datastore.entity.ClientDetailsEntity;

/** Generator for setting for an ClientDetailsEntity for tests. */
public class ClientDetailsEntityGenerator {
  /**
   * Creates a client details entity with an Id, used for tests that will not persist to a real data
   * source that will generate the Id.
   */
  public static ClientDetailsEntity createWithId(
      Consumer<ClientDetailsEntity.ClientDetailsEntityBuilder> customizer) {
    return createClientDetails(customizer)
        .id(UUID.randomUUID())
        .createdAt(Instant.now())
        .modifiedAt(Instant.now())
        .build();
  }

  /**
   * Creates a client details entity without an Id, used for tests that will persist to a real data
   * store that will generate the Id.
   */
  public static ClientDetailsEntity createWithoutId(
      Consumer<ClientDetailsEntity.ClientDetailsEntityBuilder> customizer) {
    return createClientDetails(customizer).build();
  }

  private static ClientDetailsEntity.ClientDetailsEntityBuilder createClientDetails(
      Consumer<ClientDetailsEntity.ClientDetailsEntityBuilder> customizer) {
    var builder =
        ClientDetailsEntity.builder()
            .firstName("Joe")
            .lastName("Bloggs")
            .niNumber("AB123456Q")
            .dateOfBirth(LocalDate.of(1990, 01, 01))
            .createdBy("SYSTEM")
            .modifiedBy("SYSTEM");
    if (customizer != null) {
      customizer.accept(builder);
    }
    return builder;
  }
}
