package uk.gov.justice.laa.ia.datastore.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uk.gov.justice.laa.ia.datastore.model.ClientCaseDetailsStatus;
import uk.gov.justice.laa.ia.datastore.model.OverallApplicationStatus;

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

  @Column(name = "provider_firm_id", nullable = false)
  private UUID providerFirmId;

  @Column(name = "provider_office_id", nullable = false)
  private UUID providerOfficeId;

  @Column(name = "eligibility_result_id", nullable = true)
  private UUID eligibilityResultId;

  @Column(name = "client_case_details_status", nullable = false)
  private ClientCaseDetailsStatus clientCaseDetailsStatus;

  @Column(name = "means_assessment_status_id", nullable = true)
  private UUID meansAssessmentStatusId;

  @Column(name = "evidence_status_id", nullable = true)
  private UUID evidenceStatusId;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "evidence_id", nullable = true)
  private EvidenceEntity evidence;

  @Column(name = "client_declaration_status_id", nullable = true)
  private UUID clientDeclarationStatusId;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "declaration_id", nullable = true)
  private DeclarationEntity declaration;

  @Column(name = "overall_application_status", nullable = false)
  private OverallApplicationStatus overallApplicationStatus;

  @Column(name = "unique_file_number", nullable = false)
  private UUID uniqueFileNumber;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "individual_id", nullable = false)
  private IndividualEntity individual;

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
