package uk.gov.justice.laa.ia.datastore.mapper;

/** Base test for setting up the mappers. */
public class BaseMapperTest {

  protected DateTimeMapper dtMapper;
  protected AddressMapper addressMapper;
  protected IndividualMapper individualMapper;
  protected DeclarationMapper declarationMapper;
  protected EvidenceMapper evidenceMapper;
  protected ApplicationMapper applicationMapper;

  protected BaseMapperTest() {
    dtMapper = new DateTimeMapperImpl();
    addressMapper = new AddressMapperImpl(dtMapper);
    individualMapper = new IndividualMapperImpl(dtMapper, addressMapper);
    declarationMapper = new DeclarationMapperImpl(dtMapper);
    evidenceMapper = new EvidenceMapperImpl(dtMapper);

    applicationMapper =
        new ApplicationMapperImpl(dtMapper, individualMapper, declarationMapper, evidenceMapper);
  }
}
