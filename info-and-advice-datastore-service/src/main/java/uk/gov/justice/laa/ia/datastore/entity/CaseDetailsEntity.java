package uk.gov.justice.laa.ia.datastore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Entity to represent an case details. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "case_details")
public class CaseDetailsEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private UUID id;

  @Column(name = "require_ecf", nullable = true)
  private Boolean requireEcf;

  @Column(name = "has_previous_legal_aid", nullable = true)
  private Boolean hasPreviousLegalAid;

  @Column(name = "has_six_months_legal_help", nullable = true)
  private Boolean hasSixMonthsLegalHelp;

  @Column(name = "means_assessment_required", nullable = true)
  private Boolean meansAssessmentRequired;

  @Column(name = "type_non_means_tested", nullable = true)
  private Boolean typeNonMeansTested;

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
