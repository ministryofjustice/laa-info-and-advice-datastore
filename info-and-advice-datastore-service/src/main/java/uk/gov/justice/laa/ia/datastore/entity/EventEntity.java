package uk.gov.justice.laa.ia.datastore.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Entity to represent a recorded mutation event. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "events")
public class EventEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "sequence_number")
  private Long sequenceNumber;

  @Column(name = "changed_by", nullable = false)
  private String changedBy;

  @Column(name = "provider_firm_code")
  private String providerFirmCode;

  @Column(name = "provider_office_id")
  private String providerOfficeId;

  @Column(name = "http_method", nullable = false)
  private String httpMethod;

  @Column(name = "url_path", nullable = false)
  private String urlPath;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", columnDefinition = "jsonb")
  private JsonNode payload;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreationTimestamp
  private Instant createdAt;
}
