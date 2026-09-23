# Kairos — instructions for Claude Code

This is a mature, released app: every push to `master` bumps the version and deploys to the Play Store
(`.github/workflows/android-ci.yaml`). Treat preventing regressions as the top priority.

The project rules below are mandatory and shared with the other AI tools:

@ARCHITECTURE.md
@.cursorrules
@.junie/guidelines.md

## Regression guardrails

- **Stay in scope.** Change only what the task requires. No drive-by refactors, renames, reformatting of
  untouched code, dependency bumps, or "cleanups" unless explicitly asked. If you spot something worth fixing,
  mention it instead of changing it.
- **Understand before changing.** Read the callers and the existing tests of any code you modify. Match the
  surrounding patterns (flat UseCases, single `StateFlow`, intents) rather than introducing new ones.
- **Tests are the spec.** Never delete, `@Ignore`, or loosen an existing test or assertion to make it pass. If a
  test breaks, assume the change is wrong first; only update the test when the behaviour change is intended,
  and say so explicitly in your summary.
- **Characterization first.** Before refactoring or changing logic that has no tests, add tests that pin the
  current behaviour, then make the change.
- **Every bug fix gets a regression test** that fails without the fix.
- **Do not weaken the gates:** no edits to `config/detekt/detekt.yml`, `*lint-baseline.xml`, `@Suppress`
  annotations, `.editorconfig` or Spotless config to silence findings — fix the code.
- **Public contracts are frozen** unless the task is about them: Room entities/migrations, DataStore keys,
  `Intent` extras/actions, notification channel IDs, deep links, Wear Data Layer paths and payloads,
  and exported components in `AndroidManifest.xml`. Changing them breaks installed users or the watch.
- **Phone ↔ Wear.** Changes in `:core` affect both `:app` and `:wear`; verify both compile and test.
- **Strings:** base language is Portuguese (`values/`); every new or changed string needs all 9 translations
  (`python3 check_strings.py` must pass).

## Git workflow

- Never commit or push to `master`. Work on a branch (`feat/…`, `fix/…`) and open a PR; CI must pass before merge.
- Keep commits small and focused, using the existing conventional-commit style (`feat:`, `fix:`, `docs:` …).

## Verification (automated)

`.claude/settings.json` enforces the gate with hooks:
- after each `.kt`/`.kts` edit, Spotless formats the file;
- before a turn that changed code ends, `spotlessCheck detekt testDebugUnitTest` (+ `sortDependencies` when
  build files changed) and `check_strings.py` run; failures must be fixed, not bypassed.

Still run `./gradlew lintDebug` for UI/manifest/resource changes, and for user-facing flows verify the behaviour
on a device or emulator before calling the task done.
