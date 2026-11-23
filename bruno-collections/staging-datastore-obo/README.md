# Datastore staging OBO

This separate collection obtains an RCW API user token interactively, exchanges it
for a datastore token using on-behalf-of (OBO), and makes a read-only datastore
authentication check. It does not change the local mock collection.

## Prerequisites

- Bruno for macOS at `/Applications/Bruno.app`, Python 3.9+, `kubectl` and the
  1Password CLI with desktop integration enabled.
- Kubernetes access to `rcw-api-env-secrets` in
  `laa-record-controlled-work-api-staging` and 1Password access to the
  `RCW-STAGING` and `RCW-API-STAGING` items in `LAA Record Controlled Work`.
- An authorised staging provider account. Its RCW API token must include
  `FIRM_CODE` and `LAA_ACCOUNTS`, and the apps must have the necessary delegated
  permissions and consent. The configured Bruno callback must be registered with
  the RCW staging app.

## Run

Close any existing Bruno instance, then from the repository root run:

```bash
python3 scripts/bruno-staging-obo.py
```

The launcher reads the staging Entra token URL and datastore scope from the
Kubernetes secret and resolves the client credentials from 1Password into the
Bruno process environment. It does not write those values to a file. Open
`bruno-collections/staging-datastore-obo` in Bruno and select **Staging**.

1. Open **1 Sign in**, select **Auth**, and click **Get Access Token**. Sign in
   as the staging provider; there is no need to send the request.
2. Send **2 Exchange token** to obtain the delegated datastore token.
3. Send **3 Check datastore auth**. A 404 for the nonexistent application
   confirms that authentication passed. No application is created or modified.

Repeat the exchange after signing in again or when a token expires. Tokens are
held in Bruno's OAuth cache and runtime variables; don't export or share request
logs, responses, or tokens. Close Bruno to clear runtime variables. The existing
`Applications/` tests create and update data and are not part of this collection.
