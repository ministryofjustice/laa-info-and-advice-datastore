package uk.gov.justice.laa.ia.datastore.service;

import java.util.List;
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
}
