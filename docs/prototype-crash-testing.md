# Prototype crash testing runbook

The pause/resume prototype worker has recovery paths for crashes at each stage of a job
(partitioning, reading, processing, file writing, assembly). Unit and integration tests cover these
in-process. This runbook is for the other half: proving a **deployed** worker container recovers from a
real crash without corrupting or duplicating output.

Crash injection is off by default and is only meant for `dev`/`test`. **Never arm it in prod.**

## How the injector works

`CrashInjector` halts the JVM (`Runtime.getRuntime().halt(137)`) at a chosen pipeline point, so the
container dies exactly like a real crash - no shutdown hooks, no cleanup. It is controlled by two
properties:

| Property | Env var | Meaning |
| --- | --- | --- |
| `pause-resume.prototype.crash-probability` | `PAUSE_RESUME_PROTOTYPE_CRASH_PROBABILITY` | Chance (0-1) of crashing each time the armed point is hit. `0` = off. |
| `pause-resume.prototype.crash-at` | `PAUSE_RESUME_PROTOTYPE_CRASH_AT` | Which point crashes: `process`, `read`, `write`, or `assemble`. |

A small probability (e.g. `0.01`) crashes somewhere in the middle of a large job, which is the
interesting case for recovery. `1.0` crashes at the first opportunity.

## Option A: crash the worker from the inside (injector)

Use this to test a crash at a specific pipeline stage.

1. Point the worker task at a crash stage and probability. In `dev` you can set these as env vars on the
   worker ECS task definition (they flow into Spring via relaxed binding - no code change needed):

   ```
   PAUSE_RESUME_PROTOTYPE_CRASH_AT=write
   PAUSE_RESUME_PROTOTYPE_CRASH_PROBABILITY=0.02
   ```

2. Redeploy the worker so the new task definition is live.
3. Submit a job large enough to span several chunks.
4. Watch the task die and ECS start a replacement. The replacement picks the job back up (hard recovery:
   new lease token, new files) and finishes it.
5. **Reset `PAUSE_RESUME_PROTOTYPE_CRASH_PROBABILITY=0` and redeploy** when you are done.

Repeat with `crash-at=read`, `process`, and `assemble` to exercise each stage.

## Option B: crash the container from the outside (AWS FIS / ECS)

Use this to test an infrastructure-level kill (task stopped mid-job) rather than a specific stage.

- **AWS FIS**: run an experiment with the `aws:ecs:stop-task` action targeting a running worker task
  while a job is in progress.
- **Manual equivalent**: find the running task and stop it:

  ```
  aws ecs list-tasks --cluster <worker-cluster> --service-name <worker-service>
  aws ecs stop-task --cluster <worker-cluster> --task <task-arn> --reason "crash recovery test"
  ```

ECS replaces the task; the replacement recovers the in-flight job the same way.

## Watch it in the logs

The crash/recovery lifecycle is logged with greppable markers so you can follow it end to end. In order,
you should see:

| Marker | What it means |
| --- | --- |
| `CRASH-INJECTION ARMED at '<point>'` | the worker started in crash-test mode (logged once at startup) |
| `CRASH-INJECTION firing at '<point>'` | the crash just happened - the worker is halting (exit 137) |
| `RECOVERY for job <uuid>: HARD RECOVERY` | a replacement worker fenced the dead one and is redoing incomplete partitions |
| `finished with status COMPLETED` | the job's batch run completed on the replacement |
| `assembly for job <uuid>: ... new JobOutput row(s)` | the final output was assembled and delivered |

A quick way to pull the whole story for a job:

```
grep -E "CRASH-INJECTION|RECOVERY for job|finished with status|assembly for job" <worker-logs>
```

(A graceful shutdown logs `SOFT RESUME` instead of `HARD RECOVERY` - same job, no lost work, resumed
from the last checkpoint.)

## What to verify after recovery

For the job that was interrupted:

- Job status ends `SUCCESSFUL` (not stuck `IN_PROGRESS` or `FAILED`).
- Output has **every beneficiary exactly once** - no missing benes, no duplicates.
- No torn lines in the output files (every line is a complete resource).
- No duplicate `job_output` rows for the job.

These are the same guarantees the integration tests assert
(`assertEveryBeneExactlyOnceInOutput` and the crash-point tests); this runbook confirms they hold on a
real container too.
