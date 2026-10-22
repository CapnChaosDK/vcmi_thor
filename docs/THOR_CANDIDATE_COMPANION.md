# Thor candidate companion

The standalone `dev.vcmi.builds` helper supports the development loop: discover the
latest Thor candidate, verify and install the APK, launch the game, follow the
candidate's checks, and copy/share exact-build results. It does not merge or promote.

## Plan ownership and artifact contract

Before publishing a feature, the development agent updates `CI/thor-validation-plan.json`
from the slice's agreed acceptance criteria and current code. This is the structured
source for the **current** checklist; backlog contracts link to it. Historical backlog
checklists remain historical evidence. Never guess a plan from a commit title or
reuse an older build's passed results. Keep prerequisite saves/scenarios, controls,
steps, expected outcomes, failure cases, and relevant regression coverage together.

Schema version 1 requires `feature_id`, `title`, `summary`, `preparation`, and 1–50
`checks`, each with a unique `id`, `title`, `steps`, and `expected`. Optional
`limitations` explains unavailable coverage or known restrictions. IDs use lowercase
letters, numbers and hyphens (80 characters max); titles are bounded at 160 characters,
other text at 4000. No HTML, scripts, remote assets or automatic test execution are used.
The Python validator is `CI/thor_validation_plan.py`; helper-side validation enforces
the same limits. CI adds `candidate_commit` and `apk_sha256`, writes the packaged plan,
and adds `validation_plan_sha256` to the existing receipt. A missing/malformed source
plan fails preflight. This adds no workflow and changes no signing or compiler caches.

The companion checks the archive digest supplied by GitHub, the receipt's run, branch,
artifact, candidate, package, ABI and CI statuses, the APK checksum file and bytes,
the APK's actual package, and the plan's digest/build identity. The expected package
is `is.xyz.vcmi.thor`. A malformed or mismatched plan fails verification. Older genuine
artifacts without a plan may be verified/installed but cannot gain invented checklists;
they link to the backlog at their exact commit. Plan previews come from the exact
commit and are descriptive only; the packaged verified plan owns the validation session.

## Device behavior

Home shows recent runs of `thor-ci.yml` on the configured candidate branch, including
running/failed runs and the latest available successful candidate. GitHub results
are cached with a timestamp for offline viewing. Local candidates and results remain
available after the remote artifact expires. Settings retain the existing encrypted
token. Advanced generic/Release downloads remain separate and do not create verified
candidate-validation sessions.

A foreground service owns one bounded download, independent of activity recreation.
Users can cancel it from the notification. Failed/cancelled transfers never become
installable; process termination is reported as interrupted on the next launch and
can be retried. Partial transfers restart from the beginning. Verified candidates are
stored independently, and installed APKs are never changed automatically.

Android still confirms installation. The helper handles permission refusal, installer
cancellation and failure, and independently hashes the installed Thor `sourceDir` APK
rather than trusting versionName/versionCode (multiple candidates share those values).
If the APK is missing, unreadable, split, or different, validation cannot be confirmed.
Recording a result rechecks installed bytes. The helper launches the normal game app;
it does not take ownership of either game display or controller input.

Each check offers Not tested, Pass, Fail, Blocked and notes. Draft notes/status are
retained separately until Save result; confirmed results are atomically stored per
repository + full commit + APK checksum. Different candidates start with independent
results. Historic results remain readable/exportable. All checks must explicitly pass
for the summary to say all reported passed; Blocked and Not tested never count as Pass.
Reports include the commit, APK and plan hashes, run URL, per-result device/time/status
and notes. Share/Copy is user-initiated and needs no GitHub write permission. A report
is user testimony, not authorization to merge/promote or automatic backlog mutation.

## Validation and handoff

- Python tests: `python -m unittest discover -s CI -p test_thor_validation_plan.py`.
- Helper CI: assemble/lint release, compile the instrumentation APK, validate/package-contract tests, verify signature/checksum.
- Device contract tests: install the same-signed test APK and run
  `adb shell am instrument -w -e phase candidate dev.vcmi.builds.test/dev.vcmi.builds.TokenStoreInstrumentation`.
  Synthetic isolated files test good/legacy bundles; modified APK/archive/checksum,
  wrong receipt/plan/package/failed-CI rejection; exact-build result isolation,
  persistence, report content, blocked/pass summaries and path bounds. The installed
  Thor APK is only read for identity verification. Token tests remain separate.
- User check: newest/running/failed candidate visibility; download/cancel/retry and
  background/recreation; Android cancel/permission/install; exact installed identity;
  launch/return; Save result/drafts, restart/reboot, candidate switching and report sharing.

Keep helper source/build changes on the helper implementation branch until explicitly
merged into product. Integrate the companion packaging with the latest reviewed game
candidate without discarding concurrent work, then hand that exact reviewed integrated
SHA to the existing permanent candidate ref. Full ARM64 build and user hardware checks
remain required; local helper checks are not a game-package or hardware pass.
