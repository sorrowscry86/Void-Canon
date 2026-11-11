# Story response compatibility — 0.6.2

## Publication contract

All live roleplay providers use the same `StoryResponse` policy in Classic, Freeform and AURPG. The JSON state protocol is a preferred format, not a prerequisite for a usable story turn. Neither OpenRouter nor direct DeepSeek is forced into an API JSON response mode. Summaries and vision retain their independent plain-text handling.

1. Unwrap a complete whole-response Markdown fence (`json`, `text`, or no language; JSON is case-insensitive; CRLF is accepted). Remove complete leading `<think>` blocks without publishing their contents. Unfinished or embedded reasoning cannot become story text.
2. Try the full existing `Continuity.resolve` protocol and all its semantic/record checks. If valid, atomically save narrative, validated snapshot/revision/event, action and completed audit.
3. Otherwise accept plain prose, a JSON string, a top-level string under `narrative`, `story`, `response`, `text` or `content`, or those same fields under a `message` object. Select the first nonblank supported string; do not guess story from actor/evidence/tool fields or JSON arrays.
4. Save the usable narrative and action exactly once, consume the matching draft, and mark the generation completed with the nonfatal warning: **Story saved; tracked state unchanged. Review scene continuity if needed.** Keep the exact raw response, provider/model and usage in local diagnostics. Do not apply any part of the rejected state update or optional visual payload. Native dice, momentum, clocks, canon and author notes remain unchanged.

The story is provisional, as with other live RP. The Play footer and Context diagnostics retain the warning after restart/export/restore. When that turn is the latest narrative, the next prompt explains that tracked state is the last validated snapshot and must not silently override the recent saved prose. The author can review/correct continuity through Scene details. A later fully validated state reply resumes normal state updates.

## Limits

Narrative must be nonblank and at most 12,000 characters. Empty/missing story, unclosed JSON, arrays, non-story literals, raw malformed protocol structures, unfinished reasoning and oversized text are rejected without consuming the draft or publishing a turn. The app does not heuristically fabricate missing quotes/braces or state fields. Provider errors, blocked permissions, output-budget termination and changed-scene fingerprints keep their existing failure behavior. Saving a narrative from invalid state does not permit applying stale responses to changed scenes.

No repair call, automatic resend, extra model, price change or provider switch is introduced. Existing rejected turns stay in diagnostics and are not retroactively inserted; send explicitly after upgrading. JSON syntax/quoted evidence never guarantees model correctness. This increment improves compatibility without introducing a second semantic checker or changing world canon.

Room/backup remain 6. The existing completed generation's `error` text stores this warning; messages retain normal generation references and `live` origin. Strict state parsing remains available for valid protocol replies and state corrections. Provider credentials are excluded from portable backups.
