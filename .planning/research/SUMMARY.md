# Project Research Summary

**Project:** AppSec Skills — Real-Time Vulnerability Feedback (Claude Code + GitHub Copilot)
**Domain:** LLM-powered in-editor AppSec developer tooling
**Researched:** 2026-03-18
**Confidence:** MEDIUM (stack/architecture verified from live examples; Copilot extension details from training data)

## Executive Summary

This project builds AI-powered security skills that give developers real-time vulnerability feedback while writing code, targeting Java, C#, JavaScript/TypeScript, Kotlin, and Swift across OWASP Top 10 patterns. The research consensus is clear: start with Claude Code custom slash commands (zero infrastructure, hours to first prototype), validate prompt quality with real team usage, then wrap the same prompts in a GitHub Copilot extension only if justified by adoption data. The intellectual work — the vulnerability detection prompts and OWASP pattern catalogue — is the same regardless of delivery mechanism, so the POC de-risks the hard part first.

The dominant lesson from existing tools (SonarLint, Snyk, Semgrep, GHAS) is that developer adoption lives or dies on two factors: false positive rate and output scannability. Tools with greater than 20% false positive rates get disabled within 30 days. Tools that generate wall-of-text reports get ignored immediately. The architecture must enforce a two-skill design (check reports findings only, fix generates remediation on demand) with a strict output format cap and per-language safe-pattern lists baked into the prompt.

The highest risks are: (1) false positives from pattern-match prompts that lack taint-analysis framing, (2) context window mismanagement causing either slow responses or inaccurate findings from insufficient code context, and (3) over-claiming OWASP coverage for categories that are structurally undetectable from code snippets (A04, A06). Mitigating all three requires treating the prompt and test suite as the primary engineering artifacts, not the delivery infrastructure.

---

## Key Findings

### Recommended Stack

Start with Claude Code custom slash commands in `.claude/commands/`. These are plain markdown files with YAML frontmatter that define `/appsec:check` and `/appsec:fix` invocations — no server, no deployment, no API keys beyond what Claude Code already provides. The canonical prompts live in `skills/prompts/` and are referenced by both the Claude Code commands and (later) a Copilot extension HTTP endpoint. This single-source-of-truth structure is the key architectural principle.

For detection, use LLM-native pattern recognition — not a SAST engine. Claude's code understanding is sufficient for OWASP Top 10 patterns when given well-structured prompts with explicit per-language safe and dangerous pattern lists. Semgrep is a viable pre-pass enhancement if a Copilot extension server is built (single-file scan in under 500ms), but it is not needed for the POC path. CodeQL is explicitly excluded — it requires a full compiled build and takes minutes to hours.

**Core technologies:**
- Claude Code slash commands (`.claude/commands/`): Primary skill delivery — zero infrastructure, version-controlled, team-distributable via git pull
- Markdown prompt files (`skills/prompts/`): Canonical skill definition — the entire skill is the prompt; portable to both delivery mechanisms
- `.github/copilot-instructions.md`: Lightweight Copilot context injection — stable, zero setup, usable immediately as a complement
- GitHub Copilot Extension / skillset (Phase 2): Secondary delivery for Copilot-native users — same prompts wrapped in an HTTP endpoint (Node.js/TypeScript + Vercel or Lambda)
- Semgrep (`p/owasp-top-ten` pack, optional): SAST pre-pass for Copilot extension server only — fast on single files, free OSS tier

### Expected Features

The single most common failure mode of in-editor security tools is being too generic. Context-specific findings (naming the exact variable and data flow) and language-appropriate copy-paste fixes are the primary AI differentiators over SonarLint and Semgrep. Everything else is secondary.

**Must have (table stakes):**
- Finding pinpointed to specific line with exact code quoted — any tool that doesn't locate the precise line is ignored
- Explanation of WHY this specific instance is dangerous — "SQL injection risk" without context is noise
- OWASP category + CWE identifier — teams need this vocabulary to triage and communicate with AppSec
- Language-appropriate fix guidance — generic advice signals the tool doesn't understand the codebase
- Response under 5 seconds — harder than it sounds; must be a hard gate, not a soft target
- Scannable output format (under 15 seconds to read and act on) — enforced by explicit output template in prompt

**Should have (competitive differentiators):**
- Working copy-paste fixed code snippet for the actual code shown — primary AI advantage over SonarLint/Semgrep which offer only generic examples
- Context-specific explanation naming the variable and data flow path — the gap no existing tool fills
- On-demand chat invocation (`/appsec:check`) without configuration setup — zero-friction first run
- Severity framing in business impact terms ("attacker can read any user's session token") not abstract levels
- Consistent multi-language handling from a single prompt-based interface — polyglot coverage is a free differentiator

**Defer (v2+):**
- A01 (Broken Access Control) structural patterns — high false positive risk without framework context; add after v1 validation
- Taint flow visualization — valuable but complex; v1 describes flow in prose
- Dependency vulnerability scanning — Snyk owns this well; out of scope
- Custom rule authoring — explicitly out of scope per project requirements
- Auto-apply fixes — explicitly out of scope per project requirements
- Copilot PR diff trigger (automatic, not on-demand) — requires the extension server to be built and stable first

**v1 OWASP focus (high signal, clear fixes, testable):**
- A03 Injection first: SQL injection across all 5 languages + command injection — highest prevalence, clearest fixes
- A07 Hardcoded credentials: universal across all languages, zero ambiguity, immediate developer value
- A02 Cryptographic failures: weak algorithms (MD5/SHA1/DES), detectable by import/API name
- A08 Insecure deserialization: Java ObjectInputStream, C# BinaryFormatter — high value, often missed by other tools

**Explicitly exclude from v1 coverage claims:**
- A04 (Insecure Design) — not detectable from code snippets
- A06 (Vulnerable and Outdated Components) — requires dependency manifest analysis, not code analysis
- A01 IDOR — requires business logic context unavailable in a single file

### Architecture Approach

Two separate skills with a clear division of responsibility: `check` detects and classifies (fast, minimal output, no code generation), and `fix` generates the corrected code snippet on demand for a specific finding. They share conversation context — Claude Code maintains the check output in session, so fix reads from conversation history rather than external state. This stateless design eliminates complexity while meeting the <5s constraint.

Prompt structure follows a four-layer pattern: role + constraint layer, OWASP scope block (language-keyed pattern table, ~400 tokens per language), code under review (capped at 300 lines), and output format specification. The OWASP pattern table encodes concrete code-level patterns ("look for X in Y context") rather than OWASP narrative descriptions — the latter are written for humans, not LLMs.

**Major components:**
1. `vulnerability-check` skill (`.claude/commands/appsec/check.md`) — Detect and classify vulnerabilities from code; inputs: file path or pasted code; outputs: structured findings list with OWASP/CWE refs
2. `how-to-fix` skill (`.claude/commands/appsec/fix.md`) — Explain and generate fix for one specific finding; inputs: finding reference + original code from conversation context; outputs: fixed code block + one-sentence verification guidance
3. OWASP prompt library (`skills/patterns/owasp-top10.md` + per-language files) — Language-keyed pattern table injected into skill prompts; encodes safe patterns (do not flag) and dangerous patterns (flag) per language
4. Context scoper (inline prompt logic) — Trims input to containing function body, caps at 150-300 lines, handles oversized files gracefully with user notification

### Critical Pitfalls

1. **High false positive rate destroying developer trust** — Use taint-analysis framing ("does user-controlled input reach this sink?"), include explicit safe pattern lists per language, add confidence threshold to prompt output. Build 10 safe + 10 dangerous test cases per language before distribution. One false positive on a known-safe pattern can kill adoption permanently.

2. **Verbose output that gets ignored** — Enforce an exact output template in the system prompt with a hard word cap (under 100 words for check). Test output length as a POC metric. The check skill must never include background, "what is X", or additional resources sections.

3. **Context window mismanagement (too much or too little)** — Send the containing function body, not raw diff lines and not the whole file. Cap at 150-300 lines. Instrument token count during POC — if p95 exceeds 2,000 tokens for typical files, tighten the context strategy. Too little context causes false positives; too much breaks the 5-second SLA.

4. **Language-specific pattern blind spots** — Safe framework APIs (PreparedStatement, Entity Framework parameterized queries, React JSX, GRDB predicate APIs) must be listed explicitly in the prompt as safe — the model will flag them otherwise. Kotlin and Swift are underrepresented in security training data; default to "cannot determine" for ambiguous patterns and document the limitation.

5. **OWASP coverage drift — claiming more than is tested** — Coverage claims must lag behind test coverage, never lead it. Map each OWASP category to covered/partial/not-covered before writing documentation. Do not claim A04 or A06 coverage in v1 — they are structurally impossible for a code-snippet tool.

---

## Implications for Roadmap

Research strongly implies a prompt-first, infrastructure-last build order. The prompt quality determines the tool's value; infrastructure only determines its delivery. Validate the prompt before building the server.

### Phase 1: Prompt Foundation and Check Skill

**Rationale:** The OWASP pattern catalogue and check skill prompt are the intellectual core of the entire project. Everything else depends on getting this right. Claude Code slash commands have zero infrastructure cost, so this phase has no deployment risk — just prompt engineering and testing.

**Delivers:** Working `/appsec:check` command for Java and JavaScript/TypeScript (the two highest-prevalence, best-documented target languages). OWASP pattern table for those two languages. Test suite with 10 safe + 10 dangerous patterns per language.

**Addresses:** Table-stakes features (line-specific finding, context-specific explanation, OWASP identifier, scannable format). A03 Injection and A07 Hardcoded Credentials coverage.

**Avoids:** False positive pitfall (per-language safe pattern lists built from day one), verbose output pitfall (output template enforced before any distribution), coverage over-claim pitfall (only claim what test cases prove).

**Research flag:** Standard patterns — prompt engineering for LLM security tools is well-documented. No deeper research needed before starting.

---

### Phase 2: Fix Skill and Full Language Coverage

**Rationale:** The fix skill builds directly on Phase 1 findings. Extending language coverage to C#, Kotlin, and Swift follows the same pattern as Phase 1 but requires per-language safe pattern research (especially for Kotlin and Swift, which are underrepresented in security tooling). No new infrastructure.

**Delivers:** Working `/appsec:fix` command. Full language coverage for all five target languages. A02 (Crypto) and A08 (Insecure Deserialization) pattern coverage added. Combined speed + accuracy benchmark completed.

**Uses:** Claude Code slash commands, extended OWASP pattern table, conversation context for check → fix handoff.

**Implements:** Two-skill architecture, context scoper (300-line cap with user notification), prompt injection mitigation (code wrapped in explicit delimiters).

**Avoids:** Language-specific blind spots pitfall (Kotlin/Swift safe pattern lists built before declaring coverage), speed/accuracy tradeoff pitfall (combined benchmark required before Phase 2 is declared done).

**Research flag:** Needs validation for Kotlin and Swift — fewer community resources, less security tooling coverage. Validate language-specific safe/dangerous patterns against current OWASP Mobile Security Testing Guide before building Kotlin/Swift pattern tables.

---

### Phase 3: Distribution, Smoke Tests, and Team Rollout

**Rationale:** A skill that works for the author and breaks for teammates is not a team standard. Distribution quality is a first-class concern. This phase treats packaging and distribution as engineering work, not documentation afterthought.

**Delivers:** Smoke test procedure (5-minute verification any teammate can run). Version-controlled prompts with changelog. Distribution documentation. Subscription tier compatibility confirmed. Claude Code skill tested on a clean repo clone.

**Implements:** Skill distribution best practices — relative paths, no environment-specific assumptions, accepted by multiple team members on their actual machines.

**Avoids:** Skill distribution failures pitfall (environment assumptions, tier incompatibilities, missing smoke tests).

**Research flag:** Standard patterns for team tool distribution. No deeper research needed.

---

### Phase 4: GitHub Copilot Extension (If Adoption Validates)

**Rationale:** Only build server infrastructure after the prompt is validated by real team usage. The Copilot extension wraps the same prompt logic from Phases 1-2 in an HTTP endpoint. This phase is conditional — skip it if the team is exclusively on Claude Code.

**Delivers:** HTTP skillset endpoint (Vercel serverless function or Lambda), GitHub App registration, `@appsec` invocation in Copilot Chat. Same check and fix functionality available to Copilot users.

**Uses:** Node.js/TypeScript, `@octokit/webhooks`, `express` or `hono`, `zod` for payload validation. Canonical prompts from `skills/prompts/` — no prompt rewrite needed.

**Implements:** Copilot Extension skillset architecture, SSE streaming response, HTTPS endpoint with GitHub payload verification.

**Avoids:** Building infrastructure before prompt validation (the anti-pattern the architecture research explicitly warns against).

**Research flag:** Needs API verification — Copilot Extensions API details are from training data (Aug 2025 cutoff). Verify current skillset manifest format, `currentFile` context availability, and org-level rate limits against current GitHub docs before starting this phase.

---

### Phase 5: Copilot Code Review Diff Mode (PR Trigger)

**Rationale:** Diff mode is the highest-signal trigger (right before a PR merges) but requires the extension from Phase 4 to be stable. It also requires a prompt variant tuned for diff context rather than whole-file context — extracting the containing function from raw diff lines is the key engineering challenge here.

**Delivers:** Automatic vulnerability check triggered on PR open/update. Diff-aware prompt variant that extracts function context from diff chunks rather than scanning raw +/- lines.

**Implements:** PR trigger configuration, diff context extraction logic (not raw diff lines — must include the containing function body for taint analysis to work).

**Avoids:** Context window mismanagement in diff mode (the specific failure mode of passing raw diff lines rather than structured function context).

**Research flag:** Needs deeper research — Copilot code review hook configuration and what diff context is available in the extension payload are areas of active development with low confidence in current documentation. Verify before starting.

---

### Phase Ordering Rationale

- Prompt before infrastructure: The OWASP pattern catalogue is the value; the HTTP server is just transport. Building the server before the prompt is validated wastes effort and creates rework risk.
- Check before fix: Fix depends on check output. Check must be stable and producing accurate findings before fix is worth building.
- Two languages before five: Java and JavaScript have the best-documented security patterns and highest adoption. Proving the model works well for them de-risks the Kotlin/Swift extension.
- Claude Code before Copilot extension: Slash commands ship in hours; a GitHub App takes days. Validate the concept first, build the infrastructure only if warranted.
- On-demand before automatic: Manual invocation (`/appsec:check`) is predictable and testable. Automatic PR triggers (diff mode) have more failure modes and require the extension to be stable first.

### Research Flags

Phases needing deeper research during planning:
- **Phase 2 (Kotlin/Swift):** Underrepresented in security tooling documentation. Validate language-specific patterns against OWASP Mobile Security Testing Guide (MSTG) before building pattern tables.
- **Phase 4 (Copilot Extension):** API details from training data. Verify current skillset manifest format, context payload structure, and org subscription requirements against current GitHub Copilot Extensions docs before any code.
- **Phase 5 (Diff Mode):** Lowest confidence area. What diff context the extension receives, how to extract function bodies from diff chunks, and current PR trigger configuration should all be validated against live GitHub docs.

Phases with standard patterns (skip research-phase):
- **Phase 1 (Prompt Foundation):** LLM security prompt engineering is well-documented. Four-layer prompt structure and language-keyed pattern tables are established patterns. Iterate directly.
- **Phase 3 (Distribution):** Team tool distribution is standard engineering practice. Smoke test and version control patterns don't require research.

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | MEDIUM | Claude Code slash command format verified from live local examples (HIGH). Copilot Extension API details from training data cutoff Aug 2025 — verify current docs before Phase 4 (MEDIUM). Semgrep pack names should be confirmed at implementation time (MEDIUM). |
| Features | HIGH | OWASP Top 10 (2021) is a stable published standard. Competitive analysis of SonarLint, Snyk, GHAS, Semgrep based on direct training knowledge through Aug 2025. Developer adoption behavioral research consistent with published industry surveys. |
| Architecture | HIGH for Claude Code path; MEDIUM for Copilot path | Slash command format, frontmatter fields, $ARGUMENTS, and allowed-tools verified from live Claude Code command files. Copilot skillset manifest format and payload structure from training data — may have evolved since Aug 2025. |
| Pitfalls | HIGH | False positive dynamics, output verbosity, context window management, language-specific API confusion, and OWASP coverage gaps are all well-documented in security tooling literature and directly observable LLM behavior. Copilot extension distribution details are MEDIUM — tier requirements should be verified. |

**Overall confidence:** MEDIUM-HIGH. The core approach (prompt-first, Claude Code slash commands, two-skill architecture, LLM-native detection) is well-grounded. The Copilot extension delivery path has meaningful uncertainty that requires live doc verification before Phase 4.

### Gaps to Address

- **Exact Claude Code slash command syntax:** Verify `$ARGUMENTS` behavior, `allowed-tools` list, and any new frontmatter fields against current Claude Code documentation before finalizing Phase 1 prompt files. (Confidence: MEDIUM — known from live examples but worth confirming current docs.)
- **Copilot skillset manifest format:** The exact YAML structure for defining Copilot skillset skills may have changed since Aug 2025. Verify against current GitHub Copilot Extensions docs before starting Phase 4.
- **`currentFile` context in Copilot extensions:** Whether Copilot automatically provides the currently open file in the extension payload, or whether users must explicitly use `#file`, is unclear. This affects the UX design of Phase 4.
- **Copilot subscription tier requirements:** Confirm which Copilot tiers (Individual, Business, Enterprise) support custom extensions and skillsets before committing to the Phase 4 delivery mechanism.
- **Kotlin/Swift pattern coverage:** Fewer security tooling resources exist for these languages. Build and validate per-language safe/dangerous pattern lists against OWASP MSTG before declaring coverage.
- **Semgrep pack names:** The `p/owasp-top-ten` pack name and Kotlin/Swift rule coverage should be verified against `semgrep --list-packs` or `registry.semgrep.dev` at implementation time.

---

## Sources

### Primary (HIGH confidence)
- Live Claude Code command files at `~/.claude/commands/gsd/` — exact slash command format, frontmatter fields, XML instruction sections, `$ARGUMENTS`, `allowed-tools`
- OWASP Top 10 (2021) standard — category definitions, sub-category breakdown, CWE mappings
- SonarLint, Snyk IDE, Semgrep VS Code, GHAS/CodeQL, Copilot Autofix — tool behavior and competitive analysis (direct training knowledge through Aug 2025)

### Secondary (MEDIUM confidence)
- Anthropic Claude Code documentation (training knowledge, Aug 2025 cutoff) — context injection mechanisms, `@` references, project vs global scope
- GitHub Copilot Extensions documentation (training knowledge, Aug 2025 cutoff) — skillset vs extension model, payload structure, code review mode
- LLM security prompt engineering patterns (training knowledge) — four-layer prompt structure, context window management rules
- Industry developer security adoption research (Snyk State of Developer Security, SANS AppSec surveys) — false positive thresholds, disable rates, adoption patterns

### Tertiary (LOW confidence — verify before implementation)
- Copilot code review hook configuration — active development area, limited public API as of Aug 2025
- Semgrep Kotlin/Swift rule coverage — fewer community rules; test against sample code before depending on it
- Copilot subscription tier requirements for custom extensions — may have changed; verify against current GitHub docs

### Verify Before Starting
- https://docs.anthropic.com/en/docs/claude-code/slash-commands — current slash command format
- https://docs.github.com/en/copilot/building-copilot-extensions — current Extensions/skillset API
- https://docs.github.com/en/copilot/customizing-copilot/adding-repository-custom-instructions-for-github-copilot — `copilot-instructions.md`
- https://semgrep.dev/docs — current OWASP pack names and language support
- https://mas.owasp.org/MASTG/ — OWASP Mobile Application Security Testing Guide (Kotlin/Swift patterns)

---
*Research completed: 2026-03-18*
*Ready for roadmap: yes*
