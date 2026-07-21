package uk.gov.justice.laa.ia.datastore.generator;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import uk.gov.justice.laa.ia.datastore.entity.AddressEntity;

/** Generator for setting for an AddressEntity for tests. */
public class AddressEntityGenerator {

  /**
   * Creates an Address entity without an Id, used for tests that will persist to a real data store
   * that will generate the Id.
   */
  public static AddressEntity createWithoutId(
      Consumer<AddressEntity.AddressEntityBuilder> customizer) {
    return createAddress(customizer).build();
  }

  /**
   * Creates an Address entity with an Id, used for tests that do not persist to a real data store.
   */
  public static AddressEntity createWithId(
      Consumer<AddressEntity.AddressEntityBuilder> customizer) {
    return createAddress(customizer)
        .id(UUID.randomUUID())
        .createdAt(Instant.now())
        .modifiedAt(Instant.now())
        .build();
  }

  private static AddressEntity.AddressEntityBuilder createAddress(
      Consumer<AddressEntity.AddressEntityBuilder> customizer) {
    var builder =
        AddressEntity.builder()
            .addressLine1("10 Downing Street")
            .addressLine2("Prime ministers address")
            .postCode("SW1A 2AA")
            .townOrCity("London")
            .createdBy("SYSTEM")
            .modifiedBy("SYSTEM");

    if (customizer != null) {
      customizer.accept(builder);
    }

    return builder;
  }
}
