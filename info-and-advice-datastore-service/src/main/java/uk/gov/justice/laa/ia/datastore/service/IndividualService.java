package uk.gov.justice.laa.ia.datastore.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.ia.datastore.mapper.IndividualMapper;
import uk.gov.justice.laa.ia.datastore.model.Individual;
import uk.gov.justice.laa.ia.datastore.repository.IndividualRepository;

/** Service class for handling Individuals. */
@RequiredArgsConstructor
@Service
public class IndividualService {
  private final IndividualRepository repository;
  private final IndividualMapper individualMapper;

  /**
   * Gets all the individuals.
   *
   * @return list of {@link Individual}
   */
  public List<Individual> getAllIndividuals() {
    return repository.findAll().stream().map(individualMapper::toIndividual).toList();
  }

  /**
   * Gets a specific individual.
   *
   * @return optional of {@link Individual}
   */
  public Optional<Individual> getIndividual(UUID id) {
    return repository.findById(id).map(individualMapper::toIndividual);
  }
}
