package uk.gov.justice.laa.ia.datastore.utils;

/** Shared constants to be used in testing. */
public class TestConstants {
  private TestConstants() {}

  public static final String UpdateMeansData = "/api/v0/applications/{id}:update-means-data";
  public static final String UpdateDeclarationData =
      "/api/v0/applications/{id}:update-declaration-data";
  public static final String UpdateEvidence = "/api/v0/applications/{id}:update-evidence";
  public static final String UpdateScopingData = "/api/v0/applications/{id}:update-scoping-data";
  public static final String UpdateApplication = "/api/v0/applications/{id}:update-application";
  public static final String EditApplication = "/api/v0/applications/{id}:edit-application";
}
