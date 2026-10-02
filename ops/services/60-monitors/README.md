# 60-monitors

Datadog monitors for AB2D, built on the CDAP
[`datadog_monitors`](https://github.com/CMSgov/cdap/tree/main/terraform/modules/datadog_monitors)
module. `config/defaults.yml` is the baseline; `config/<env>.yml` overrides it. Keys are merged one
level deep, so an env file that sets `notifications:` replaces the whole `notifications` block from
the defaults, not just the keys it names.

A merge to `main` applies dev → test → sandbox → prod in one run (`.github/workflows/monitors.yml`).
There is no "lower environments only" deploy: anything meant to stay out of prod has to be turned
off in `config/prod.yml`.

## VictorOps

Every monitor notifies Slack (`@webhook-slack-ab2d`). Monitors marked `critical = true` also page
VictorOps, when `victorops_critical: true` is set for that environment.

| Environment | `victorops_critical` |
|-------------|----------------------|
| dev, test, sandbox | `true` |
| prod | `false` — turn on once the lower-environment rollout is reviewed |

`victorops_critical` is an AB2D setting. It is separate from the CDAP module's
`notifications.victorops`, which is all-or-nothing: it would page for every module monitor,
including warning-level CPU, S3 4xx and Lambda alerts. Leave that one `false`.

A critical monitor's message gets these handles, from the module's `victorops_notify` output:

```
{{#is_alert}}@webhook-victorops-ab2d-critical{{/is_alert}}
{{#is_warning}}@webhook-victorops-ab2d-warning{{/is_warning}}
{{#is_recovery}}@webhook-victorops-ab2d-recovery{{/is_recovery}}
{{#is_no_data}}@webhook-victorops-ab2d-critical{{/is_no_data}}
```

The webhooks are owned by CDAP (`terraform/services/505-datadog-webhooks`). **All environments
share one VictorOps routing key** (`/cdap/prod/datadog/victorops_webhook_urls/ab2d`), so a dev page
reaches the same on-call rotation as a prod page. The `[DEV]`/`(dev)` in the monitor name is the
only way to tell them apart.

### Which monitors are critical

A monitor is critical when it means customers are getting no service or wrong data, or data is
about to be lost for good, and someone needs to act before the next business day.

| Monitor | Critical | Why |
|---------|----------|-----|
| ECS — Running Tasks Below Desired | yes | A service is down or degraded. |
| ECS — Tasks Stuck Pending | yes | Tasks cannot start, so a failed task is not being replaced. |
| Coverage V3 — Sync failures detected | yes | Coverage served to customers may be wrong. |
| Coverage V3 — Import row delta anomaly | yes | The IDR extract was probably incomplete, so coverage is missing rows. |
| Coverage V3 — Historical sync failures detected | yes | The archive copy failed. The archive window is a few hours a month; missing it loses the month. |
| Coverage V3 — Coverage below retention cutoff missing from historical | yes | Coverage is about to be aged out without having been archived. |
| Coverage V3 — Coverage preserved by the staging-copy guard | no | The guard worked; nothing was lost. Follow up in business hours. |
| Coverage V3 — Import staged no rows on a scheduled import day | no | Data is a day stale, not wrong. It is evaluated at 23:45 UTC. |
| All CDAP module monitors (ECS CPU/memory, SQS, SNS, Lambda, S3, RDS) | no | The module cannot route a subset of its monitors (see follow-ups). |

To make a monitor critical, add `critical = true` to it in `main.tf` or `ecs.tf`. This only works
for monitors in `custom_monitors`; a standalone `datadog_monitor` resource has to add
`local.victorops_critical_notify` to its message itself.

### Testing

```
bin/victorops-alert-test dev          # read-only: webhooks exist, which live monitors page
bin/victorops-alert-test dev --fire   # page once per critical monitor, then resolve
```

The read-only check reads the live monitors from Datadog. Use it after every apply; a correct plan
is not proof, because the CDAP module silently drops settings it does not recognise.

`--fire` creates a temporary copy of each critical monitor with the same name and message and a
query that is always true. Each copy sends a CRITICAL, then is made to recover (RECOVERY), then is
deleted. The real monitors are not changed. **It pages the real on-call rotation — tell them first.**
Then check VictorOps: there should be one incident per monitor, titled
`[VICTOROPS TEST] <monitor name>`, each resolved by its recovery. Datadog can only show that it
called the webhook, not that VictorOps accepted the call, so VictorOps is the real check.

Record results here:

| Monitor | dev | test | sandbox |
|---------|-----|------|---------|
| ECS — Running Tasks Below Desired | not run | not run | not run |
| ECS — Tasks Stuck Pending | not run | not run | not run |
| Coverage V3 — Sync failures detected | not run | not run | not run |
| Coverage V3 — Import row delta anomaly | not run | not run | not run |
| Coverage V3 — Historical sync failures detected | not run | not run | not run |
| Coverage V3 — Coverage below retention cutoff missing from historical | not run | not run | not run |

### Known gaps and follow-ups

1. **One routing key for all environments.** Lower-environment pages wake the real on-call. Ask
   CDAP for a separate non-prod routing key (a second SSM parameter under
   `/cdap/prod/datadog/victorops_webhook_urls/`), or turn `victorops_critical` off in dev and test
   once testing is done.
2. **The CDAP module monitors cannot be scoped.** `notifications.victorops` is one switch for all
   of them, warnings included. If any of them should page (RDS freeable memory is the likeliest),
   either ask CDAP for a per-service VictorOps flag, or rebuild that monitor in `custom_monitors`
   and mark it critical.
3. **No Data pages as critical.** The module maps `is_no_data` to the critical webhook.
   "Running Tasks Below Desired" uses `show_and_notify_no_data`, so a gap in the AWS ECS metrics
   (which are sparse and late) pages on-call even when nothing is wrong. Watch for this in dev
   before enabling prod.
4. **Row delta anomaly is grouped by contract.** One bad IDR extract can open one incident per
   contract.
5. **Module pin.** This service pins CDAP `f6fe4544`. Later CDAP versions replace `shadow_mode`
   with `draft_status`; bumping the pin will need `shadow_mode` replaced here too, including in
   `local.victorops_critical_notify`.
