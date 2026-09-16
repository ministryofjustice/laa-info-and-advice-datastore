# Request Command → Datastore Field Mapping

Verified by tracing OpenAPI request schemas (`info-and-advice-datastore-api/open-api-applications/components/request-schemas.yml`,
`client-details-components.yml`) through the MapStruct mappers
(`info-and-advice-datastore-service/src/main/java/uk/gov/justice/laa/ia/datastore/mapper/`)
to the JPA entities (`info-and-advice-datastore-service/src/main/java/uk/gov/justice/laa/ia/datastore/entity/`).

## Summary

Every field in every request command maps through to a concrete column/entity field in the datastore.

| Command | Entity | Mapping notes |
|---|---|---|
| `StartApplicationCommand` | `ApplicationEntity` (+ nested `ClientDetailsEntity`) | `applicationType`, `providerOfficeCode` auto-map by name; `client` → `clientDetails` |
| `CreateClientCommand` | `ClientDetailsEntity` | `nationalInsuranceNumber` → `niNumber` (renamed via explicit `@Mapping` in `ClientDetailsMapper`); `createAddressCommand` → `address` (also explicit, despite the odd property name) |
| `CreateAddressCommand`/`UpdateAddressCommand` | `AddressEntity` | 1:1 field names |
| `DeclarationCommand` | `DeclarationEntity` | `declarationConfirmation`, `dateSigned` map directly |
| `UpdateEvidenceCommand` | `EvidenceEntity` | all 4 fields map directly |
| `UpdateMeansDataCommand` | `EligibilityResultEntity` | `data` → `data`, `result` → `resultJson` |
| `UpdateScopingDataCommand` | `ApplicationEntity.scopingQuestions` (jsonb) | direct |
| `UpdateApplicationCommand` | `ApplicationEntity.applicationState` | direct |
| `EditApplicationCommand` | `ApplicationEntity` | `ufn`, `laaReference`, `reasonForReapplication`, `meansAssessmentRequired`, `typeOfNonMeans`, `ecfFlag`, `contribution`, `determinationId` — all present as columns on `ApplicationEntity` |
| `UpdateClientDetailsCommand` | `ClientDetailsEntity` | `firstName`, `lastName` (→ `surname` column), `dateOfBirth`, `niNumber`, `noFixedAbode`, nested `address` — all map |

## Notable nuances

- **`eTag` is not persisted on the child entity being updated.** `DeclarationEntity`, `EvidenceEntity`, and `EligibilityResultEntity` have no `etag`/`@Version` column at all (confirmed by explicit `@Mapping(target = "etag", ignore = true)` in the mappers). Instead, `ApplicationService.validateEtag()` compares the command's `eTag` against the parent `ApplicationEntity.etag` (its `@Version` column) for optimistic-concurrency checks (`ApplicationService.java` lines ~269-272). The eTag is consumed for validation, not stored where you might expect.
- **`DeclarationCommand` has no `clientDeclarationStatus` field**, and `DeclarationMapper` explicitly ignores that entity property during mapping. It's populated separately by the service, hardcoded to `ClientDeclarationStatus.DRAFT` (`ApplicationService.java` line 216), not derived from any request field.

## Conclusion

No dropped/unmapped request fields, but two intentional renames (`nationalInsuranceNumber` → `niNumber`, `createAddressCommand` → `address`) and one field (`eTag`) that's validated against a different entity than the one being mutated rather than stored on it directly.