package uk.gov.justice.laa.ia.datastore.service;

import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.ia.datastore.entity.ClientDetailsEntity;
import uk.gov.justice.laa.ia.datastore.mapper.ClientDetailsMapper;
import uk.gov.justice.laa.ia.datastore.model.ClientDetails;
import uk.gov.justice.laa.ia.datastore.model.CreateClientCommand;
import uk.gov.justice.laa.ia.datastore.repository.ClientDetailsRepository;

/** Service class for handling client details. */
@RequiredArgsConstructor
@Service
public class ClientDetailsService {
  private final ClientDetailsRepository repository;
  private final ClientDetailsMapper clientDetailMapper;
  private final AddressService addressService;

  /**
   * Gets a specific client's details.
   *
   * @return optional of {@link ClientDetails} if found, otherwise empty optional
   */
  public Optional<ClientDetails> getClientDetails(UUID id) {
    return repository.findById(id).map(clientDetailMapper::toClientDetails);
  }

  /**
   * Creates a client.
   *
   * @return UUID of created {@link ClientDetailsEntity}
   */
  @Transactional
  public UUID handleCreateCommand(CreateClientCommand cmd) {
    final ClientDetailsEntity clientDetails = clientDetailMapper.toClientDetailsEntity(cmd);
    addressService.createAddress(cmd.getCreateAddressCommand());
    repository.save(clientDetails);
    return null;
  }
}
