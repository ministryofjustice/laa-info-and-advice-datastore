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

/** Entity to represent an individuals address. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Addresses")
public class AddressEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private UUID id;

  @Version
  @Column(name = "etag", nullable = false)
  private long etag;

  @Column(name = "address_line_1", nullable = false)
  private String addressLine1;

  @Column(name = "address_line_2")
  private String addressLine2;

  @Column(name = "address_line_3")
  private String addressLine3;

  @Column(name = "address_line_4")
  private String addressLine4;

  @Column(name = "town_city")
  private String townOrCity;

  @Column(name = "postcode", length = 12)
  private String postCode;

  @Column(name = "county")
  private String county;

  @Column(name = "country", length = 2)
  private String country;

  @Column(name = "created_at")
  @CreationTimestamp
  private Instant createdAt;

  @Column(name = "modified_at")
  @UpdateTimestamp
  private Instant modifiedAt;

  @Column(name = "created_by", nullable = false)
  private String createdBy;

  @Column(name = "modified_by", nullable = false)
  private String modifiedBy;
}
