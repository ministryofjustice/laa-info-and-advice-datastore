package uk.gov.justice.laa.ia.datastore.exception;

import java.util.UUID;

/**
 * Thrown when a PATCH request attempts to partially update a linked entity (e.g. declaration) that
 * does not yet exist on the application. Such linked entities must be created via their dedicated
 * endpoint first, since it requires fields that a partial patch cannot guarantee are all present.
 */
public class MissingLinkedEntityException extends RuntimeException {

  /**
   * Constructs the exception for the given linked entity name and application ID.
   *
   * @param linkedEntityName the name of the linked entity that does not yet exist (e.g.
   *     "declaration")
   * @param applicationId the ID of the application the patch was attempted against
   */
  public MissingLinkedEntityException(String linkedEntityName, UUID applicationId) {
    super(
        "Cannot patch "
            + linkedEntityName
            + " for application "
            + applicationId
            + " because it does not exist yet");
  }
}
