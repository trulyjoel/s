# Technology Stack

**Project:** AppSec Skills — Real-Time Vulnerability Feedback
**Researched:** 2026-03-18
**Research mode:** Ecosystem
**Note:** WebSearch and WebFetch were unavailable in this environment. All findings are from training data (cutoff August 2025). Every claim is confidence-rated; LOW-confidence items must be verified against live docs before implementation.

---

## Integration Model Decision (Decide First)

This is the most important stack decision. The two delivery mechanisms have fundamentally different development models.

### Option A: Claude Code Custom Slash Commands (Recommended for POC)

**What it is:** Markdown files placed in `.claude/commands/` that define custom `/command-name` invocations within Claude Code sessions. The markdown file is the entire skill — no server, no deployment, no API keys beyond what Claude Code already uses.

**Format:** Each command is a `.md` file. The file content is a system prompt that Claude Code injects when the command is invoked. Arguments are passed inline. Supports `$ARGUMENTS` placeholder and can reference file contents.

**Example structure:**
```
.claude/commands/
  vulnerability-check.md   → invoked as /vulnerability-check
  how-to-fix.md            → invoked as /how-to-fix
```

**Example file content pattern:**
```markdown
You are an AppSec reviewer. Analyze the following code for OWASP Top 10 vulnerabilities.

Code to review:
$ARGUMENTS

For each finding:
- Name the vulnerability (OWASP category + CWE ID)
- Quote the exact dangerous line(s)
- Explain why it is dangerous in this specific context (2 sentences max)
- Severity: Critical / High / Medium

Output must be scannable in 15 seconds. No preamble.
```

**Confidence: MEDIUM** — The `.claude/commands/` format was documented by mid-2025 and is the standard Claude Code extensibility mechanism. The exact `$ARGUMENTS` syntax and invocation details should be verified against current Claude Code docs before final implementation.

**Strengths for this project:**
- Zero infrastructure — no server, no deployment, no webhook
- Version-controlled alongside code (team standard by design)
- Modifiable by any team member with a text editor
- Works offline after initial model load
- <5s response constraint is achievable (no network hop to external service)
- Claude Code already has file context — the skill can read surrounding code

**Weaknesses:**
- Only works for Claude Code users (not VS Code + Copilot users)
- No automatic trigger on diff/PR — must be invoked manually via `/vulnerability-check`
- No persistent state between invocations

---

### Option B: GitHub Copilot Extension

**What it is:** A GitHub App with a webhook endpoint that receives Copilot Chat messages and responds via the Copilot Extensions API. Requires a deployed HTTP server (or GitHub Actions) to handle requests.

**API model (as of mid-2025):**
- Register a GitHub App with Copilot Chat permissions
- Implement an HTTP endpoint that receives a JSON payload (conversation history, user message, context)
- Stream responses back using Server-Sent Events (SSE) format
- Users invoke via `@your-extension-name` in Copilot Chat
- GitHub handles authentication; your server receives a verified JWT

**Development stack required:**
- HTTP server (Node.js/Express or Python/FastAPI are common choices)
- GitHub App registration + secrets management
- Deployment target (Vercel, Railway, a VPS, or GitHub Actions)
- Optional: `@github-copilot/extensions-sdk` (Node) or equivalent

**Confidence: MEDIUM** — GitHub Copilot Extensions entered public beta in mid-2024 and were generally available by late 2024. The SSE streaming model and GitHub App registration requirement are well-documented. The exact SDK package names and API surface should be verified before implementation.

**Copilot Code Review hooks (separate from Chat):**
- As of 2025, GitHub Copilot Code Review is a separate feature (automatic review on PR open/update)
- It uses GitHub Actions integration or a repository setting, not the Extensions API
- Custom rules for code review can be configured via `.github/copilot-instructions.md` (repository-level custom instructions)
- There is no public API for injecting custom SAST-style rules into Copilot's automatic code review as of mid-2025

**Confidence: LOW** — The code review hooks/rules integration is an area of active development. What is possible may have changed significantly. Verify with GitHub docs before committing to this approach.

**Strengths:**
- Works for any Copilot Chat user regardless of which AI model they use
- Can trigger on PRs via GitHub Actions
- Native `@appsec` invocation in Copilot Chat sidebar

**Weaknesses:**
- Requires deployed server infrastructure
- Adds latency (network round-trip to your server + LLM call)
- More complex to version-control and distribute as a team standard
- GitHub App approval process for public distribution

---

### Recommended POC Strategy

**Start with Claude Code custom commands. Add Copilot extension if adoption data justifies it.**

Rationale:
1. POC goal is to validate the concept, not the delivery mechanism
2. Claude Code commands ship in hours; a Copilot extension takes days to set up
3. The skill prompt is the intellectual work — it's portable to either platform
4. If the team uses Claude Code, zero friction; if they use Copilot, the POC still proves the prompt quality
5. Later, the same prompt can be wrapped in a Copilot extension with minimal rework

---

## Core Stack

### Delivery Layer

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Claude Code custom commands | Current | Primary skill delivery | Zero infra, version-controlled, fast, team-distributable via git |
| GitHub Copilot Extension (Phase 2) | Current API | Secondary delivery for Copilot users | Same prompt, different transport — adds coverage if team is split on tooling |

### Skill Definition Format

| Technology | Purpose | Why |
|------------|---------|-----|
| Markdown (.md files) | Claude Code command definition | The entire skill is the prompt; no code required for Claude Code delivery |
| JSON (GitHub App manifest) | Copilot extension registration | Required by GitHub App platform |
| Node.js / TypeScript | Copilot extension HTTP server | Typed, fast startup, GitHub's own SDK is JS-first; small footprint |

**Confidence: MEDIUM** on Node/TypeScript for Copilot extension — this is the most common choice in the GitHub ecosystem but Python/FastAPI is equally viable.

### Vulnerability Detection Approach

**Use LLM-native detection, not a SAST engine.**

The skills run inside an LLM. Adding a SAST engine (semgrep, CodeQL) would require:
- A subprocess call from inside Claude Code (not available in the command format)
- Or a server-side component (only viable for the Copilot extension path)
- Response time well above the 5-second constraint

**For the Claude Code command path:** The skill is a prompt that instructs Claude to detect patterns. Claude's code understanding is sufficient for OWASP Top 10 patterns when given a well-structured prompt with explicit patterns to look for.

**For the Copilot extension path (if built):** The server can optionally run semgrep as a pre-pass, then feed findings + code to the LLM for explanation. This is the only viable integration of a SAST engine within the 5-second constraint — semgrep is fast on small code snippets.

| Detection Approach | When to Use | Confidence |
|-------------------|-------------|------------|
| LLM prompt with OWASP pattern list | Claude Code commands (v1 POC) | HIGH — proven approach |
| Semgrep pre-pass + LLM explanation | Copilot extension server (if built) | MEDIUM — requires server |
| CodeQL | Never for this use case | CodeQL requires full build context; way too slow for in-editor use |
| Regex pattern matching | Never as primary | Fragile, high false-positive rate, unmaintainable across 5 languages |

### OWASP Top 10 Rule Packaging

**LLM prompt with embedded pattern catalogue (recommended for POC)**

Package vulnerability patterns as structured prompt context, not as SAST rules. This is the correct approach for an LLM-native tool.

Format:
```markdown
## Vulnerability Patterns to Detect

### A01: Broken Access Control
- Missing authorization checks before data access (look for: database queries without user role/permission validation)
- Insecure direct object references (look for: user-supplied IDs used directly in queries without ownership checks)
- CORS misconfiguration (look for: `Access-Control-Allow-Origin: *` on authenticated endpoints)

[Java-specific]: look for `@PreAuthorize` absence on `@RequestMapping` methods handling sensitive data
[C#-specific]: look for controller actions without `[Authorize]` attribute accessing user data
[JavaScript-specific]: look for Express routes without middleware auth checks
...
```

**Why this over SAST rules:**
- No toolchain dependency
- Immediately portable across both delivery mechanisms
- Language-specific patterns can be embedded per-language context
- Easy to update by editing the markdown file

**Semgrep as optional enhancement (Copilot extension only)**

If a Copilot extension server is built, semgrep can run as a pre-pass:
- `semgrep --config=auto` runs the community rule set (Apache 2.0 licensed)
- The `p/owasp-top-ten` rule pack covers all 5 target languages
- Semgrep OSS is free; no account required for CLI use
- Typical scan time on a single file: <500ms

| Semgrep Detail | Value | Confidence |
|---------------|-------|------------|
| OWASP pack name | `p/owasp-top-ten` | MEDIUM — verify current pack name |
| Java support | Yes | HIGH |
| C# support | Yes (partial — less mature than Java/Python/JS) | MEDIUM |
| JavaScript/TypeScript | Yes | HIGH |
| Kotlin | Partial — community rules, lower coverage | MEDIUM |
| Swift | Limited — fewer community rules | LOW — verify before depending on this |
| CLI flag for single file | `semgrep --config=<pack> <file>` | HIGH |

**CodeQL — explicitly excluded**

CodeQL requires a compiled database of the full project. It cannot scan a snippet or a single file in real time. Build times range from minutes to hours. This is the opposite of the <5s constraint. Do not use.

### Supporting Libraries (Copilot Extension path only)

| Library | Purpose | Why |
|---------|---------|-----|
| `@octokit/webhooks` | GitHub webhook signature verification | Required for security; GitHub signs all payloads |
| `express` or `hono` | HTTP server | Express is battle-tested; Hono is lighter and faster cold-start |
| `zod` | Payload validation | Type-safe parsing of Copilot API payloads |
| `ai` (Vercel AI SDK) | LLM streaming | Handles SSE streaming back to Copilot; abstracts Claude/OpenAI |

**Confidence: MEDIUM** — These are the standard choices for GitHub App + LLM integration as of mid-2025. Package versions should be verified at implementation time.

---

## How the Two Integration Models Differ

| Dimension | Claude Code Commands | GitHub Copilot Extension |
|-----------|---------------------|-------------------------|
| Deployment | None — files in repo | HTTP server required |
| Invocation | `/vulnerability-check` in Claude Code | `@appsec` in Copilot Chat |
| Automatic trigger | No — manual only | Possible via GitHub Actions on PR |
| Response time | Fast (no extra network hop) | Adds ~200-500ms server round-trip |
| Distribution | Git clone / team dotfiles | GitHub Marketplace or org-level install |
| User requirement | Claude Code subscription | GitHub Copilot subscription |
| Skill modification | Edit a markdown file | Redeploy server |
| State/memory | Stateless (per-session context) | Can be stateful (server has persistence) |
| Code context | Claude Code reads files natively | Must be passed in the request payload |
| Infrastructure cost | $0 | Server hosting cost |
| Time to first working prototype | Hours | Days |

---

## Repository Structure (Recommended)

```
/
  .claude/
    commands/
      vulnerability-check.md    # Claude Code slash command
      how-to-fix.md             # Claude Code slash command
  .github/
    copilot-instructions.md     # Repo-level Copilot custom instructions (lightweight)
  skills/
    prompts/
      vulnerability-check.md    # Canonical prompt (shared source of truth)
      how-to-fix.md
    patterns/
      owasp-top10.md            # Vulnerability pattern catalogue
      java-patterns.md
      csharp-patterns.md
      javascript-patterns.md
      kotlin-patterns.md
      swift-patterns.md
  extension/                    # Only if Copilot extension is built
    src/
      index.ts
      handlers/
    package.json
    tsconfig.json
```

**Key principle:** The canonical prompt lives in `skills/prompts/`. The `.claude/commands/` file includes or references it. This means the same prompt powers both delivery mechanisms.

---

## `.github/copilot-instructions.md` — Lightweight Option

GitHub supports a `.github/copilot-instructions.md` file in any repository. Content is injected as context into all Copilot Chat conversations for that repo. This is not an extension — it's a configuration file.

**Use case:** Add AppSec context without building an extension. Example:
```markdown
When reviewing code in this repository, apply OWASP Top 10 principles.
Flag SQL injection, XSS, IDOR, and insecure deserialization patterns.
...
```

**Limitations:** No structured output, no custom invocation, no code review hook trigger. Useful as a complement, not a replacement for the skills.

**Confidence: HIGH** — This feature is well-documented and stable.

---

## Alternatives Explicitly Rejected

| Category | Rejected Option | Why Rejected |
|----------|----------------|--------------|
| Detection engine | CodeQL | Requires full build; minutes per scan; incompatible with <5s constraint |
| Detection engine | Regex-only patterns | High false-positive rate; breaks on string formatting; unmaintainable across 5 languages |
| Detection engine | SonarQube API | Heavyweight; requires server install; CI/CD tool not an IDE tool |
| Delivery | VS Code extension | Separate development effort; not Copilot-native; JS required; out of scope for POC |
| Delivery | IDE plugin (IntelliJ/Xcode) | Per-IDE effort; completely out of scope for POC |
| Rule format | SARIF output integration | SARIF is a reporting format, not a detection format; adds complexity with no benefit at POC scale |
| Infra | Kubernetes/containerized SAST | Overkill for a team POC; violates lean constraint |

---

## Installation

### Claude Code Commands (no dependencies)

```bash
# In the project repository root
mkdir -p .claude/commands
# Create vulnerability-check.md and how-to-fix.md
# Users invoke with /vulnerability-check in any Claude Code session in this repo
```

### Semgrep (if used in Copilot extension server)

```bash
# macOS
brew install semgrep

# Or via pip (cross-platform)
pip install semgrep

# Run against a single file
semgrep --config=p/owasp-top-ten path/to/file.java --json
```

### Copilot Extension Server (if built)

```bash
npm install express zod @octokit/webhooks
npm install -D typescript @types/node tsx
# Optional: npm install hono  # if choosing Hono over Express
```

---

## Confidence Assessment

| Area | Confidence | Basis | Verify Before |
|------|------------|-------|---------------|
| Claude Code `.claude/commands/` format | MEDIUM | Documented by mid-2025 in training data | Check current Claude Code docs for exact `$ARGUMENTS` syntax and any new features |
| GitHub Copilot Extensions API (Chat) | MEDIUM | Public beta → GA by late 2024; API shape known | Verify current SDK names, streaming format |
| Copilot Code Review hooks/rules | LOW | Active development area; limited public API as of mid-2025 | Check GitHub docs for what's now configurable |
| `.github/copilot-instructions.md` | HIGH | Stable feature, broadly documented | Low risk |
| Semgrep OWASP pack + language support | MEDIUM | Known as of 2025; pack names may change | `semgrep --list-packs` or check registry.semgrep.dev |
| Semgrep Kotlin/Swift coverage | LOW | Fewer community rules; coverage gaps known | Test against sample Kotlin/Swift code before depending on it |
| LLM-native detection approach | HIGH | Core capability of Claude; well-validated for code review tasks | No external verification needed |

---

## Sources

- Training data (knowledge cutoff August 2025) — used for all claims above
- No live sources could be fetched (WebSearch/WebFetch unavailable in this environment)
- **Action required:** Verify against:
  - https://docs.anthropic.com/en/docs/claude-code/slash-commands (Claude Code command format)
  - https://docs.github.com/en/copilot/building-copilot-extensions (Copilot Extensions API)
  - https://docs.github.com/en/copilot/customizing-copilot/adding-repository-custom-instructions-for-github-copilot (copilot-instructions.md)
  - https://semgrep.dev/docs (Semgrep rule packs and language support)
