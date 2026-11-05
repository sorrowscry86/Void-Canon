# Cloudflare image increment verification

- [Android build](android-build.txt): preview/debug APKs, JVM tests, lint and instrumentation APK assembly passed.
- [JVM results](jvm-results.txt): **59 tests, no failures/errors**. Includes HTTPS request privacy, cancellation/no retry, response bounds/model/request identity, preview persistence, reference resizing, owner/baseline preservation, duplicate save and backup provenance.
- [Android lint](android-lint.txt): no issues found.
- [Workers runtime tests](server-tests.txt): **4 passed**, using workerd/Miniflare, real local D1, and a fixture AI binding. Authentication/validation, multipart reference handling, replay protection, failure reservations and concurrent budget enforcement are covered.
- [Android fixture walkthrough](android-fixture.txt): execution in progress; final status is recorded below when available. The fixture uses a local HTTPS service and solid-color image, not live AI.

Deployment checks confirmed Worker upload, AI/D1/secret bindings, initialized D1 schema, workers.dev activation, and enabled logs/traces. The new public host is blocked by this workspace's destination policy: `Forbidden: requests to void-canon-dev-images.sorrowscry86.workers.dev are not allowed`. A live Android → deployed Worker → AI round trip is not claimed. Earlier live model samples were a separate lab evaluation.

The final 0.4.2 preview is signed with the existing 0.4.1 preview certificate, SHA-256 `be36b09130f79882b6a032b7cbfc8c0515737952dac493d23c71cbbb7265a4c2`. The original production package remains separate. Device/emulator results do not establish physical-phone performance.

The emulator required reclaiming duplicate dependency/Gradle caches before startup; original AVD user data was retained. The fixture selects the existing demo world for reproducibility without deleting worlds or transcripts.
