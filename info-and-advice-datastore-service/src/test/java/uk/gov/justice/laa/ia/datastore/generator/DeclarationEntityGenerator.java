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
      Consumer<DeclarationEntity.DeclarationEntityBuilder> customizer) {
    return createDeclaration(customizer)
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
      Consumer<DeclarationEntity.DeclarationEntityBuilder> customizer) {
    return createDeclaration(customizer).build();
  }

  private static DeclarationEntity.DeclarationEntityBuilder createDeclaration(
      Consumer<DeclarationEntity.DeclarationEntityBuilder> customizer) {
    var builder =
        DeclarationEntity.builder()
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
