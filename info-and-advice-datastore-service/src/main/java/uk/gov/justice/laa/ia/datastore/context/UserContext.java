package uk.gov.justice.laa.ia.datastore.context;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Component to provide centralized access to user and provider information. This acts as the "one
 * place" to update when we implement real authentication.
 */
@Component
@RequestScope
@Getter
@Setter
public class UserContext {
  /** The user's firm code. */
  private String providerFirmCode = "";

  /** The provider office ID. */
  private String providerOfficeId = "00000000-0000-0000-0000-000000000000";

  /** The current user. */
  private String currentUser = "SYSTEM";
}
