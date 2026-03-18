# Requirements: AppSec Skills — Real-Time Vulnerability Feedback

**Defined:** 2026-03-18
**Core Value:** A developer sees a vulnerability flag and its fix before they move to the next task — not a week later in a PR comment.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Vulnerability Detection

- [ ] **VULN-01**: Skill detects injection vulnerabilities (OWASP A03) in Java and JavaScript/TypeScript code with context-specific explanation of why the specific code is dangerous
- [ ] **VULN-02**: Skill detects cryptographic failures (OWASP A02) in Java and JavaScript/TypeScript — weak ciphers, hardcoded keys, insecure random, HTTP vs HTTPS
- [ ] **VULN-03**: Each finding is mapped to its OWASP category with severity rating
- [ ] **VULN-04**: Findings name the specific variable, function, or code path at risk (not generic OWASP descriptions)
- [ ] **VULN-05**: False positive rate is acceptable — safe/valid patterns are not flagged (taint-analysis framing, not string matching)

### Remediation

- [ ] **REMED-01**: How-to-fix skill provides a plain-language explanation of the correct approach and why it's safer
- [ ] **REMED-02**: How-to-fix skill provides a working fixed code snippet the developer can apply directly
- [ ] **REMED-03**: Fixed code is specific to the language and library in use (not generic pseudo-code)
- [ ] **REMED-04**: Fix skill can be invoked on a specific finding from a prior check (finding number or description)

### Performance & Output

- [ ] **PERF-01**: Vulnerability-check skill completes in <5 seconds for a typical function/code block
- [ ] **PERF-02**: Output is concise — finding is ≤3 sentences; full response is scannable in <15 seconds
- [ ] **PERF-03**: Output format is consistent: severity → OWASP category tag → one-sentence finding → code location

### Claude Code Integration

- [ ] **CC-01**: Vulnerability-check is a Claude Code slash command (`/appsec:check`)
- [ ] **CC-02**: How-to-fix is a Claude Code slash command (`/appsec:fix`)
- [ ] **CC-03**: Commands accept code via file path argument (`$ARGUMENTS`) and can read the file directly
- [ ] **CC-04**: Commands are project-scoped (`.claude/commands/`) for team distribution via git

### GitHub Copilot Extension

- [ ] **COP-01**: Skills are packaged as a GitHub Copilot Extension installable by team members
- [ ] **COP-02**: Developer can invoke vulnerability-check on-demand via Copilot Chat
- [ ] **COP-03**: Vulnerability-check triggers automatically on Copilot code review (diff mode)
- [ ] **COP-04**: Code review trigger sends containing function as context (not raw diff lines) to avoid false positives from missing scope

### Distribution & Quality

- [ ] **DIST-01**: Skills are distributable as a team standard — documented, version-controlled, reliable
- [ ] **DIST-02**: Smoke test suite validates the skills work correctly in a clean-clone environment
- [ ] **DIST-03**: OWASP coverage map documents which categories are covered, partial, or not detectable — no false advertising

## v2 Requirements

Deferred to future release.

### Language Expansion

- **LANG-01**: C# / .NET vulnerability detection (A02 + A03 with EF Core / ADO.NET patterns)
- **LANG-02**: Kotlin vulnerability detection (A02 + A03 with Android-specific patterns)
- **LANG-03**: Swift vulnerability detection (A02 + A03 with iOS-specific patterns — extra conservatism on pattern confidence)

### Additional OWASP Coverage

- **OWASP-01**: A07 — Auth & Credentials (hardcoded secrets, weak session management) — add once A02/A03 are validated
- **OWASP-02**: A01 — Broken Access Control — defer until framework context detection is reliable enough to avoid false positives

### Advanced Features

- **ADV-01**: PR-level summary report (aggregate findings across all changed files)
- **ADV-02**: Suppression comments — developer can mark a false positive to suppress future flags

## Out of Scope

| Feature | Reason |
|---------|--------|
| Full CI/CD SAST pipeline | These are developer-time tools, not pipeline gates — different problem |
| Auto-apply fixes | Developer must always review — no auto-remediation in any version |
| Custom rule authoring | Baked-in OWASP rules for v1; configurability is a v3+ concern |
| A04 (Insecure Design) | Not detectable from code snippets by design |
| A06 (Vulnerable Components) | Requires dependency manifest analysis, not code scanning |
| Real-time keystroke analysis | Too invasive, too noisy — trigger on request/review only |
| Mobile binary analysis | Static source analysis only; no APK/IPA inspection |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| VULN-01 | Phase 1 | Pending |
| VULN-02 | Phase 1 | Pending |
| VULN-03 | Phase 1 | Pending |
| VULN-04 | Phase 1 | Pending |
| VULN-05 | Phase 1 | Pending |
| REMED-01 | Phase 2 | Pending |
| REMED-02 | Phase 2 | Pending |
| REMED-03 | Phase 2 | Pending |
| REMED-04 | Phase 2 | Pending |
| PERF-01 | Phase 1 | Pending |
| PERF-02 | Phase 1 | Pending |
| PERF-03 | Phase 1 | Pending |
| CC-01 | Phase 1 | Pending |
| CC-02 | Phase 2 | Pending |
| CC-03 | Phase 1 | Pending |
| CC-04 | Phase 3 | Pending |
| COP-01 | Phase 4 | Pending |
| COP-02 | Phase 4 | Pending |
| COP-03 | Phase 4 | Pending |
| COP-04 | Phase 4 | Pending |
| DIST-01 | Phase 3 | Pending |
| DIST-02 | Phase 3 | Pending |
| DIST-03 | Phase 1 | Pending |

**Coverage:**
- v1 requirements: 23 total
- Mapped to phases: 23
- Unmapped: 0 ✓

---
*Requirements defined: 2026-03-18*
*Last updated: 2026-03-18 after roadmap creation*
