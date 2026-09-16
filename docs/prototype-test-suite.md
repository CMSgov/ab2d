# Pause/resume prototype - test suite

A plain-language map of what the prototype's tests cover. The point of this suite is to prove the
recovery logic is correct: if a worker dies, is redeployed, or is superseded mid-job, the job still
finishes once, with no duplicated, torn, or lost data.

Everything here runs in CI on every PR. The recovery tests use Testcontainers (a real Postgres and
LocalStack in Docker) and drive the real Spring Batch pipeline; beneficiary and BFD data are mocked.
There is no AWS or manual setup - a crash is simulated by killing the worker in-process and, where
needed, nudging the database into the state a real crash would leave behind.

Around 50 test cases across 12 test classes.

## Crash and recovery - the core

Does a job survive a worker dying mid-run?

- Crash at every stage (PrototypeCrashPointIntegrationTest): the worker is crashed during partitioning,
  reading, processing, file-writing, and assembly. Each time the job recovers with every beneficiary
  delivered exactly once and no torn or duplicated output.
- Hard crash recovery (PrototypeHardRecoveryIntegrationTest): a worker killed dead is picked up and
  finished, even when it left the job's bookkeeping in a broken or unknown state (recovery heals it first).
- Resume where it left off (PrototypeCopyForwardIntegrationTest): after a crash a half-done partition
  resumes from its last saved chunk instead of redoing all its work - and with that feature off, it
  correctly redoes it.
- Graceful pause and resume (PrototypeJobPauseResumeIntegrationTest): a clean shutdown (like a deploy)
  pauses the job and a later worker resumes from the checkpoint; a cancelled job stops promptly.

## Many workers not stepping on each other

If two workers touch the same job, does the right one win and the wrong one back off?

- Fencing a superseded worker (PrototypeFenceGuardIntegrationTest): when another worker takes over the
  lease, the old one is fenced off cleanly - it cannot corrupt the output - and the new owner finishes
  the job.
- Losing the lease (PrototypeJobLeaseExpirationIntegrationTest): a worker that stops heartbeating loses
  its lease, and a fenced worker's lease is removed. The 'a dead worker lets go' guarantee.
- Watching for stranded jobs (lease/PrototypeLeaseMonitorTest): the background monitor spots a job whose
  worker died and nobody picked up, alerts Slack once (not once per worker), stays quiet while a normal
  takeover is still expected, and never takes the worker down if the database hiccups.

## Correctness under bad data and load

- Bad or failing beneficiaries (PrototypeErrorAndSkipIntegrationTest): a beneficiary that can't be
  serialized is written to the error file and the job still succeeds; a persistently-failing one is
  skipped; but too many failures correctly fails the whole job.
- Parallel processing (PrototypeConcurrentItemIntegrationTest): beneficiaries in a chunk are processed
  in parallel, a transient failure retries only the failing beneficiary rather than the whole chunk, and
  a skipped beneficiary doesn't cost its chunk-mates.

## Can we see what happened?

- Alerts and recovery counts (PrototypeObservabilityIntegrationTest): the three failure paths (threshold
  tripped, out of resume attempts, launch failure) each alert Slack and are counted; a clean pause is
  counted as a soft recovery and a crash as a hard one; a success alerts nothing.
- Metric tagging (PrototypeMetricsTest): every job claim is tagged fresh/soft/hard, failure reasons are
  distinguished, lease health (active/stale/unrecovered) is gauged, and it safely does nothing when
  there's no metrics client.

## The dev crash tool

- Crash injector logic (CrashInjectorTest): the config-driven crash injector used to exercise recovery
  in a deployed worker - off by default, only fires at the configured stage, forgiving of casing, ignores
  unknown values, and refuses to arm outside dev/test/local.

## What this suite intentionally does not do

- It does not crash real containers or test that ECS/auto-scaling brings a worker back - that is AWS's
  job and already happens. What matters, and what is tested here, is that the replacement worker resumes
  the orphaned job correctly.
- It does not inject AWS-infrastructure faults (an Aurora failover, a network partition). Those are a
  separate, lower-priority concern; a one-off manual check (deploy to test, stop a real worker task
  mid-job, watch it recover) covers the deployment plumbing without an automated harness.
