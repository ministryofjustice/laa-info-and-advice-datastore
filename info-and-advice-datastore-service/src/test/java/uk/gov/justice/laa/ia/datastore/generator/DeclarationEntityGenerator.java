package uk.gov.justice.laa.ia.datastore.generator;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import uk.gov.justice.laa.ia.datastore.entity.DeclarationEntity;
import uk.gov.justice.laa.ia.datastore.model.ClientDeclarationStatus;

/** Generator for setting for an DeclarationEntity for tests. */
public class DeclarationEntityGenerator {
  /**
   * Creates an Declaration entity with an Id, used for tests that will not persist to a real data
   * source that will generate the Id.
   */
  public static DeclarationEntity createWithId(
      UUID applicationId, Consumer<DeclarationEntity.DeclarationEntityBuilder> customizer) {
    return createDeclaration(applicationId, customizer)
        .id(UUID.randomUUID())
        .createdAt(Instant.now())
        .modifiedAt(Instant.now())
        .build();
  }

  /**
   * Creates an Declaration entity with an Id, used for tests that will persist to a real data store
   * that will generate the Id.
   */
  public static DeclarationEntity createWithoutId(
      UUID applicationId, Consumer<DeclarationEntity.DeclarationEntityBuilder> customizer) {
    return createDeclaration(applicationId, customizer).build();
  }

  private static DeclarationEntity.DeclarationEntityBuilder createDeclaration(
      UUID applicationId, Consumer<DeclarationEntity.DeclarationEntityBuilder> customizer) {
    var builder =
        DeclarationEntity.builder()
            .referenceNumber(applicationId)
            .clientDeclarationStatus(ClientDeclarationStatus.DRAFT)
            .declarationStatement(false)
            .createdBy("Joe Bloggs")
            .modifiedBy("James Bloggs");
    if (customizer != null) {
      customizer.accept(builder);
    }
    return builder;
  }
}
