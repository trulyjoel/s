# Roadmap: AppSec Skills — Real-Time Vulnerability Feedback

## Overview

Four phases, prompt-first, infrastructure-last. The intellectual work — the OWASP detection prompts and per-language pattern catalogue — is built and validated before any server infrastructure is touched. Phase 1 ships a working Claude Code check command for Java and JavaScript/TypeScript. Phase 2 adds the fix command and expands to all five languages. Phase 3 packages both skills as a team standard with smoke tests. Phase 4 wraps the validated prompts in a GitHub Copilot Extension for Copilot-native users and adds automatic PR diff triggering.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Check Skill + Prompt Foundation** - Working `/appsec:check` command for Java and JavaScript/TypeScript via Claude Code slash command
- [ ] **Phase 2: Fix Skill + Full Language Coverage** - Working `/appsec:fix` command plus vulnerability detection extended to C#, Kotlin, and Swift
- [ ] **Phase 3: Distribution and Team Standard** - Skills packaged for team adoption — version-controlled, smoke-tested, documented, and distributable via git
- [ ] **Phase 4: GitHub Copilot Extension** - Same prompts wrapped in a Copilot Extension with on-demand chat invocation and automatic PR diff triggering

## Phase Details

### Phase 1: Check Skill + Prompt Foundation
**Goal**: Developers can run a fast, accurate vulnerability check on Java or JavaScript/TypeScript code and get a concise, actionable finding in under 5 seconds
**Depends on**: Nothing (first phase)
**Requirements**: VULN-01, VULN-03, VULN-04, VULN-05, PERF-01, PERF-02, PERF-03, CC-01, CC-03, DIST-03
**Success Criteria** (what must be TRUE):
  1. Developer runs `/appsec:check path/to/file.java` and receives a finding that names the specific variable or function at risk — not generic OWASP text
  2. Each finding includes an OWASP category, severity, and a one-sentence explanation of why this specific code instance is dangerous
  3. A known-safe pattern (PreparedStatement, parameterized query) is not flagged — the skill does not produce false positives on canonical safe APIs
  4. The full response arrives in under 5 seconds and can be read and acted on in under 15 seconds
  5. A written OWASP coverage map exists stating exactly which categories are covered vs not covered — no false advertising
**Plans**: 2 plans
Plans:
- [ ] 01-01-PLAN.md — Build /appsec:check slash command with SQLi patterns and test fixtures
- [ ] 01-02-PLAN.md — Create OWASP coverage map and validate command end-to-end

### Phase 2: Fix Skill + Full Language Coverage
**Goal**: Developers can request a working fix for any finding and receive language-specific, copy-paste-ready corrected code; detection covers all five target languages
**Depends on**: Phase 1
**Requirements**: VULN-02, REMED-01, REMED-02, REMED-03, REMED-04, CC-02
**Success Criteria** (what must be TRUE):
  1. Developer runs `/appsec:fix 1` (or finding description) and receives a fixed code snippet specific to the language and library shown — not generic pseudocode
  2. The fix skill reads from conversation context — no need to re-paste the original code
  3. Vulnerability detection works on C#, Kotlin, and Swift code with the same finding quality as Phase 1 (specific variable named, OWASP category, severity)
  4. The fix command includes a one-sentence verification note so the developer knows what to confirm after applying it
**Plans**: TBD

### Phase 3: Distribution and Team Standard
**Goal**: Any team member can clone the repo, run the smoke test, and have both skills working in under 5 minutes — no per-machine setup beyond what Claude Code already requires
**Depends on**: Phase 2
**Requirements**: CC-04, DIST-01, DIST-02
**Success Criteria** (what must be TRUE):
  1. Both commands exist under `.claude/commands/` and are version-controlled — any team member gets them via `git pull`
  2. A smoke test procedure exists that a new team member can run in under 5 minutes and confirm both skills work correctly
  3. Skills work correctly in a clean-clone environment with no author-specific paths, environment variables, or configuration
**Plans**: TBD

### Phase 4: GitHub Copilot Extension
**Goal**: Copilot users can invoke vulnerability-check on-demand via Copilot Chat and receive automatic checks when opening a PR — using the same prompt logic validated in Phases 1-2
**Depends on**: Phase 3
**Requirements**: COP-01, COP-02, COP-03, COP-04
**Success Criteria** (what must be TRUE):
  1. Team member installs the extension and can type `@appsec check` in Copilot Chat to receive a finding on the current file — same quality as the Claude Code command
  2. Opening or updating a PR automatically triggers a vulnerability check comment — no manual invocation needed
  3. The automatic PR check sends the containing function as context (not raw diff lines) so findings are accurate, not false positives from missing scope
**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Check Skill + Prompt Foundation | 0/2 | Planning complete | - |
| 2. Fix Skill + Full Language Coverage | 0/TBD | Not started | - |
| 3. Distribution and Team Standard | 0/TBD | Not started | - |
| 4. GitHub Copilot Extension | 0/TBD | Not started | - |
