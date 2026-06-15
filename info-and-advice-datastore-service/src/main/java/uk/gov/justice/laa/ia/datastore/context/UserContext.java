package uk.gov.justice.laa.ia.datastore.context;

import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;

/**
 * Component to provide centralized access to user and provider information. This acts as the "one
 * place" to update when we implement real authentication.
 */
@Component
public class UserContext {

  /**
   * Stubbed authorization check. Currently permissive (returns true).
   *
   * @param entity The application entity to check access for.
   * @return true if access is allowed.
   */
  public boolean canAccessApplication(ApplicationEntity entity) {
    // TODO: Compare entity.getProviderFirmId() with this.getProviderFirmId() once tokens are live
    return true;
  }

  /**
   * Gets the current provider firm ID.
   *
   * @return UUID of the provider firm.
   */
  public UUID getProviderFirmId() {
    // TODO: Replace with real logic (e.g., extract from JWT)
    return UUID.fromString("00000000-0000-0000-0000-000000000000");
  }

  /**
   * Gets the current provider office ID.
   *
   * @return UUID of the provider office.
   */
  public UUID getProviderOfficeId() {
    // TODO: Replace with real logic (e.g., extract from JWT)
    return UUID.fromString("00000000-0000-0000-0000-000000000001");
  }

  /**
   * Gets the current user.
   *
   * @return username of the current user.
   */
  public String getCurrentUser() {
    // TODO: Replace with real logic (e.g., extract from SecurityContext)
    return "SYSTEM";
  }
}
