# AppSec Skills — Real-Time Vulnerability Feedback

## What This Is

Two lean skills (vulnerability-check and how-to-fix) that give developers AppSec feedback at the moment they write code — not at PR review or end-of-pipeline. They work with GitHub Copilot hooks: triggering automatically on code review and on-demand via Copilot Chat. Targets team-wide adoption across a Java, C#, JavaScript/TypeScript, Kotlin, and Swift codebase.

## Core Value

A developer sees a vulnerability flag and its fix before they move to the next task — not a week later in a PR comment.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] vulnerability-check skill: pattern-match dangerous code + map to OWASP/CWE + explain why it's dangerous in this specific context
- [ ] how-to-fix skill: explain the correct approach + provide a working fixed code snippet
- [ ] Skills trigger automatically on Copilot code review (async/diff mode)
- [ ] Skills available on-demand via Copilot Chat (@appsec or equivalent invocation)
- [ ] Coverage: Java, C#, JavaScript/TypeScript, Kotlin, Swift
- [ ] Output is short and fast — feels like a quick IDE hint, not a security report (<5s, concise)
- [ ] Skills are distributable as a team standard (documented, reliable)

### Out of Scope

- Full SAST pipeline integration — these are in-editor skills, not CI/CD gates
- Exhaustive rule sets — v1 POC covers high-signal patterns (OWASP Top 10 focus), not every CVE
- Auto-remediation / auto-apply fixes — developer always reviews and accepts the fix
- Custom rule authoring — rules are baked in, not configurable in v1

## Context

- Shift-left AppSec: feedback arrives at write-time, not review-time
- Team has identified late feedback as the core pain — developers have context-switched away from code by the time security issues are flagged
- Integration model is still open: could be Claude Code skills (markdown, invoked via /skill-name), Copilot-native extensions, or both — v1 POC will validate which feels right
- "Lean and fast" is a hard constraint: both concise output and fast response time matter; full security reports are the anti-pattern

## Constraints

- **Response time**: <5 seconds end-to-end — skills must not interrupt coding flow
- **Output length**: Concise — findings and fixes should be scannable in 10-15 seconds
- **Language coverage**: Java, C#, JavaScript/TypeScript, Kotlin, Swift — no other languages in v1
- **Scope**: v1 is a POC to validate the concept with the team, not a production deployment

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Two separate skills (check + fix) | Keeps each skill focused and fast; developer can invoke fix only when they want it | — Pending |
| Integration format (Claude Code skills vs Copilot extension) | Still open — POC will validate which delivery mechanism fits team workflow | — Pending |
| OWASP Top 10 as v1 vulnerability scope | High-signal, well-understood, covers the most common issues across all target languages | — Pending |

---
*Last updated: 2026-03-18 after initialization*
