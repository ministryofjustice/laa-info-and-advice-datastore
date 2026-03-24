package uk.gov.justice.laa.ia.datastore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** The entity class for Individual. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Individuals")
public class IndividualEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private UUID id;

  @Column(name = "first_name", nullable = false)
  private String firstName;

  @Column(name = "last_name", nullable = false)
  private String lastName;

  @Column(name = "last_name_at_birth")
  private String lastNameAtBirth;

  @Column(name = "date_of_birth", nullable = false)
  private LocalDate dateOfBirth;

  @Column(name = "ni_number")
  private String niNumber;

  @Column(name = "created_at")
  @CreationTimestamp
  private Instant createdAt;

  @Column(name = "last_modified_at")
  @UpdateTimestamp
  private Instant modifiedAt;
}
