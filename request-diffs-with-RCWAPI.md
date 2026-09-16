# REST Model Comparison: laa-info-and-advice-datastore vs laa-record-controlled-work-api

Comparison of OpenAPI schemas between **info-and-advice-datastore**
(`info-and-advice-datastore-api/open-api-applications/components/request-schemas.yml`,
`response-schemas.yml`, `client-details-components.yml`) and
**laa-record-controlled-work-api** (`record-controlled-work-api/open-api-specification.yml`).

## Naming conventions

| | info-and-advice-datastore | record-controlled-work-api |
|---|---|---|
| Request models | `*Command` (`StartApplicationCommand`, `DeclarationCommand`, `UpdateEvidenceCommand`...) | `*RequestBody` (`CreateApplicationRequestBody`, `UpdateEvidenceRequestBody`...) |
| Response/entity models | `*Response`/`*Details` (`ApplicationResponse`, `DeclarationResponse`, `ClientDetails`) | Plain nouns (`Application`, `Declaration`, `Evidence`, `ClientDetails`) reused for both persisted entity and response |
| Error model | `ApiError` (title/status/detail only) | `ProblemDetail` (adds `type`, `instance`, `reason` — full RFC 7807 + custom `reason` code) |

## Optimistic concurrency (`eTag`)

- info-and-advice-datastore requires `eTag` on **almost every** update command: `DeclarationCommand`, `UpdateEvidenceCommand`, `UpdateMeansDataCommand`, `UpdateScopingDataCommand`, `UpdateApplicationCommand`, `EditApplicationCommand`, `UpdateClientDetailsCommand` — and returns it on `ApplicationResponse`.
- record-controlled-work-api only puts `eTag` on `UpdateApplicationStatusRequestBody`. `UpdateEvidenceRequestBody`, `UpdateMeansDataRequestBody`, and `UpdateDeclarationRequestBody` have **no** `eTag`, and the `Application` schema doesn't expose one at all — no optimistic-concurrency story for those endpoints.

## Application creation

- info-and-advice `StartApplicationCommand`: `client` (nested `CreateClientCommand`), `applicationType` **enum** (`RCW`, `CLA`), `providerOfficeCode`. Scoping questions are set later via a separate `UpdateScopingDataCommand`.
- record-controlled-work `CreateApplicationRequestBody`: `legalAidBefore`, `legalAidLast6Months`, `reasonForReapplication`, `providerOfficeCode`, `scopingQuestions` (set inline at creation), `clientDetails`. No `applicationType` field on create, even though the persisted `Application` requires one — implies it's hardcoded server-side to a single work type, consistent with this API being RCW-only, whereas info-and-advice is a shared datastore for both RCW and CLA.

## Client/address fields

- **Boolean naming is inverted**: info-and-advice uses `noFixedAbode`; record-controlled-work uses `hasFixedAddress` (opposite polarity for essentially the same concept).
- **NI number field name differs**: `nationalInsuranceNumber` (info-and-advice `CreateClientCommand`) vs `niNumber` (record-controlled-work, and also `niNumber` in info-and-advice's own `UpdateClientDetailsCommand` — so info-and-advice is inconsistent with itself between create and update).
- record-controlled-work's `niNumber` has a `pattern` regex validation; info-and-advice's `CreateClientCommand.nationalInsuranceNumber` also has a pattern, but `UpdateClientDetailsCommand.niNumber` has no pattern, just `minLength`/`maxLength`.
- Likely bug: info-and-advice's `CreateClientCommand` nests the address under a property literally named `createAddressCommand` instead of `address` (inconsistent with `UpdateClientDetailsCommand.address` and with record-controlled-work's `address`/`CreateAddressRequestBody`).
- Address model fields themselves (`addressLine1-4`, `townOrCity`, `postCode`, `county`, `country` maxLength 2) are consistent across both projects.

## Update/edit surface area

- info-and-advice-datastore exposes far more granular edit capability: `EditApplicationCommand` (`ufn`, `laaReference`, `reasonForReapplication`, `meansAssessmentRequired`, `typeOfNonMeans`, `ecfFlag`, `contribution`, `determinationId`) and `UpdateClientDetailsCommand` — neither has an equivalent in record-controlled-work-api.
- record-controlled-work-api has no `ufn`, `ecfFlag`, `laaReference`, `determinationId`, or `isMeansTested` concepts anywhere — these appear to be info-and-advice/CLA-specific fields.

## Declaration

- info-and-advice `DeclarationCommand` adds an `x-extra-annotation` for `@PastOrPresent` validation on `dateSigned` (code-gen hint, not present in record-controlled-work).
- info-and-advice `DeclarationResponse` includes a `clientDeclarationStatus` enum (`DRAFT`) not present on record-controlled-work's `Declaration`.
- record-controlled-work's `Declaration` entity has a strict `required` list (id, declarationConfirmation, createdAt/By, modifiedAt/By) with nullable fields; info-and-advice's response model has no required list at all.

## Evidence

- Both share the same four fields (`evidenceExemptionCode`, `evidenceExemptionReason`, `incomeEvidenceChecklist`, `expenditureCapitalEvidenceChecklist`).
- info-and-advice's `UpdateEvidenceCommand` adds `maxLength: 400` on `evidenceExemptionReason`; record-controlled-work has no length constraint.
- record-controlled-work's `Evidence` entity marks all four fields `required` (but `nullable: true`), whereas info-and-advice only requires `eTag`.

## Means / Eligibility

- Both use a `data`/`result` pair, but named differently: info-and-advice `UpdateMeansDataCommand`/`EligibilityResult` vs record-controlled-work `UpdateMeansDataRequestBody`/`Eligibility`.
- Both define an identical `EligibilityIndication` enum (`eligible`/`ineligible`), so that concept is consistent.

## Top-level Application/response shape

- record-controlled-work's `Application` has a long `required` list (11 fields including `applicationState`, `declaration`, `evidence`, `eligibility`, `applicationType`) and uses the `nullable: true` + `allOf: [$ref]` pattern for optional nested refs.
- info-and-advice's `ApplicationResponse` has a much shorter required list (id, individualLegalAidNumber, client, providerFirmCode, providerOfficeCode, createdAt/By, modifiedAt/By) with everything else optional — reflecting a more incrementally-populated application lifecycle (start → edit → declaration → evidence → means, each independently optional until set).

## Summary

The two APIs model overlapping domain concepts (client, address, declaration, evidence, eligibility/means, application state) but diverge in naming conventions, required/nullable strictness, concurrency-control design (pervasive `eTag` in info-and-advice vs one-off in record-controlled-work), and info-and-advice carries extra CLA/RCW-shared fields (`ufn`, `ecfFlag`, `laaReference`, `determinationId`, `applicationType` enum) that record-controlled-work doesn't need since it's scoped to a single work type.