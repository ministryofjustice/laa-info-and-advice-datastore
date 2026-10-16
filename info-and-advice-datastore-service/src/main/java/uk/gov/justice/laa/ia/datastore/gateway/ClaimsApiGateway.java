package uk.gov.justice.laa.ia.datastore.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.http.HttpClient;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.ia.datastore.config.ClaimsApiProperties;
import uk.gov.justice.laa.ia.datastore.models.ApplicationClaimResponse;
import uk.gov.justice.laa.ia.datastore.models.ClaimsModel;

/**
 * Calls the claims API retention lookup, which returns only VALID claims for an office and UFN
 * without any PII. Results are paged, so every page is read to make sure the latest claim is seen.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "laa.datastore.claims-api.mode", havingValue = "live")
public class ClaimsApiGateway implements ClaimsGateway {
  private static final String RETENTION_LOOKUP_PATH = "/api/v1/claims/retention-lookup";

  private final RestClient restClient;
  private final int pageSize;

  /** Builds the REST client from {@link ClaimsApiProperties}. */
  public ClaimsApiGateway(RestClient.Builder restClientBuilder, ClaimsApiProperties properties) {
    final var httpClient =
        HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
    final var requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(properties.readTimeout());
    this.restClient =
        restClientBuilder
            .baseUrl(properties.baseUrl())
            .requestFactory(requestFactory)
            .defaultHeader(HttpHeaders.AUTHORIZATION, properties.apiKey())
            .build();
    this.pageSize = properties.pageSize();
  }

  @Override
  public ApplicationClaimResponse getClaims(String officeCode, String ufn) {
    final var claims = new ArrayList<ClaimsModel>();
    int page = 0;
    int totalPages;
    do {
      final var result = fetchPage(officeCode, ufn, page);
      if (result == null || result.content() == null) {
        break;
      }
      result.content().stream().map(ClaimsApiGateway::toModel).forEach(claims::add);
      totalPages = result.totalPages();
      page++;
    } while (page < totalPages);
    return ApplicationClaimResponse.builder().claims(claims).build();
  }

  private RetentionLookupResult fetchPage(String officeCode, String ufn, int page) {
    return restClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(RETENTION_LOOKUP_PATH)
                    .queryParam("office_code", "{officeCode}")
                    .queryParam("ufn", "{ufn}")
                    .queryParam("page", "{page}")
                    .queryParam("size", "{size}")
                    .build(officeCode, ufn, page, pageSize))
        .retrieve()
        .body(RetentionLookupResult.class);
  }

  private static ClaimsModel toModel(RetentionLookupClaim claim) {
    return ClaimsModel.builder()
        .claimId(claim.claimId())
        .status(claim.status())
        .createdOn(claim.createdOn())
        .updatedOn(claim.updatedOn())
        .build();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record RetentionLookupResult(
      List<RetentionLookupClaim> content, @JsonProperty("total_pages") int totalPages) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record RetentionLookupClaim(
      @JsonProperty("claim_id") UUID claimId,
      String status,
      @JsonProperty("created_on") OffsetDateTime createdOn,
      @JsonProperty("updated_on") OffsetDateTime updatedOn) {}
}
