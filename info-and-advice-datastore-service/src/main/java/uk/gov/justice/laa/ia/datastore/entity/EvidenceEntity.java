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
import uk.gov.justice.laa.ia.datastore.model.EvidenceStatus;

/** Entity to represent an evidence. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Evidence")
public class EvidenceEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private UUID id;

  @Column(name = "reference_number", nullable = false)
  private UUID referenceNumber;

  @Column(name = "evidence_status", nullable = false)
  private EvidenceStatus evidenceStatus;

  @Column(name = "paye_income_evidence", nullable = false)
  private boolean payeIncomeEvidence;

  @Column(name = "other_income_evidence", nullable = false)
  private boolean otherIncomeEvidence;

  @Column(name = "housing_costs_evidence", nullable = false)
  private boolean housingCostsEvidence;

  @Column(name = "capital_evidence", nullable = false)
  private boolean capitalEvidence;

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
