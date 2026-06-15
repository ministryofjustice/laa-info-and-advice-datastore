package uk.gov.justice.laa.ia.datastore.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;

/** Unit tests for {@link UserContext}. */
class UserContextTest {

  private final UserContext sut = new UserContext();

  @Test
  void canAccessApplication_shouldReturnTrue_forPermissiveStub() {
    // Arrange
    ApplicationEntity entity = new ApplicationEntity();

    // Act
    boolean result = sut.canAccessApplication(entity);

    // Assert
    assertThat(result).isTrue();
  }
}
