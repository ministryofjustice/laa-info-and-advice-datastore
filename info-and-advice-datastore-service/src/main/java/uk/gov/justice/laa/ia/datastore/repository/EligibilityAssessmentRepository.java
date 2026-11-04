package uk.gov.justice.laa.ia.datastore.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityAssessmentEntity;

/** Repository for EligibilityAssessmentEntity. */
@Repository
public interface EligibilityAssessmentRepository
    extends JpaRepository<EligibilityAssessmentEntity, UUID> {}
