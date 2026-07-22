package uk.gov.justice.laa.ia.datastore.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;

/** Entity to represent an application. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Applications")
public class ApplicationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private UUID id;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @Column(name = "provider_firm_id", nullable = false)
  private UUID providerFirmId;

  @Column(name = "provider_office_id", nullable = false)
  private UUID providerOfficeId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "evidence", nullable = true, columnDefinition = "json")
  private Map<String, Object> evidence;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "declaration_id", nullable = true)
  private DeclarationEntity declaration;

  @Column(name = "application_state", nullable = false)
  private ApplicationState applicationState;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "client_details_id", nullable = false)
  private ClientDetailsEntity clientDetails;

  @Column(name = "reference_number", nullable = false)
  private String referenceNumber;

  @Column(name = "reason_for_reapplication", nullable = true)
  private String reasonForReapplication;

  @Column(name = "means_assessment_required", nullable = true)
  private Boolean meansAssessmentRequired;

  @Column(name = "type_of_non_means", nullable = true)
  private Boolean typeOfNonMeans;

  @Column(name = "ecf_flag", nullable = true)
  private Boolean ecfFlag;

  @Column(name = "contribution", nullable = true)
  private String contribution;

  @Column(name = "application_type", nullable = false)
  private String applicationType;

  @Column(name = "determination_id", nullable = true)
  private UUID determinationId;

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "application_id", referencedColumnName = "id")
  private Set<EligibilityResultEntity> eligibilityResults;

  /** Returns the most recent eligibility result for this application. */
  public EligibilityResultEntity getMostRecentEligibilityResult() {
    if (eligibilityResults == null || eligibilityResults.isEmpty()) {
      return null;
    }
    return eligibilityResults.stream()
        .max((er1, er2) -> er1.getCreatedAt().compareTo(er2.getCreatedAt()))
        .orElse(null);
  }

  @Column(name = "created_at", nullable = false)
  @CreationTimestamp
  private Instant createdAt;

  @Column(name = "created_by", nullable = false)
  private String createdBy;

  @Column(name = "last_modified_at", nullable = false)
  @UpdateTimestamp
  private Instant modifiedAt;

  @Column(name = "last_modified_by", nullable = false)
  private String modifiedBy;
}
