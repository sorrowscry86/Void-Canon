# Development image service

Authenticated Cloudflare Worker + AI binding + D1 ledger. Only FLUX.2 Klein 4B
is used. No account-level Cloudflare key is distributed to Android.

- GET /health: non-sensitive version check.
- Authenticated GET /v1/status: UTC day, reserved budget, 5,000 limit, model.
- Authenticated POST /v1/images: JSON requestId, prompt, size, optional reference.
  Sizes: portrait (512²), large (1024²), background (1024×576). Prompt: 1–2,000
  characters. Reference: base64 PNG, at most 448×448. Request ceiling: 700,000 bytes.
  Reply: base64 image, MIME, fixed model, matching request ID, reserved estimate.
- Authorization: Bearer scoped-device-code. Errors are bounded JSON messages.

Accepted requests reserve ceil(26.05 × output tiles + 5.37 × input tiles),
rounding 512-pixel tiles up on each axis. The insert trigger in schema.sql
enforces the budget atomically; failed/timed-out work retains its reservation.
A duplicate insert rolls back without spending or generating again.
The ledger stores IDs/dates/status/reservations, not prompts/images.

The budget is not an account billing cap. Cloudflare's 10,000-neuron daily free
allocation is shared across all account AI activity. No plan was upgraded.
This authenticated endpoint is for private development, not a public service.

## Verify

Node 24.22+, npm ci, npm test. Tests use the production module in workerd,
real SQLite D1, and a fixture AI binding. The pinned Miniflare V5 beta provides
a published V4-options adapter used by the harness. Loopback permission is needed.

## Deploy and maintain

deployment.json records public resource identity and compatibility date.
Upload worker.js as an ES module through Cloudflare's multipart Worker API:
main_module worker.js; AI binding AI; D1 binding DB using the recorded UUID;
secret-text binding APP_ACCESS_TOKEN generated using secure random bytes.
Initialize the database with schema.sql. Enable logs and traces; workers.dev
is enabled and preview URLs are disabled. Code logs no prompts, images or keys.

Use the connected Cloudflare tools or official cf CLI. No Wrangler configuration
is needed here. On redeploy, preserve existing secret-text bindings with
keep_bindings unless intentionally rotating the code. Do not write secrets in
source or deployment.json. Keep Android connection files outside the repository;
their only fields are endpoint and accessToken. Anyone holding one can access
this bounded service. Disconnect removes a code locally; rotating the Worker
secret revokes all copies.

Endpoint: https://void-canon-dev-images.sorrowscry86.workers.dev.
This execution environment's destination policy blocks that hostname. Local
runtime and API deployment checks do not prove a live public HTTP request.
Allow that exact host for verification here; Android uses ordinary HTTPS.
