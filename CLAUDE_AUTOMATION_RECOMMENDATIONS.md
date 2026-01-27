# Claude Code Automation Recommendations for Banyg

I've analyzed your Android finance app and identified the most valuable automations for your workflow. Here are my top recommendations:

---

## 📊 Codebase Profile
- **Type**: Android (Kotlin)
- **Framework**: Jetpack Compose + Material 3
- **Database**: Room (SQLite)
- **Architecture**: Multi-module (Clean Architecture with core/domain, core/data, core/ui, feature modules)
- **Key Focus**: Money safety (Long-based arithmetic), offline-first personal finance

---

## ⚡ Hooks (Critical for Your Project)

### 1. Money Safety Pre-commit Hook
**Why**: Your CLAUDE.md emphasizes critical money handling rules. This hook prevents Float/Double from ever entering your codebase for money operations.

**Where**: `.claude/settings.json`

```json
{
  "hooks": {
    "PreToolUse": {
      "Edit": [
        {
          "command": "bash",
          "args": ["-c", "if echo \"$CLAUDE_TOOL_INPUT\" | grep -q 'amount.*Double\\|amount.*Float\\|amountMinor.*toDouble\\|amountMinor.*toFloat'; then echo 'ERROR: Float/Double detected in money context. Use Long for amountMinor.'; exit 1; fi"],
          "description": "Block Float/Double in money operations"
        }
      ],
      "Write": [
        {
          "command": "bash",
          "args": ["-c", "if echo \"$CLAUDE_TOOL_INPUT\" | grep -q 'amount.*Double\\|amount.*Float\\|amountMinor.*toDouble\\|amountMinor.*toFloat'; then echo 'ERROR: Float/Double detected in money context. Use Long for amountMinor.'; exit 1; fi"],
          "description": "Block Float/Double in money operations"
        }
      ]
    }
  }
}
```

### 2. Detekt/Lint Auto-run Hook
**Why**: Android projects benefit from immediate lint feedback after code changes.

**Where**: `.claude/settings.json`

```json
{
  "hooks": {
    "PostToolUse": {
      "Edit": [
        {
          "command": "./gradlew",
          "args": ["lintDebug", "--daemon"],
          "description": "Run lint after edits",
          "continueOnError": true
        }
      ]
    }
  }
}
```

---

## 🎯 Skills (Leverage Your Existing Ones!)

You already have excellent custom skills! Here are 2 more that would complement your workflow:

### 1. **android-test-gen** (Create this)
**Why**: You have test directories but only example tests. Automate test generation for ViewModels, UseCases, and Repositories.

**Create**: `.claude/skills/android-test-gen/SKILL.md`

**Invocation**: User-only (via `/android-test-gen`)

```yaml
---
name: android-test-gen
description: Generate unit tests for Android components (ViewModel, UseCase, Repository)
disable-model-invocation: true
---

# Android Test Generator

Generate comprehensive unit tests for Banyg Android components following project conventions.

## Supported Components
- ViewModels (with StateFlow testing)
- UseCases (domain logic)
- Repositories (with fake DAOs)
- Money calculations (overflow, rounding, edge cases)

## Test Patterns

### ViewModel Test
- Test initial state
- Test each event handler
- Test state transitions
- Mock dependencies with MockK

### UseCase Test
- Happy path
- Edge cases (empty, null, boundaries)
- Error conditions
- Money overflow scenarios

### Repository Test
- Use fake DAO implementations
- Test Flow emissions
- Test data mapping (Entity ↔ Domain)

## Requirements
- Use MockK for mocking
- Use Truth for assertions where beneficial
- Use Turbine for Flow testing
- Follow naming: `[ClassName]Test.kt`
- Test functions: backtick descriptions
```

### 2. **architecture-verify** (Create this)
**Why**: Your CLAUDE.md has strict architectural rules. This skill helps verify module dependencies are correct.

**Create**: `.claude/skills/architecture-verify/SKILL.md`

**Invocation**: Both Claude and user can invoke

```yaml
---
name: architecture-verify
description: Verify Banyg architecture rules (module dependencies, import restrictions)
---

# Architecture Verification

Verify that code follows Banyg's strict architecture rules.

## Rules to Check

1. **core/domain has ZERO Android imports**
   - Search for `import android` in core/domain
   - Fail if found

2. **Repository interfaces in core/domain/repository**
   - Implementations in core/data/repository

3. **ViewModels never import Room or DataStore**
   - Search ViewModels for Room/DataStore imports
   - Should only use domain types

4. **Composables never import Room or DataStore**
   - Check feature/*Screen.kt files
   - Should only use domain types via ViewModel

5. **Module dependency direction**
   - app → feature → core/domain
   - core/data → core/domain
   - feature → core/ui
   - core/ui → core/domain (types only)

## Output
- ✅ Pass: Rule adhered to
- ❌ Fail: Violations found with file paths
```

---

## 🔌 MCP Servers

### 1. context7
**Why**: You're using Kotlin, Jetpack Compose, Room, Hilt, Coroutines - context7 provides instant documentation lookup for these libraries.

**Install**:
```bash
claude mcp add context7
```

**Usage**: Automatically queries up-to-date docs when you ask about Compose APIs, Room migrations, or Hilt injection patterns.

### 2. GitHub MCP (if you use GitHub)
**Why**: Manage issues, PRs, and actions directly from Claude.

**Install**:
```bash
claude mcp add github
```

**Prerequisite**: Install GitHub CLI (`brew install gh` and `gh auth login`)

---

## 🤖 Subagents

### 1. **android-migration-reviewer**
**Why**: Database migrations are critical in your app. A specialized subagent can review migrations for safety before you run them.

**Where**: `.claude/agents/android-migration-reviewer.md`

```markdown
---
name: android-migration-reviewer
description: Review Room migrations for data safety, indices, and test coverage
---

# Android Migration Reviewer

Review Room database migrations for Banyg.

## Review Checklist

1. **Migration SQL**
   - ✅ Creates tables/columns correctly
   - ✅ Handles ALTER TABLE safely
   - ✅ Creates indices on FKs and query columns
   - ✅ Preserves existing data
   - ✅ No DROP without backup

2. **Money Safety**
   - ✅ Money columns are `INTEGER NOT NULL` (Long)
   - ✅ Column named `amount_minor`, not `amount`
   - ✅ No REAL or FLOAT columns for money

3. **Test Coverage**
   - ✅ Migration test exists
   - ✅ Verifies schema creation
   - ✅ Verifies data preservation
   - ✅ Verifies indices created

4. **Database Version**
   - ✅ Version incremented in @Database
   - ✅ Migration added to .addMigrations()
   - ✅ Schema exported to schemas/ directory

## Output Format
- List violations with severity (CRITICAL, WARNING, INFO)
- Suggest fixes
- Approve or request changes
```

---

## 📦 Plugins

### superpowers (Already Available)
**Why**: You're already using skills from this plugin (I see references in your system context). This is essential for systematic workflows.

**What it provides**:
- systematic-debugging
- test-driven-development
- brainstorming
- writing-plans
- code-review workflows

**Already active** based on your session context.

---

## Implementation Priority

Based on your project's critical requirements, I recommend implementing in this order:

1. **FIRST: Money Safety Hook** - Prevents catastrophic bugs
2. **SECOND: architecture-verify skill** - Enforces your clean architecture
3. **THIRD: context7 MCP** - Speeds up Android development
4. **FOURTH: android-test-gen skill** - Improves test coverage
5. **FIFTH: android-migration-reviewer subagent** - Safeguards database changes

---

## Want More Recommendations?

Ask for:
- More MCP server options (Playwright for UI testing, Database MCP for SQL queries)
- Additional hooks (test auto-run, format on save)
- More skills (CSV import generator, APK deployment)

---

**Generated**: 2026-01-27
**For Project**: Banyg - Local-first Android Finance App
