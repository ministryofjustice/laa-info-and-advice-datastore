package uk.gov.justice.laa.ia.datastore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uk.gov.justice.laa.ia.datastore.model.ClientDeclarationStatus;

/** Entity to represent a client declaration. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Declaration")
public class DeclarationEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private UUID id;

  @Version
  @Column(name = "etag", nullable = false)
  private long etag;

  @Column(name = "client_declaration_status", nullable = false)
  private ClientDeclarationStatus clientDeclarationStatus;

  @Column(name = "declaration_confirmation", nullable = false)
  private boolean declarationConfirmation;

  @Column(name = "created_at", nullable = false)
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
