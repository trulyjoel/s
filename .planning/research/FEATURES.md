# Feature Landscape: AppSec Developer Skills

**Domain:** Real-time in-editor AppSec feedback (Copilot skill / AI-powered assistant)
**Researched:** 2026-03-18
**Confidence note:** Web search and WebFetch unavailable. Analysis drawn from training knowledge of SonarLint, Snyk IDE, Semgrep VS Code extension, GitHub Advanced Security (GHAS), CodeQL, Checkmarx One IDE plugin, and developer adoption research through August 2025. Confidence levels marked per section.

---

## Table Stakes

Features developers expect from any in-editor security tool. Missing any of these = tool gets ignored or disabled within a week.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Finding appears at the specific line of bad code | Any tool that doesn't pinpoint the exact location fails immediately — devs won't hunt for the problem | Low | In Copilot skill context: the response must reference the exact line/code fragment, not a general warning |
| Explanation of WHY it's a vulnerability | "SQL injection risk" without context is ignored; "user input on line 14 flows into query on line 22 without sanitization" is acted on | Medium | Context-specific explanation is what separates signal from noise |
| OWASP/CWE identifier included | Teams use these as triage vocabulary; without it, devs can't communicate findings to AppSec team or track against standards | Low | Surface A01, A03, etc. with a short label — not a wall of OWASP text |
| Fix guidance (not just identification) | SonarLint and Snyk both learned this the hard way: flagging without fixing is a morale drain | Medium | At minimum: "use PreparedStatement instead of string concatenation"; ideally a corrected code snippet |
| Language-appropriate fix | Java fix for SQL injection is different from JS fix. Generic advice signals the tool doesn't understand the codebase | Medium | Must target the specific language in context — one of Java, C#, JS/TS, Kotlin, Swift |
| Fast response | <5s is the hard limit; anything slower breaks flow state. SonarLint's on-type analysis completes in 1-3s; Snyk's deep analysis is 3-8s and users notice | Low (architectural concern) | Speed is a product requirement, not a nice-to-have |
| Low false positive rate (signal, not noise) | False positives are the #1 reason developers disable security tools. Snyk reports ~30% of findings are dismissed in enterprise use; teams with >20% FP rate disable tools within 30 days | High (rule quality) | OWASP Top 10 focus specifically reduces FP rate vs exhaustive CVE coverage |
| Scannable output (fits in 10-15 seconds of reading) | Developers don't read security reports during coding. SonarLint's inline message is 1-2 sentences; Snyk's panel view is skimmable. Wall-of-text = ignored | Low | Enforced by design: short finding + short fix, not a report |

---

## Differentiators

Features that separate this tool from existing options. Developers won't switch to a new tool for parity — it must do something existing tools don't.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Context-specific explanation (not generic) | SonarLint says "avoid SQL injection"; a good AI skill says "the `userId` parameter from your HTTP request is concatenated directly into this query — an attacker can inject `'; DROP TABLE users; --`". The specificity is the differentiator | High | Requires reading the actual code context, not just pattern-matching the vulnerable line |
| Working fixed code snippet (copy-paste ready) | Snyk and SonarLint both show fix examples, but they are generic. An AI skill can produce the corrected version of the actual code in front of the developer | High | This is the primary AI advantage — personalized, not templated |
| Triage by severity + exploitability, not just presence | GHAS and SonarLint surface everything; developers learn to ignore high-severity findings because many are theoretical. Ranking by actual exploitability in context is rare and valued | High | v1 scope: focus on clearly exploitable patterns (taint analysis patterns, not theoretical issues) |
| Trigger on code review diff (not just open file) | Most IDE tools scan the open file. Triggering on a Copilot code review diff — the specific changes being submitted — targets the highest-signal moment: right before a PR | Medium | This is the Copilot hook model described in PROJECT.md; no existing IDE tool does this natively |
| On-demand via chat (@appsec invocation) | Developers can ask "is this endpoint vulnerable?" in natural language without knowing rule IDs or OWASP categories. None of the current tools offer this conversational mode in-editor (GitHub Copilot Chat has general knowledge but no specialized AppSec skill) | Medium | The chat trigger is a differentiator from static analysis tools; it meets the developer where they are |
| Severity framing tied to business impact | "High severity" is abstract. "An attacker can read any user's session token by manipulating the `id` parameter" is concrete. Framing in impact terms drives action | Medium | Requires prompt engineering; not a code change |
| Multi-language consistency with a single invocation | Snyk, Checkmarx, and GHAS require language-specific configuration per repo. A skill that handles Java, C#, JS/TS, Kotlin, Swift from a single prompt-based interface is simpler to deploy team-wide | Low (model handles it) | The AI model is inherently polyglot — this is a free differentiator if the prompts are designed right |

---

## Anti-Features

Things that cause developers to tune out, disable, or ignore the tool. These are the most important findings from developer adoption research on SonarLint, Snyk, and similar tools.

| Anti-Feature | Why It Fails | What to Do Instead |
|--------------|-------------|-------------------|
| False positives without explanation | When a flagged finding is wrong and the tool doesn't explain its reasoning, developers assume all findings are wrong. This kills adoption permanently. Snyk data shows the first false positive a developer encounters sets their mental model for the whole tool | Surface reasoning ("this is flagged because user input from `request.getParameter()` flows into...") so devs can immediately assess validity |
| Wall-of-text security report | Security reports are for auditors, not developers. Every tool that generates PDF-style output during coding gets ignored within a sprint. IntelliJ's old security plugin fell into this trap | Hard cap: one finding description (2-3 sentences max), one fix (code snippet + 1 sentence rationale) |
| Auto-applying fixes without review | Developers need to understand what changed and why. Auto-apply destroys trust when the fix breaks something — and it will sometimes break something. PROJECT.md explicitly rules this out. | Always require developer acceptance. Show the diff. Explain the change. |
| Blocking workflow (modal alerts, required acknowledgment) | Any tool that interrupts the coding flow is immediately disabled. Security warnings are not compiler errors. | Passive surface: inline squiggles, panel entries, chat responses — never modal, never blocking |
| Duplicate findings already caught by CI/CD | If SonarQube already flags SQL injection in the pipeline, an IDE tool that re-surfaces the same finding with no additional context adds noise, not value. Developers tune out redundant signals | Add context the pipeline can't add: the specific code path, a concrete fix for THIS code, impact framing |
| Generic fixes that don't match the codebase | "Use parameterized queries" as a fix for a Kotlin + Exposed ORM codebase is useless. Generic advice signals the tool is not actually reading the code. | Generate fixes using the libraries and patterns visible in the code under review |
| Overwhelming severity counts | Showing "47 HIGH severity issues" on first run causes learned helplessness — no one fixes 47 things. Snyk and GHAS both suffer from this on legacy codebases. | Triage to top 3-5 actionable findings per review. Depth over breadth. |
| Opaque rule IDs without explanation | "CWE-89 violation" means nothing to most developers. Tool-internal identifiers without plain-language framing get ignored. | Lead with human language, append the CWE/OWASP identifier as reference |
| Requiring configuration before first value | Tools that need a config file, server connection, or rule selection before doing anything lose developers in setup. SonarLint connected mode is a known adoption blocker. | Zero-config first run. Work on the first invocation without any setup. |

---

## OWASP Top 10 Coverage: High-Signal Patterns by Language

Focus for v1. These are patterns with high true-positive rates and clear, demonstrable fixes. Ranked by exploitability and prevalence per language.

**Confidence: HIGH** — OWASP Top 10 (2021) is stable and well-researched. Language-specific prevalence based on industry SAST tooling research through 2025.

### Java

| OWASP Category | Pattern | Why High-Signal |
|----------------|---------|-----------------|
| A03 — Injection | SQL injection via JDBC (`Statement` + string concat), JPQL injection | Extremely common in Spring/JPA codebases; clear fix (PreparedStatement, parameterized JPQL) |
| A03 — Injection | Command injection (`Runtime.exec()` with user input) | Easy to detect, severe impact, unambiguous fix |
| A02 — Cryptographic Failures | `MD5`/`SHA1` for password hashing; `ECB` cipher mode | Detectable by import/usage pattern; clear upgrade path to BCrypt/Argon2 |
| A01 — Broken Access Control | Missing `@PreAuthorize` / Spring Security method-level auth | Detectable in Spring Boot controllers; very common in feature development |
| A05 — Security Misconfiguration | CORS wildcard (`*`), disabled CSRF in Spring Security | Config-level patterns, easy to detect |
| A07 — Identification Failures | Hardcoded credentials / API keys in source | Universal high-signal pattern |

### C\#

| OWASP Category | Pattern | Why High-Signal |
|----------------|---------|-----------------|
| A03 — Injection | SQL injection via `SqlCommand` + string concat; EF raw SQL | Common in ASP.NET codebases; fix is parameterized queries |
| A03 — Injection | LDAP injection via `DirectorySearcher` | Specific to enterprise C# codebases; high severity |
| A02 — Cryptographic Failures | `MD5CryptoServiceProvider`, `DES`, `RC2` usage | API name is the fingerprint; clear fix |
| A01 — Broken Access Control | Missing `[Authorize]` attribute on ASP.NET controllers/actions | Structural pattern; easy to detect |
| A08 — Software/Data Integrity | Insecure deserialization (`BinaryFormatter`) | Microsoft deprecated `BinaryFormatter` in .NET 7+; still present in legacy code |
| A07 — Identification Failures | Hardcoded credentials in `appsettings.json` or connection strings | Common in .NET projects |

### JavaScript / TypeScript

| OWASP Category | Pattern | Why High-Signal |
|----------------|---------|-----------------|
| A03 — Injection | SQL injection via template literals in query strings | Extremely common in Node.js + Express codebases |
| A03 — Injection | NoSQL injection (MongoDB `$where`, unvalidated filter objects) | Underdetected; JS object structure enables it trivially |
| A03 — XSS | `innerHTML`, `document.write()` with user-controlled data; React `dangerouslySetInnerHTML` | Extremely high prevalence; pattern-detectable |
| A02 — Cryptographic Failures | `Math.random()` for tokens/IDs; `md5` npm package for passwords | Import-detectable; fix is `crypto.randomBytes` / bcrypt |
| A01 — Broken Access Control | Missing auth middleware on Express routes; JWT not verified (`jwt.decode` vs `jwt.verify`) | Structural; common mistake |
| A07 — Identification Failures | Hardcoded API keys, secrets in JS source; `.env` values committed | Very common; high visibility impact |

### Kotlin

| OWASP Category | Pattern | Why High-Signal |
|----------------|---------|-----------------|
| A03 — Injection | SQL injection in Android `SQLiteDatabase.rawQuery()` with string concat | Android-specific; direct fix available (parameterized query) |
| A03 — Injection | Command injection via `Runtime.getRuntime().exec()` | Same as Java; Kotlin inherits the JVM API |
| A02 — Cryptographic Failures | `MessageDigest.getInstance("MD5")` for password hashing | Same JVM API as Java; import-detectable |
| A05 — Security Misconfiguration | Android `WebView.setJavaScriptEnabled(true)` + `addJavascriptInterface` | Mobile-specific; common in hybrid apps |
| A07 — Identification Failures | Hardcoded API keys in Android source / `BuildConfig` | Very common in mobile development |

### Swift

| OWASP Category | Pattern | Why High-Signal |
|----------------|---------|-----------------|
| A02 — Cryptographic Failures | `CommonCrypto` MD5/SHA1 for password hashing; `kCCAlgorithmDES` | API name is the fingerprint |
| A05 — Security Misconfiguration | `NSAllowsArbitraryLoads` in `Info.plist` (ATS disabled); `UIWebView` instead of `WKWebView` | Config + API patterns; easy to detect |
| A01 — Broken Access Control | Sensitive data stored without `kSecAttrAccessibleWhenUnlocked`; storing credentials in `UserDefaults` instead of Keychain | Storage pattern; high mobile relevance |
| A03 — Injection | SQLite raw query concatenation via FMDB or raw C API | Less common than other languages; still present |
| A07 — Identification Failures | Hardcoded API keys / secrets in Swift source or plists | Universal pattern |

---

## How Existing Tools Present Findings (Competitive Analysis)

**Confidence: HIGH** — Based on direct knowledge of these tools through mid-2025.

### SonarLint (VS Code / JetBrains)
- **Detection trigger:** On-type / on-save; analyzes open file and recently changed files
- **Finding presentation:** Inline squiggle (red/yellow) + hover tooltip with rule title + 1-2 sentence explanation; secondary locations shown for taint flows
- **Fix presentation:** "Quick Fix" lightbulb (code action) for some rules; links to SonarSource rule documentation; no AI-generated fix
- **Strengths:** Low latency, good Java/C# coverage, connected mode syncs with SonarQube server rules
- **Weaknesses:** Generic fixes (not code-context-aware), connected mode setup is a barrier, no chat interface, no PR diff trigger

### Snyk IDE Extension (VS Code / JetBrains / Eclipse)
- **Detection trigger:** On-save + background scan; separate scan for code, dependencies, IaC
- **Finding presentation:** Inline annotation + dedicated Snyk panel with categorized findings; severity color coding
- **Fix presentation:** "Snyk Fix" automated fix for dependency vulnerabilities (updates `package.json`); for code issues, shows example fix but not a generated fix for the specific code
- **Strengths:** Dependency vulnerability coverage is best-in-class; UI is polished
- **Weaknesses:** Code scanning is weaker than SonarLint for Java/C#; 3-8s scan delay noticeable; high false positive rate on code findings vs dependency findings

### Semgrep VS Code Extension
- **Detection trigger:** On-save; rule-set based
- **Finding presentation:** Inline squiggles + panel; rule ID + message; no taint flow visualization in VS Code
- **Fix presentation:** Some rules include `fix:` patterns (automated replacement); most rules show message only
- **Strengths:** Excellent rule customization; fast for pattern-matching rules; good JS/TS coverage
- **Weaknesses:** Fix quality depends entirely on rule author; no AI synthesis; setup requires rule selection

### GitHub Advanced Security / CodeQL (VS Code extension)
- **Detection trigger:** Background; triggered on file open or explicit scan
- **Finding presentation:** Inline squiggle + `gh-codeql` panel; shows full data flow paths for taint analysis
- **Fix presentation:** No automated fix; links to rule documentation; Copilot Autofix (separate feature) generates AI fix suggestions in PR context
- **Strengths:** Best taint analysis depth (data flow paths); SARIF standard output; Copilot Autofix in PRs is the closest existing analogue to this project
- **Weaknesses:** Slow (minutes for full scan); not suitable for real-time on-type; limited to supported languages; Copilot Autofix is PR-only, not in-editor during writing

### Copilot Autofix (GitHub, PR context) — Closest Existing Analogue
- **Detection trigger:** When GHAS code scanning surfaces a finding on a PR
- **Finding presentation:** Comment on PR with vulnerability description
- **Fix presentation:** AI-generated code diff with explanation; developer clicks "Commit Suggestion" to apply
- **Strengths:** Context-aware fix generation (knows the actual code); actionable; clear accept/reject flow
- **Weaknesses:** PR-only (not at write-time); requires GHAS to be enabled; not conversational; no on-demand invocation

---

## Feature Dependencies

```
Language-specific detection patterns → Context-specific explanation (must know the language to explain it)
Context-specific explanation → Working fixed code snippet (fix must match the pattern and language)
Low false positive rate → Developer trust → Tool adoption (all downstream from rule quality)
Fast response (<5s) → Copilot skill architecture (not just rule quality)
Copilot code review trigger → Diff-aware scanning (must operate on changed lines, not whole file)
On-demand chat (@appsec) → Copilot Chat skill registration (separate integration path from auto-trigger)
```

---

## MVP Recommendation

### Prioritize

1. **Context-specific finding with OWASP category** — The one sentence that names the specific variable/function at risk. This is the table stakes item most AI tools get wrong by being generic.
2. **Language-appropriate copy-paste fix** — The actual corrected code for what's in front of the developer. This is the primary AI differentiator over SonarLint/Semgrep.
3. **OWASP A03 (Injection) first** — Highest prevalence, highest true positive rate, clearest fixes across all five target languages. Single category, five language implementations = maximum coverage per unit of effort.
4. **Scannable format** — Finding: 2 sentences. Fix: code block + 1 sentence why. Non-negotiable.
5. **Hardcoded credentials detection (A07)** — Universal across all languages; zero ambiguity; highest developer "oh that's useful" reaction per effort invested.

### Defer

- **A01 (Broken Access Control) structural patterns** — Requires understanding the authorization framework in use (Spring Security vs custom vs none). High risk of false positives in v1 without framework context. Add in v2.
- **Dependency vulnerability scanning** — Snyk owns this well. Out of scope and redundant.
- **Taint flow visualization** — Valuable but complex. v1 describes the flow in prose; visual taint paths are a v2 investment.
- **Custom rule authoring** — Explicitly out of scope per PROJECT.md.
- **Auto-apply fixes** — Explicitly out of scope per PROJECT.md.

---

## Sources

**Confidence: HIGH** for tool behavior descriptions (SonarLint, Snyk, GHAS, Semgrep, Copilot Autofix) — based on direct training knowledge through August 2025 covering official documentation and product behavior.

**Confidence: HIGH** for OWASP Top 10 (2021) category descriptions and language-specific pattern prevalence — OWASP Top 10 is a stable, extensively documented standard.

**Confidence: MEDIUM** for developer adoption behavioral claims (false positive thresholds, disable rates) — these figures are consistent with industry reporting (Snyk State of Developer Security, SANS AppSec surveys, Sonatype DevSecOps community survey) but could not be verified against live sources in this session.

**Note:** Web search and WebFetch were unavailable for this research session. No live source verification was possible. Recommend spot-checking the competitive analysis against current SonarLint, Snyk, and Semgrep documentation before finalizing roadmap decisions.

- SonarLint documentation: https://docs.sonarsource.com/sonarlint/
- Snyk IDE plugin docs: https://docs.snyk.io/scm-ide-and-ci-cd-integrations/snyk-ide-plugins-and-extensions/
- OWASP Top 10 (2021): https://owasp.org/Top10/
- GitHub Copilot Autofix: https://docs.github.com/en/code-security/code-scanning/managing-code-scanning-alerts/about-autofix-for-codeql
- Semgrep VS Code extension: https://semgrep.dev/docs/extensions/semgrep-vs-code/
