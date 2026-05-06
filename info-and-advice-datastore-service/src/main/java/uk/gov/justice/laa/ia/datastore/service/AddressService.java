package uk.gov.justice.laa.ia.datastore.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.ia.datastore.entity.AddressEntity;
import uk.gov.justice.laa.ia.datastore.mapper.AddressMapper;
import uk.gov.justice.laa.ia.datastore.model.CreateAddressCommand;
import uk.gov.justice.laa.ia.datastore.repository.AddressRepository;

/** Service class for handling Addresses. */
@RequiredArgsConstructor
@Service
public class AddressService {
  private final AddressMapper mapper;
  private final AddressRepository addressRepository;

  /**
   * Create an address.
   *
   * @return ID of newly created address.
   */
  public UUID createAddress(CreateAddressCommand createAddressCommand) {
    final AddressEntity address = mapper.toAddressEntity(createAddressCommand);
    final AddressEntity savedAddress = addressRepository.save(address);
    return savedAddress.getId();
  }
}
