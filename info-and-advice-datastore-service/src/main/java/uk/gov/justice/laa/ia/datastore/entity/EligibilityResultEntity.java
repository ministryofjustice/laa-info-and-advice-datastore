package uk.gov.justice.laa.ia.datastore.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Entity to represent eligibility results. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "eligibility_results")
public class EligibilityResultEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "eligibility_result_id")
  private UUID eligibilityResultId;

  @Column(name = "application_id", nullable = false)
  private UUID applicationId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "data")
  private JsonNode data;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "result_json", nullable = false)
  private JsonNode resultJson;

  @Column(name = "indication")
  private Boolean indication;

  @Column(name = "contribution")
  private String contribution;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreationTimestamp
  private Instant createdAt;

  @Column(name = "created_by", nullable = false)
  private String createdBy;
}
