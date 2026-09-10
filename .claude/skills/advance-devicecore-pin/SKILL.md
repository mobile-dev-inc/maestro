---
name: advance-devicecore-pin
description: Use when moving Maestro's committed device-core pin (devicecore.version) to a newer
  device-core sha, and the bump changes the gateway seam — a verb device-core now serves, or a
  device-core verb it RETYPED. Covers picking + verifying the sha, publish/resolve, wiring the
  RealDeviceGateway delta, the contract-change check on Orchestra's default injections, and the clean
  CLI build that gates every downstream host run. Symptoms — devicecore.version bump, RealDeviceGateway
  NotImplemented verb to wire, setPermission/waitUntilGone/waitForSettle, "green compile" on a bump.
---

# Advance the device-core pin and wire the seam for the delta

Moving the pin is not "just wire the new verbs." A bump has one job: express the DELTA between the
old pin and the new one in the `RealDeviceGateway` seam, then prove it with a clean CLI build — the
local, cheap half of the work. The remote, expensive half (the fidelity corpus) is a SEPARATE skill
(`remote-differential-batch`) that this one hands off to at gate **G1**. Everything here happens
before a single host is touched.

**Violating the letter of the gates below is violating the spirit.** Each one is cheap; each one
guards a failure whose only other detector is a corpus of phantom divergences handed to triage as if
they were device-core's.

## Assume up-to-pin correct — your job is the delta

Everything already wired to the OLD pin was wired correctly. Do not re-audit it, do not "fix" wiring
that looks off, do not widen scope to pre-existing seam bugs. The bump's surface is exactly:
the verbs the new sha newly SERVES, and the verbs it RETYPED. That set — and only that set — is what
you wire. A pre-existing misrouting is a separate cleanup backlog item, not this bump's work.

## When to use

- The committed `devicecore.version` is moving to a newer device-core sha.
- That sha serves a verb `RealDeviceGateway` currently throws `NotImplemented` for, OR retypes a
  verb the seam already calls.

**When NOT to use:** a no-op pin refresh that changes no seam-visible contract (just edit
`devicecore.version` and rebuild); the corpus run itself (that is `remote-differential-batch`);
fixing a pre-existing wiring bug unrelated to the delta (backlog it).

## Preconditions

Know two things before you start, or stop and get them:

1. **The target device-core sha**, and that it is the sha you mean to pin — on `main`, not a WIP
   worktree branch (`git -C <device-core> branch --contains <sha>`), clean tree (no `-dirty`).
2. **The delta**: which verbs the bump is FOR. For each, whether device-core has actually REALIZED
   and scored it on the target platform — a claim you verify (below, step 1), not one you take from a
   changelog or a doc comment.

## The ordered process

**1. Pick the sha and verify each delta verb is REALIZED at it — not just present.**
`git -C <device-core> log <sha>` carries the verbs this bump is for. Then, per verb, open the
adaptor body at that sha (`AndroidDevice.kt` / `AndroidScreen.kt` / `AndroidLocator.kt`, iOS
equivalents) and confirm it does not throw the reserved `NotImplementedError`.

> **Trap — the doc comment lags the tip commit.** An `Api.kt` docstring can still say a verb is
> "currently OWED — the adaptor throws NotImplementedError" while the very commit you are pinning is
> the one that rebuilt it. Read the ADAPTOR/STRATEGY body, not the prose above the interface method.
> "Android-scored" is a fact you confirm at the sha, never assume.

A verb still throwing at your sha is not wireable this bump — either wall it honestly (the seam's
`DeviceCoreErrorMapper` already maps `NotImplementedError` → `MaestroException.NotImplemented`, so an
unrealized platform walls cleanly) or don't claim it.

**2. Publish + resolve the local override.**
`DEVICECORE_DIR=<device-core> ./scripts/devicecore-sync.sh` — builds device-core to mavenLocal and
writes the gitignored `devicecore.version.local` (the local override the orchestra build prefers).
Confirm it printed the CLEAN version string (`0.1.0-<sha12>`, no `-dirty`).

**3. Set the committed pin to the CLEAN sha (G2).**
Edit `devicecore.version` to `0.1.0-<sha12>`. Leave `devicecore.version.local` for your own build;
the committed file is what the pool and everyone else resolves.

**4. Wire the delta in `RealDeviceGateway` — classify each verb first.**

| Delta kind | What it means | What you do |
|---|---|---|
| **Addition** | device-core now serves a verb the seam throws `NotImplemented` for | Add the override on the `clearAppState`/`stopApp` template: translate Maestro args → device-core vocab, `runBlocking`, route throwables through `DeviceCoreErrorMapper.mapInfraThrow`, read the `Outcome`/evidence for the verdict. |
| **Contract change** | device-core RETYPED a verb the seam ALREADY calls | This is an edit to shipped code that **will not compile** untouched. Re-translate the existing passthrough to the new type. |

Extend `SelectorTranslator` only if a verb needs a new selector field. A verb whose value/shape has
no clean device-core equivalent walls (`NotImplemented`) rather than degrading silently.

**Green compile ≠ same behavior.** Read the device-core decision notes for each verb; a retype can
invert polarity or change meaning while compiling clean. (E.g. an inverted-postcondition wait reads
`Acted` as its PASS and `Absent` as its FAIL — backwards from the sibling verb.)

**5. Build to G1.**
`./gradlew :maestro-cli:installDist -x buildMcpViewer --refresh-dependencies`, then confirm
`maestro-cli/build/install/maestro/bin/maestro --version`. `--refresh-dependencies` always — it
defeats a stale mavenLocal jar cached under a reused version string.

## The three hard gates

### G1 — CLEAN BUILD BEFORE ANY HOST (RED-LINE)

A red build STOPS everything: no corpus run, no smoke, no PR. The corpus dispatches a BUILT artifact;
a red build has nothing to dispatch, and a green editor/incremental compile is not the shipped
artifact. G1 is the seam between this skill and `remote-differential-batch` precisely because one side
is local and cheap and the other is remote and expensive — you do not cross it on red.

### G2 — the committed pin is a CLEAN sha

`devicecore.version` is never a `-dirty` string. `-dirty` means the published bytes came from an
uncommitted device-core tree no one else can reproduce. The local override may be anything while you
iterate; the COMMITTED pin is clean or the bump does not ship.

### G3 — the contract-change check: Orchestra's DEFAULT injections must still have a device-core home

**This is the check that costs a whole bump when skipped, and the one an isolated per-verb audit
misses.** A retyped or removed verb does not break only its own flows — Orchestra INJECTS defaults
into nearly every flow, so one broken verb can break the ENTIRE corpus. On every bump, confirm each
default injection still routes somewhere real:

| Injection site | What Orchestra injects | Fires on |
|---|---|---|
| `Orchestra.kt` `launchApp` default | `driver.setPermissions(appId, mapOf("all" to "allow"))` | ~every launch |
| `Orchestra.kt` `clearAppState` | `driver.setPermissions(appId, mapOf("all" to "unset"))` | ~every clearState |

If the bump retypes the verb an injection lands on (here, `setPermissions`), the injected default is
the FIRST thing that must still work — trace `all:allow` and `all:unset` all the way to a real
device-core call and confirm they land. Get this wrong and every `launchApp`/`clearState` in the
corpus fails, and the differential reads it as a device-core divergence. Line numbers drift; find the
sites by `grep -n 'mapOf("all"' Orchestra.kt` each time rather than trusting a remembered line.

## Other failure modes to bake in

- **Meaning, not just shape.** A verb can change what it MEANS (polarity, default, scope) with the
  same or a compatible signature. The device-core diff's decision notes say which; a green compile
  does not.
- **`--cli-3x-dir` must point at THIS build.** `batch_differential.py build` DEFAULTS `--cli-3x-dir`
  to a sibling worktree, not yours. Handing off, pass `--cli-3x-dir <this worktree>` or the corpus
  runs someone else's build and your wiring is never exercised.
- **mavenLocal-only is fine for the remote run.** `devicecore-sync.sh` publishes to this machine's
  `~/.m2` only, but the remote hosts run the pre-built installDist tree (the resolved jar baked in),
  not a re-resolve — so a mavenLocal-only publish does not strand the corpus. Do not chase a "publish
  to a shared repo" step the pipeline does not need.
- **Attribution hygiene.** The commit and PR carry NO session/agent/`Co-Authored-By`/`Claude-Session`
  trailer and no "generated with" note. Pin bump + gateway wiring in one commit; PR body is
  `## Why` / `## Approach` / `## Verification`.

## Rationalizations — STOP if you catch yourself here

| Excuse | Reality |
|---|---|
| "These are small additions, existing wiring is fine" | A retyped verb is a contract change to code you already ship — it will not compile, and can be wrong when it does. Classify each delta (step 4) before touching anything. |
| "The compile is green, ship it to the corpus" | A green compile is the weakest signal for a bump whose risk is runtime polarity/mapping. It proves none of the three verbs got their meaning right. |
| "I checked all three verbs, they're correct" | Per-verb correctness is not G3. Trace Orchestra's `all:allow`/`all:unset` injections to a real device-core call — that is the check that breaks the whole corpus, not one verb. |
| "The docstring says this verb is realized/owed" | The doc comment lags the tip commit. Read the adaptor body at the pinned sha. |
| "It's late / the host is waiting / lead says trivial" | Tiredness and a claimed host are the argument FOR the gates. G1/G3 are minutes; a phantom corpus is hours of host time plus a day triaging your own bug. |
| "I'll pin the -dirty build just to test remotely" | G2. The committed pin is clean or it does not ship. Iterate on the local override. |

## Red flags — any of these means STOP

- About to run smoke/corpus on a red or unbuilt CLI (G1).
- Committing `devicecore.version` with a `-dirty` suffix (G2).
- Wired the retyped verb but never traced Orchestra's default injections (G3).
- Trusting "realized" from a changelog/docstring instead of the adaptor body at the sha.
- Widening scope to a pre-existing seam bug the delta didn't introduce (backlog it instead).
- A `Co-Authored-By`/`Claude-Session`/session-link trailer on the commit or PR.

## Handoff (at G1)

Once G1 is green: hand to **`remote-differential-batch`** for the corpus run, passing
`--cli-3x-dir <this worktree>`. Its smoke gate is the go/no-go for the full fan-out; when the smoke
folder can, pick one that exercises a newly-wired verb so smoke proves the verb lights up rather than
walls. Then `triage-batch` / `triage-one` on the `genuine-fidelity` bucket. This skill ends here.
