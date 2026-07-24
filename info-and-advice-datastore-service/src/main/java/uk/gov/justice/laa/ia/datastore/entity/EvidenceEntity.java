package uk.gov.justice.laa.ia.datastore.entity;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/** Entity to represent evidence associated with an application. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "evidence")
public class EvidenceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "evidence_id")
  private UUID evidenceId;

  @Column(name = "case_id", nullable = false)
  private String caseId;

  @Column(name = "evidence_exemption_code")
  private String evidenceExemptionCode;

  @Column(name = "evidence_exemption_reason", length = 400)
  private String evidenceExemptionReason;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "income_evidence_checklist", columnDefinition = "jsonb")
  private JsonNode incomeEvidenceChecklist;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "expenditure_capital_evidence_checklist", columnDefinition = "jsonb")
  private JsonNode expenditureCapitalEvidenceChecklist;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreationTimestamp
  private Instant createdAt;

  @Column(name = "created_by", nullable = false)
  private String createdBy;

  @Column(name = "modified_at", nullable = false)
  @UpdateTimestamp
  private Instant modifiedAt;

  @Column(name = "modified_by", nullable = false)
  private String modifiedBy;
}
