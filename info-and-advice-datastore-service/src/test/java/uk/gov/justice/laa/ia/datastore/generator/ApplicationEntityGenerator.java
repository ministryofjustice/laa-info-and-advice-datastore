package uk.gov.justice.laa.ia.datastore.generator;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;

/** Generator for setting for an ApplicationEntity for tests. */
public class ApplicationEntityGenerator {

  /**
   * Creates an Application entity with an Id, used for tests that will not persist to a real data
   * source that will generate the Id.
   */
  public static ApplicationEntity createWithId(
      Consumer<ApplicationEntity.ApplicationEntityBuilder> customizer) {
    return createApplication(customizer)
        .id(UUID.randomUUID())
        .createdAt(Instant.now())
        .modifiedAt(Instant.now())
        .build();
  }

  /**
   * Creates an Application entity with an Id, used for tests that will persist to a real data store
   * that will generate the Id.
   */
  public static ApplicationEntity createWithoutId(
      Consumer<ApplicationEntity.ApplicationEntityBuilder> customizer) {
    return createApplication(customizer).build();
  }

  private static ApplicationEntity.ApplicationEntityBuilder createApplication(
      Consumer<ApplicationEntity.ApplicationEntityBuilder> customizer) {
    var builder =
        ApplicationEntity.builder()
            .providerFirmCode("123456")
            .providerOfficeId(UUID.randomUUID().toString())
            .applicationState(ApplicationState.DRAFT)
            .referenceNumber("L-56C-FTQ")
            .applicationType("RCW")
            .createdBy("Joe Bloggs")
            .modifiedBy("Joe Bloggs");
    if (customizer != null) {
      customizer.accept(builder);
    }
    return builder;
  }
}
