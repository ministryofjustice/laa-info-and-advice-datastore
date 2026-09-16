# API Requests

Based on the generated API clients under `src/api/clients`, the app talks to two external APIs. All RCW requests are JSON (`Content-Type: application/json`) with an `Authorization: Bearer <token>` header (Entra-acquired, or a mock token in test mode); PDA requests use an `X-Authorization` API key header instead.

## RCW (Record Controlled Work) API

Base URL: `config.api.rcw.baseUrl`, defined in `src/api/clients/rcw/schema/applications/applications.gen.ts`

| Request | Endpoint | Data sent |
|---|---|---|
| `getApplications` | `GET /api/v1/applications` | Query params only: `page`, `size`, `officeId?`, `status?` (`DRAFT`/`COMPLETED`), `eligibilityIndication?` (`eligible`/`ineligible`) — see `getApplicationsParams.zod.gen.ts` |
| `getApplication` | `GET /api/v1/applications/{id}` | No body — just the application `id` in the path |
| `createApplication` | `POST /api/v1/applications` | Body (`createApplicationRequestBody.zod.gen.ts`): `legalAidBefore`, `legalAidLast6Months?`, `reasonForReapplication?`, `providerOfficeCode`, `scopingQuestions` (arbitrary map), `clientDetails` (`firstName`, `lastName`, `dateOfBirth`, `niNumber?`, `hasFixedAddress`, `address?` with address lines/town/postcode/county/country) |
| `updateApplicationEvidence` | `PUT /api/v1/applications/{id}/evidence` | Body (`updateEvidenceRequestBody.zod.gen.ts`): `evidenceExemptionCode?`, `evidenceExemptionReason?`, `incomeEvidenceChecklist?`, `expenditureCapitalEvidenceChecklist?` (arbitrary objects) |
| `updateApplicationMeans` | `PUT /api/v1/applications/{id}/means` | Body (`updateMeansDataRequestBody.zod.gen.ts`): `data` and `result` — arbitrary objects holding the eligibility/means assessment answers and outcome (used by `eligibility.service.ts`) |
| `updateApplicationDeclaration` | `PUT /api/v1/applications/{id}/declaration` | Body (`updateDeclarationRequestBody.zod.gen.ts`): `declarationConfirmation` (boolean), `dateSigned` (date) |
| `updateApplicationStatus` | `PATCH /api/v1/applications/{id}/status` | Body (`updateApplicationStatusRequestBody.zod.gen.ts`): `applicationState` (`DRAFT`/`COMPLETED`), `eTag` (int, for optimistic concurrency) |

Auth for all of these is built by `getRcwApiDefaultOptions.ts`: it acquires a downstream Entra access token for the current session/user (`homeAccountId`/`sessionId`) and sets it as `Authorization: Bearer <token>`, or uses a fixed `test-access-token` when `config.api.useMockAccessToken` is set.

## PDA (Provider Details API)

Base URL: `config.api.pda.baseUrl`, defined in `src/api/clients/pda/schema/provider-firms-endpoints/provider-firms-endpoints.gen.ts`

| Request | Endpoint | Data sent |
|---|---|---|
| `getAllProviderOffices` | `GET /provider-firms/{firmId}/provider-offices` | No body — just `firmId` in the path |

Headers, from `getPdaApiDefaultOptions.ts`: `X-Authorization: <config.api.pda.key>` (static API key) and an optional `X-Correlation-Id`.