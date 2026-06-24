package uk.gov.justice.laa.ia.datastore.mapper;

/** Base test for setting up the mappers. */
public class BaseMapperTest {

  protected DateTimeMapper dtMapper;
  protected AddressMapper addressMapper;
  protected ClientDetailsMapper clientDetailsMapper;
  protected DeclarationMapper declarationMapper;
  protected ApplicationMapper applicationMapper;

  protected BaseMapperTest() {
    dtMapper = new DateTimeMapperImpl();
    addressMapper = new AddressMapperImpl(dtMapper);
    clientDetailsMapper = new ClientDetailsMapperImpl(dtMapper, addressMapper);
    declarationMapper = new DeclarationMapperImpl(dtMapper);

    applicationMapper = new ApplicationMapperImpl(dtMapper, clientDetailsMapper, declarationMapper);
  }
}
