package uk.gov.justice.laa.ia.datastore.context;

import java.util.List;
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

  /** The office codes the user is authorised for. */
  private List<String> officeCodes = List.of();

  /** The current user. */
  private String currentUser = "SYSTEM";

  /** The correlation ID for the current request. */
  private String correlationId;
}
