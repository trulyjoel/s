# Phase 1: Check Skill + Prompt Foundation - Context

**Gathered:** 2026-03-18
**Status:** Ready for planning

<domain>
## Phase Boundary

Build the `/appsec:check` Claude Code slash command for Java and JavaScript/TypeScript. Developers run it against a file and get a specific, fast vulnerability finding. Fix command and additional languages are Phase 2. Team distribution and smoke tests are Phase 3.

</domain>

<decisions>
## Implementation Decisions

### POC spirit
- This is a proof of concept — validate the core idea, not a production tool
- Keep scope narrow, output simple, prompt lean
- Don't over-engineer: a working demo that impresses > a complex system that isn't done

### Detection scope
- **SQLi only for Phase 1** (OWASP A03, SQL injection)
- Not: command injection, path traversal, XSS, A02 crypto failures — those come later
- Rationale: narrowest scope that proves the concept cleanly; easy to demo, hard to argue with

### Zero-findings output
- **Show explicit clean message**, not silence
- Format: something like "No SQL injection vulnerabilities detected (OWASP A03)"
- Developer needs to know the skill ran and what it checked — silent pass is ambiguous

### Claude's Discretion
- Output format for findings (exact template, spacing, emoji or no emoji)
- Multi-finding ordering (by severity, by line number, etc.)
- File size handling strategy
- Language auto-detection from file extension vs. explicit arg
- Prompt engineering approach (chain-of-thought vs. direct output)
- Safe-pattern allowlist contents (PreparedStatement, parameterized queries, etc.)
- OWASP coverage map format and location in repo

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements
- `.planning/REQUIREMENTS.md` — VULN-01 through VULN-05, PERF-01 through PERF-03, CC-01, CC-03, DIST-03 define the acceptance criteria for this phase

### Research
- `.planning/research/SUMMARY.md` — Key findings on prompt design, false positive risks, and recommended architecture (Claude Code slash commands in `.claude/commands/`)
- `.planning/research/PITFALLS.md` — Known failure modes (pattern-match false positives, context window mismanagement, over-claiming coverage)
- `.planning/research/STACK.md` — Technology choices and rationale

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- None — greenfield project, no existing source code

### Established Patterns
- None yet — this phase establishes the pattern for Phase 2

### Integration Points
- `.claude/commands/` — where the slash command markdown file lives
- `.claude/settings.local.json` — project settings (already exists)

</code_context>

<specifics>
## Specific Ideas

- "Keep it short and sweet" — user explicitly wants a lean POC, not a thorough implementation
- Prove the high-level idea first; details come in later phases once the concept is validated

</specifics>

<deferred>
## Deferred Ideas

- A02 crypto failures (hardcoded secrets, weak ciphers) — Phase 2 or later
- Command injection, path traversal, XSS — later phases
- C#, Kotlin, Swift coverage — Phase 2
- Smoke test suite — Phase 3
- Team distribution setup — Phase 3
- GitHub Copilot Extension — Phase 4

</deferred>

---

*Phase: 01-check-skill-prompt-foundation*
*Context gathered: 2026-03-18*
