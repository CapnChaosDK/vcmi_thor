# Thor candidate companion

The standalone [VCMI Builds app](https://github.com/CapnChaosDK/vcmi-builds)
(`dev.vcmi.builds`) consumes Thor candidate artifacts through GitHub. It discovers
candidate runs, verifies and installs APKs, launches the game, and stores and shares
exact-build validation results. Its source, Android CI, device tests, and releases
are maintained in the standalone repository. `vcmi_thor` owns the game APK,
validation plan, receipt, and artifact publication.

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
- Companion build, signing, and contract-test instructions are maintained in
  [VCMI Builds](https://github.com/CapnChaosDK/vcmi-builds).
- User check: newest/running/failed candidate visibility; download/cancel/retry and
  background/recreation; Android cancel/permission/install; exact installed identity;
  launch/return; Save result/drafts, restart/reboot, candidate switching and report sharing.

Coordinate changes to the artifact layout, receipt fields, or validation-plan
schema explicitly with both repositories. Check that the standalone app can
consume the proposed artifact and that supported older candidates still work.
Use the [Thor playbook](AYN_THOR_BUILD_PLAYBOOK.md) for game candidate and hardware
validation.
