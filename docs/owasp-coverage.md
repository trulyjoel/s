# OWASP Top 10 Coverage Map

AppSec Skills v1 -- Phase 1 (SQL Injection Detection)

**Date:** 2026-03-18
**Version note:** Coverage reflects Phase 1 implementation only. Additional categories will be added as subsequent phases are completed. No category is claimed as Covered without validated test cases.

---

## Coverage Table

| OWASP Category | Status | Languages | Details |
|---|---|---|---|
| A01: Broken Access Control | Not Covered | -- | Requires business logic context unavailable in single-file analysis |
| A02: Cryptographic Failures | Not Covered | -- | Planned for future phase |
| A03: Injection (SQL) | Covered | Java, JS/TS | SQL injection via string concatenation, template literals, unsafe raw queries |
| A03: Injection (Command) | Not Covered | -- | Planned for future phase |
| A03: Injection (XSS) | Not Covered | -- | Planned for future phase |
| A04: Insecure Design | Not Detectable | -- | Structural limitation: not detectable from code snippets |
| A05: Security Misconfiguration | Not Covered | -- | Requires configuration file analysis |
| A06: Vulnerable Components | Not Detectable | -- | Structural limitation: requires dependency manifest analysis |
| A07: Auth Failures | Not Covered | -- | Planned for future phase |
| A08: Data Integrity Failures | Not Covered | -- | Planned for future phase |
| A09: Logging Failures | Not Covered | -- | Planned for future phase |
| A10: SSRF | Not Covered | -- | Planned for future phase |

---

## Status Tier Definitions

- **Covered**: Specific detectable patterns with test cases validated against fixture files
- **Partially Covered**: Some sub-categories covered, others not
- **Not Covered**: Category is in scope but not yet implemented
- **Not Detectable**: Category requires context unavailable in single-file code analysis (structural limitation — no amount of prompt engineering can overcome this)

---

## Safe Patterns: What the Skill Will NOT Flag

These patterns are explicitly in the skill's safe list and will not produce findings.

### Java — Safe Patterns

- `PreparedStatement` with `?` placeholders and `setString()`, `setInt()`, `setLong()`, etc.
- Spring Data JPA `@Query` with named parameters and `@Param` binding
- Spring Data derived query methods (`findByEmail`, `findByIdAndStatus`, etc.)
- JPA Criteria API (`CriteriaBuilder`, `cb.equal(root.get("field"), value)`)
- JOOQ DSL query builder (`.select()`, `.from()`, `.where()` chain)

### JavaScript/TypeScript — Safe Patterns

- Parameterized queries with `$1`/`?` placeholders: `client.query('SELECT ... WHERE id = $1', [id])`
- Sequelize model methods: `User.findOne({ where: { email } })`, `Model.findAll({ where: {...} })`
- Knex query builder chain: `knex('users').where({ id }).first()`
- Prisma client queries: `prisma.user.findUnique({ where: { id } })`
- TypeORM repository methods and query builder with `.setParameter()`

---

## Limitations

- This skill analyzes **single files**. Cross-file taint analysis is not performed. If user input originates in file A and reaches a SQL sink in file B, this skill will not detect it.
- If the skill cannot determine whether input is user-controlled from the provided code, it reports **"cannot determine"** rather than flagging as vulnerable. This is intentional — false negatives are preferable to false positives in a developer-facing tool.
- File analysis is capped at **300 lines**. Files exceeding 300 lines are analyzed from the top; a note is appended to the response.
- Only **Java** and **JavaScript/TypeScript** (`.js`, `.ts`, `.tsx`, `.mjs`) are supported in this version.

---

Coverage claims are validated against test fixtures in `tests/fixtures/`. No category is claimed as Covered without passing test cases.
