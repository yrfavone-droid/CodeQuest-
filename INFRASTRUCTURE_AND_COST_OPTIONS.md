# Archived cloud infrastructure and cost option — do not implement in the local-first release

The current product version requires no paid API, cloud account, remote database, or hosted execution service. See `LOCAL_FIRST_ARCHITECTURE.md`.

# CodeQuest Academy — Infrastructure and Cost Options

**Pricing retrieval date:** 2026-08-29. Prices change; this is a planning estimate, not a vendor quote or commitment. No service is being provisioned in Phase 01.

## Recommended starting topology

| Layer | Development | Staging | Production |
| --- | --- | --- | --- |
| Public site/downloads | Current Vercel project or equivalent static host | Separate preview/staging deployment | Existing public deployment with release compatibility preserved |
| Accounts/content/progress | Separate Supabase dev project | Separate Supabase staging project | Separate Supabase production project with RLS, backups, monitoring |
| Desktop data | Local SQLite fixture/cache | Isolated test cache | SQLite offline cache; no local source deletion |
| Browser exercises | Local/WebAssembly sandbox | Same signed bundles | Same signed bundles and versioned assets |
| Advanced execution | Not enabled | Isolated security POC | Queue plus hardened ephemeral runner; separately billed and monitored |
| Observability | Developer logs | Alert rehearsal | Centralized metrics/logs, crash reporting, backup-restore evidence, incident runbooks |

Production must have its own project, keys, database, asset bucket, execution identity, and alert routing. Development and staging must never share production secrets. Region selection is an owner/legal decision based on learner residency, latency, and data-processing obligations.

## Current official reference prices

- Supabase Pro starts at **US$25/month** and includes the first project, 100,000 MAU, 8 GB database disk, 250 GB egress, 100 GB storage, daily backups retained seven days, and US$10 compute credit. Additional included thresholds and overages are published by Supabase; point-in-time recovery begins at US$100/month per seven-day retention. [Supabase pricing](https://supabase.com/pricing)
- Google Cloud Run is usage-based. Its published `us-central1` service reference lists the first 240,000 vCPU-seconds and 450,000 GiB-seconds monthly free, then US$0.000024 per active vCPU-second, US$0.0000025 per active GiB-second, and US$0.40 per million requests; regional pricing differs. Cloud Run jobs have a one-minute minimum, so it must be benchmarked for queued sandbox workloads. [Cloud Run pricing](https://cloud.google.com/run/pricing)

These references describe general managed compute, not a proof that Cloud Run alone safely runs arbitrary learner code. Any selected runner must add a tested isolation layer and a security review.

## Planning assumptions

The reported **7,000+** number is downloads/explorers, not a verified monthly-active count. To avoid inventing traction, the scenarios below use two explicit planning cases: **7,000 monthly active learners (MAU)** and **10,000 MAU**.

Baseline estimate assumptions:

- 100 durable learning events per MAU/month (700,000 or 1,000,000 events);
- 10% of MAU use advanced execution, averaging 20 advanced jobs/month (14,000 or 20,000 jobs);
- median advanced job: 10 seconds, 1 vCPU, 1 GiB RAM, single concurrency; browser exercises are client-side and have no runner bill;
- 5–15 GB database, 25–100 GB content/files, and disciplined retention/redaction;
- costs exclude paid AI model/tutor usage, SMS, enterprise compliance, taxes, support staff, and one-time engineering/security review.

## Monthly planning ranges (USD)

| Cost area | 7,000 MAU planning case | 10,000 MAU planning case | What changes the number |
| --- | ---: | ---: | --- |
| Supabase production baseline | $25–$60 | $25–$85 | Pro base, compute size, database/storage/egress overage; both MAU counts fit the quoted 100k Pro MAU allowance |
| Supabase development + staging | $0–$50 | $0–$50 | Free projects are suitable only for non-production experiments and pause when inactive; paid isolation is safer for staging |
| Public website/release hosting | $0–$50 | $0–$100 | Existing host plan, installer bandwidth, traffic, and retention |
| Advanced execution compute | $0–$75 | $0–$125 | Free tier, job duration, RAM, concurrency, warm capacity, network/storage, and hardened-runner overhead |
| Monitoring/log retention | $0–$100 | $25–$200 | Event volume, traces, crash reporting, alert retention, and log redaction |
| Backups/object storage | $5–$40 | $10–$75 | Book PDFs, content media, audit retention, restore copies |
| **Indicative recurring platform total** | **$30–$375/month** | **$60–$635/month** | Excludes AI tutor tokens, people, and security/compliance engagements |

The runner estimate is deliberately a range. Under the stated 10-second/1-vCPU/1-GiB assumption, 14,000 jobs use roughly 140,000 vCPU-seconds and 140,000 GiB-seconds; 20,000 jobs use roughly 200,000 of each. Both can fall near Cloud Run’s published free allowances in a qualifying region before platform overhead. Real ML/deep-learning jobs, warm instances, queue workers, GPUs, longer time limits, and hardened microVM capacity can increase the cost sharply. Benchmarking the POC is mandatory before setting an execution budget.

## Options and trade-offs

| Option | Suitable use | Benefits | Risks / decision needed |
| --- | --- | --- | --- |
| Supabase Pro + browser-only sandbox initially | Foundation/Python/math launch | Lowest operational cost; fast offline-first rollout | No advanced packages/ML execution; still requires RLS and sync POCs |
| Supabase Pro + queue-backed managed runner | Initial advanced labs after POC | Auto-scale potential and metered cost | Must prove sandbox isolation; do not equate a container host with a secure code judge |
| Dedicated microVM runner platform | High-risk arbitrary code / higher concurrency | Stronger isolation boundary and clearer tenancy controls | Higher engineering/operations cost; provider selection and security review required |
| GPU/on-demand notebook environment | Curated deep learning labs only | Supports realistic ML workloads | Cost can dominate; require strict quotas, curated images/datasets, per-user budgets, and wait queues |

## Cost controls and recovery controls

- Keep new remote capabilities feature-flagged and introduce per-user/day execution quotas before opening them broadly.
- Set vendor budgets, usage alerts, concurrency caps, request rate limits, storage lifecycle policies, and a hard maximum job duration.
- Send analytics as sampled/aggregated operational telemetry where full fidelity is not needed; never log raw learner code or sensitive answers by default.
- Test restore procedures in staging. Supabase Pro’s listed seven-day backup retention is a baseline, not a replacement for an owner-approved recovery policy.
- Reforecast after POC measurements using real job duration, memory, storage, egress, database, and crash/error metrics; do not use download count as MAU.

## Approval requested before Phase 02

1. Approve using a Pro-grade production Supabase plan and independent development/staging projects when implementation begins.
2. Approve a bounded POC budget for the runner and monitoring (recommend an initial ceiling of US$100–$250 total for non-production experiments, subject to provider billing setup).
3. Select preferred production data region and confirm whether any learner-residency/compliance constraints apply.
