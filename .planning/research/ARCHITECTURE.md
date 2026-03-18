# Architecture Patterns

**Domain:** LLM-powered AppSec developer skills (Claude Code + GitHub Copilot)
**Researched:** 2026-03-18
**Confidence note:** Claude Code slash command format verified from local examples (HIGH). Copilot extension architecture from training knowledge, cutoff August 2025 (MEDIUM). Prompt engineering patterns from training knowledge (MEDIUM).

---

## Integration Path A: Claude Code Slash Commands

### File Format (HIGH confidence — verified from live examples)

Claude Code slash commands are plain markdown files with a YAML frontmatter block followed by XML-tagged instruction sections. They live in:

- `~/.claude/commands/` — global, available across all projects
- `.claude/commands/` — project-scoped, checked into the repo

A complete skill file looks like this:

```markdown
---
name: appsec:check
description: Scan current file for OWASP Top 10 vulnerabilities
argument-hint: "[optional: file path or selection]"
allowed-tools:
  - Read
  - Bash
---

<objective>
Scan the provided code for high-signal security vulnerabilities. Map each
finding to an OWASP Top 10 category and CWE. Output is concise — one
finding block per issue, no prose padding.
</objective>

<context>
Target: $ARGUMENTS (if empty, use the currently open file)
</context>

<process>
1. Identify the language (Java, C#, JavaScript/TypeScript, Kotlin, Swift).
2. Apply OWASP Top 10 pattern checks for that language.
3. For each finding: location, pattern name, OWASP/CWE ref, why dangerous here.
4. If no findings: one-line confirmation.
</process>

<output_format>
[FINDING] <pattern-name> — <file>:<line>
OWASP: <category> | CWE: <id>
Risk: <one sentence why dangerous in this specific context>
</output_format>
```

### Frontmatter Fields

| Field | Required | Purpose |
|-------|----------|---------|
| `name` | Yes | Invocation name — becomes `/appsec:check` |
| `description` | Yes | Shown in `/help` and command picker |
| `allowed-tools` | Yes | Whitelist of tools the skill can call (Read, Bash, Write, Glob, Grep, Task) |
| `argument-hint` | No | Shown as placeholder when user types the command |

### How Code Context Reaches the Skill

Claude Code does **not** automatically inject file content. The skill receives context through these mechanisms:

1. **`$ARGUMENTS`** — whatever the user types after the command name. User can pass a file path, a selection pasted inline, or nothing.
2. **`@file` references** — the skill can reference files by path. User can prefix an `@filename` in their invocation.
3. **`Read` tool** — if `Read` is in `allowed-tools`, the skill can proactively read files. For the check skill, the process block should instruct Claude to `Read` the current file if `$ARGUMENTS` is empty.
4. **Bash tool** — if allowed, the skill can run `git diff HEAD` to get a diff, or `cat <file>` to load content. Use `Bash` sparingly to keep the skill fast.

**Practical pattern for the check skill:**

```markdown
<process>
If $ARGUMENTS contains a file path, Read that file.
If $ARGUMENTS is empty, ask the user which file to check OR check the most
recently mentioned file in conversation context.
Never silently scan the whole repo — scope to one file per invocation.
</process>
```

### Skill Invocation Modes

| Mode | How invoked | Context available |
|------|-------------|-------------------|
| On-demand chat | `/appsec:check path/to/File.java` | User-supplied path |
| On-demand with selection | User pastes code, then `/appsec:check` | Inline in conversation |
| Copilot code review hook | Triggered automatically on PR/diff | Diff text in context (see Path B) |

---

## Integration Path B: GitHub Copilot Extensions

**Confidence:** MEDIUM (training knowledge, Copilot Extensions GA'd in 2024, details may have evolved)

### Extension Types

Copilot supports two extension models:

| Type | Hosting | Complexity | When to use |
|------|---------|------------|-------------|
| **Copilot Agent** (skillset) | GitHub-hosted | Low — define skills in YAML, GitHub calls your endpoint | Simple tool-calling pattern |
| **Copilot Extension** (full) | Self-hosted or GitHub App | High — full webhook server, SSE streaming, OAuth | Full control over UX |

For this project, the **skillset/agent model** is the right fit. It avoids running infrastructure while still appearing as `@appsec` in Copilot Chat.

### Skillset Architecture (recommended path)

A Copilot skillset is defined in a GitHub App manifest. Skills are HTTP endpoints you expose; GitHub handles the chat routing.

```
Developer types: @appsec check this file
         │
         ▼
GitHub Copilot Chat
         │  calls skill endpoint with context payload
         ▼
Your HTTP endpoint (cloud function, Vercel, etc.)
         │  returns SSE stream of text
         ▼
Copilot Chat renders response inline
```

**Skill definition (in GitHub App manifest):**

```yaml
# .github/copilot-skillset.yml (illustrative — exact format per GitHub docs)
skills:
  - name: vulnerability-check
    description: Scan code for OWASP Top 10 vulnerabilities
    parameters:
      - name: code
        description: The code to scan
        required: true
      - name: language
        description: Programming language (java, csharp, javascript, kotlin, swift)
        required: false
```

### How Code Context Reaches a Copilot Extension

Copilot sends a structured payload to the skill endpoint. The payload includes:

| Context field | What it contains | Availability |
|---------------|-----------------|--------------|
| `messages` | Full conversation history including user message | Always |
| `references` | File/selection references the user attached | When user uses `#file` or selects code |
| `currentFile` | Currently open file in editor | Available via Copilot context (editor state) |
| `diff` | PR diff (in code review mode) | PR review context only |

The extension receives this as JSON in the POST body. The skill extracts code from `references[].data` or from the last user `messages[].content`.

**Code review (diff) mode** — triggered automatically on PR:
- Copilot sends the diff as part of the `messages` context
- The skill does not need to fetch the diff itself
- The diff is chunked if large — skills see one chunk at a time

### Authentication and Deployment

- GitHub App with Copilot permission scope
- Skills endpoint must be HTTPS, publicly reachable
- For internal team use: can be a private GitHub App installed on the org
- Simplest deployment: Vercel serverless function or AWS Lambda

---

## Data Flow: Both Paths

```
Path A (Claude Code):
  Developer writes code
       │
       ▼
  /appsec:check  ─── $ARGUMENTS ──▶  skill.md (prompt)
                                           │
                                    Read tool → file content
                                           │
                                    Claude processes (single LLM call)
                                           │
                                    Findings output in chat
                                           │
  /appsec:fix    ◄── developer copies findings or types "fix finding 2"
                                           │
                                    Claude generates fixed code snippet
                                           │
                                    Developer reviews and applies

Path B (Copilot Extension):
  Developer opens PR / types @appsec in chat
       │
       ▼
  GitHub Copilot ─── context payload ──▶  HTTP endpoint
                                               │
                                        Prompt assembly
                                        (OWASP rules + code)
                                               │
                                        LLM call (Claude or other)
                                               │
                                        SSE stream back to Copilot
                                               │
                                        Finding rendered in chat
                                               │
  @appsec fix finding 1  ──────────────▶  follow-up HTTP call
                                               │
                                        LLM generates fix
                                               │
                                        Fix rendered in chat
```

---

## Component Boundaries

| Component | Responsibility | Inputs | Outputs |
|-----------|---------------|--------|---------|
| `vulnerability-check` skill | Detect + classify vulnerabilities | Code text, language | Structured findings list |
| `how-to-fix` skill | Explain + generate fix for one finding | Finding description + original code | Fixed code snippet + explanation |
| OWASP prompt library | Language-specific pattern descriptions | Language name | Compressed pattern block injected into prompt |
| Context scoper | Trim code to relevant sections | Full file | Focused snippet (±50 lines around risk area) |

The OWASP prompt library and context scoper are **prompt fragments**, not separate services. They are sections within the skill markdown files.

---

## Prompt Architecture for Vulnerability Detection

### Structure That Works (MEDIUM confidence — established LLM security prompt patterns)

A vulnerability detection prompt has four layers:

```
Layer 1: Role + constraint
  "You are a security reviewer. Output only findings, no explanations
   unless a finding exists. Never fabricate issues."

Layer 2: OWASP scope block (compressed, language-specific)
  "For {language}, check: [A01] Broken Access Control — look for X, Y...
   [A02] Cryptographic Failures — look for hardcoded keys, weak algos..."

Layer 3: Code under review
  <code language="{lang}">
  {file content, trimmed to ≤300 lines}
  </code>

Layer 4: Output format specification
  "Respond ONLY with finding blocks in this format, or NONE if clean..."
```

### OWASP Pattern Encoding Without Massive Prompts

The key insight: OWASP Top 10 patterns are highly compressible per language. Rather than one giant prompt, use a **language-keyed pattern table** embedded in the skill file:

```markdown
## OWASP Patterns by Language

### Java
- [A01] Access Control: Missing @PreAuthorize, direct object refs in @PathVariable without ownership check
- [A02] Crypto: DES/MD5/SHA1 usage, hardcoded keys/IVs, SecureRandom seeded with predictable value
- [A03] Injection: String concatenation in JPQL/HQL/SQL, unvalidated path in File(), ProcessBuilder with user input
- [A07] Auth: HttpSession without invalidate on login, missing CSRF token in forms
...

### JavaScript/TypeScript
- [A01] Access Control: Missing auth middleware, req.params used without ownership check
- [A03] Injection: eval(userInput), innerHTML assignment, child_process.exec with template literal
- [A05] Misconfig: helmet() not applied, CORS wildcard in production
...
```

This table adds ~400 tokens per language, ~2,000 tokens for all five languages. Acceptable. The alternative (full rule descriptions) would be 10-20x larger and slower.

### Context Window Management

The biggest risk with LLM code review: sending too much code and overwhelming the model's attention.

**Rules:**
1. One file per invocation — never scan a directory
2. Cap file input at 300 lines (roughly 12,000 tokens of code). Files larger than this need user to select a section.
3. For diff mode: process one file's diff at a time, not the entire PR diff
4. Strip comments and blank lines before sending if file is near the cap

**Scoping heuristic for the check skill:**

```markdown
<process>
Read the target file. If it exceeds 300 lines:
  - Notify the developer: "File is large — scanning first 300 lines.
    Specify a line range to scan a different section."
  - Scan only the first 300 lines.
Otherwise scan the full file.
</process>
```

---

## Check → Fix Data Flow (Two-Skill Design)

### Why two separate skills

- `check` is invoked often (every file, every review) — must be fast, minimal output
- `fix` is invoked selectively — developer chooses which finding to fix
- Keeping them separate means `check` never generates code (faster, cheaper) and `fix` always has a specific target (focused, accurate)

### How fix receives check output

The fix skill does not need to store check output externally. Claude Code maintains conversation context. The developer workflow is:

```
1. /appsec:check src/UserController.java
   → Finding: [A01] Missing access control on getUserById() at line 42

2. /appsec:fix finding 1
   (or: /appsec:fix the access control issue at line 42)
   → Fix explanation + corrected code block

3. Developer copies the fix or applies it via Claude's Edit tool
```

The fix skill prompt references the prior conversation:

```markdown
<process>
The developer has run /appsec:check and received findings.
The finding to fix is: $ARGUMENTS (or the most recent finding if empty).

Read the file that was scanned (from conversation context or re-read if path given).
Generate:
  1. One paragraph: why this pattern is dangerous in their specific context
  2. Fixed code block (drop-in replacement, same language, no extra dependencies)
  3. One line: how to verify the fix is correct
</process>
```

For the Copilot extension path, the conversation history in the `messages` payload carries the check output — no external state needed.

---

## Build Order

### Phase 1: Claude Code check skill (build this first)

**Why:** Fastest feedback loop. A markdown file + prompt iteration costs nothing. You validate the OWASP pattern encoding and output format before writing any server code. The skill can be tested immediately by the team.

**Deliverable:** `.claude/commands/appsec/check.md` — runnable via `/appsec:check`

### Phase 2: Claude Code fix skill

**Why:** Builds directly on check. Same format, tested against real findings from Phase 1. No new infrastructure.

**Deliverable:** `.claude/commands/appsec/fix.md` — runnable via `/appsec:fix`

### Phase 3: Copilot extension (if team validates Claude Code path)

**Why:** Only build the HTTP endpoint after the prompt is validated. Don't write infrastructure around a prompt that hasn't been tested. The Copilot extension wraps the same prompt logic from Phases 1-2 in an HTTP server.

**Deliverable:** Serverless function (Vercel/Lambda) + GitHub App registration

### Phase 4: Copilot code review hook (diff mode)

**Why:** Diff mode requires the extension to be running (Phase 3 prerequisite). It also requires tuning the prompt for diff context (not whole-file). Build after the baseline extension is stable.

**Deliverable:** Diff-aware prompt variant, PR trigger configuration

---

## Anti-Patterns to Avoid

### Anti-Pattern 1: Scanning whole repositories
**What:** Passing multiple files or entire directories to the check skill
**Why bad:** Context window saturation, slow response, missed findings (attention dilution)
**Instead:** One file per invocation. The skill should refuse and redirect if given a directory path.

### Anti-Pattern 2: Generating fixes inside the check skill
**What:** Having check output both findings AND fixed code
**Why bad:** Doubles token usage on every check invocation, slows the fast-path, and generates fixes the developer didn't ask for
**Instead:** check outputs findings only. fix is invoked on demand.

### Anti-Pattern 3: Generic OWASP descriptions in the prompt
**What:** Copying OWASP category descriptions verbatim into the prompt
**Why bad:** These descriptions are written for humans, not LLMs. They're long and not pattern-specific enough to trigger reliable detection
**Instead:** Encode as code-level patterns: "look for X in Y context" rather than "understand A01 Broken Access Control"

### Anti-Pattern 4: Building the Copilot extension before validating the prompt
**What:** Writing the HTTP endpoint, GitHub App, and deployment pipeline before the prompt is validated
**Why bad:** If the prompt produces noisy or inaccurate findings, you have to rework both the prompt AND the infrastructure
**Instead:** Validate the prompt via Claude Code skills first (zero infrastructure cost), then wrap in an extension.

### Anti-Pattern 5: Stateful finding storage
**What:** Storing findings in files or a database between check and fix invocations
**Why bad:** Unnecessary complexity. Claude Code conversation context and Copilot message history carry the findings naturally.
**Instead:** Let the conversation be the state. fix reads from conversation context, not from a file.

---

## Scalability Considerations

| Concern | At 5 developers (POC) | At 50 developers (team rollout) |
|---------|----------------------|--------------------------------|
| Claude Code skills | Share via `.claude/commands/` in repo — zero infra | Same — git pull updates everyone |
| Copilot extension | Single Vercel function (free tier) | Same function, may need rate limit awareness |
| Prompt cost | ~$0.01–0.05 per check invocation | Budget ~$50–200/month at high usage |
| Latency | Single LLM call, <5s target | Same — no fan-out, no caching needed |
| Rule updates | Edit markdown file, commit | Same — no redeployment for Claude Code path |

---

## Sources

| Source | Confidence | What it informed |
|--------|------------|-----------------|
| Live Claude Code command files at `~/.claude/commands/gsd/` | HIGH | Exact skill file format, frontmatter fields, XML sections, $ARGUMENTS, allowed-tools |
| Anthropic Claude Code documentation (training knowledge, Aug 2025 cutoff) | MEDIUM | Context injection mechanisms, @ references, project vs global scope |
| GitHub Copilot Extensions documentation (training knowledge, Aug 2025 cutoff) | MEDIUM | Skillset vs extension model, payload structure, code review mode |
| LLM security prompt engineering patterns (training knowledge) | MEDIUM | Four-layer prompt structure, context window management rules |

**Gaps requiring validation before Phase 3:**
- Exact Copilot skillset manifest format (may have changed since Aug 2025 — verify against current GitHub docs)
- Whether Copilot sends `currentFile` context automatically or requires user to use `#file` reference
- Rate limits on Copilot Extensions for org-wide rollout
