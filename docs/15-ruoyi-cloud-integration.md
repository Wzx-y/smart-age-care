# RuoYi Cloud Integration Baseline

Status: source integration is complete and unverified. No local dependency installation, compilation, database migration, container startup, or test execution occurred.

## Included Platform

The official RuoYi Cloud `springboot3` source is included at `backend/ruoyi-cloud`.

- Upstream: `https://github.com/yangzongzhuan/RuoYi-Cloud.git`
- Snapshot commit: `cd323dfee84862dd887d080ca8782369719b2bdc`
- RuoYi version: `3.6.8`
- Runtime baseline: Java 17+, Spring Boot 3.5.14, Spring Cloud 2025.0.2, Spring Cloud Alibaba 2025.0.0.0, Nacos 3.x.

The aged-care service parent was aligned to Spring Boot 3.5.14 and now declares Nacos discovery. The platform and the aged-care services remain separate Maven roots inside one future Git repository; cloud CI must build them separately.

## Real Request Path

1. The browser authenticates through RuoYi Gateway/Auth.
2. For `/care/**`, the browser supplies a selected `X-Tenant-Id`.
3. `TenantContextFilter` removes browser-supplied internal headers, verifies the authenticated RuoYi user against RuoYi System's member directory, then adds trusted `X-Platform-User-Id`, `X-Platform-Tenant-Id`, and the server-only internal credential for `care-service`.
4. `care-service` first verifies the internal credential, then rechecks membership and role permissions through the same internal directory before every care write.

`RUOYI_MEMBER_DIRECTORY_URL` must point to the private RuoYi System address, for example `http://ruoyi-system:9201`. Both Gateway and `care-service` require the same server-only `PLATFORM_INTERNAL_AUTH_KEY`; the directory endpoint rejects missing or incorrect values.

## Tenant and Permission Model

Run `backend/ruoyi-cloud/sql/smart-age-care/001_tenant_member_directory.sql` after the official RuoYi system SQL. It creates `care_tenant_member`, which is the source of tenant membership and status. Permissions are calculated from standard RuoYi tables:

`care_tenant_member_role -> sys_role -> sys_role_menu -> sys_menu.perms`

Create RuoYi menu permissions for the care codes before assigning roles, including:

- `care:plan:manage`, `care:plan:publish`
- `care:task:manage`, `care:task:execute`
- `care:follow-up:manage`
- `care:handover:manage`, `care:handover:receive`
- `care:master-data:manage`
- `care:report:view`
- `care:export:manage`, `care:export:download`
- `care:notification:view`, `care:audit:view`

## Cloud Provisioning Order

1. Start real MySQL 8, Redis, and Nacos 3 in Codespaces or GitHub Actions.
2. Import RuoYi SQL from `backend/ruoyi-cloud/sql/`, then run `001_tenant_member_directory.sql` and `003_tenant_directory.sql`.
3. Load `002_gateway_care_route.sql` into the Nacos config database. It creates the required `care-gateway-routes-dev.properties` record; Gateway bootstrap imports it as a required configuration source.
4. Configure `NACOS_SERVER_ADDR`, `NACOS_USERNAME`, `NACOS_PASSWORD`, `CARE_DB_URL`, `CARE_DB_USERNAME`, `CARE_DB_PASSWORD`, `RUOYI_MEMBER_DIRECTORY_URL`, and `PLATFORM_INTERNAL_AUTH_KEY` only as cloud secrets or runtime variables. Auth, System and Gateway bootstrap files resolve the Nacos address and credentials from those variables instead of assuming a local Nacos instance.
5. Build RuoYi Cloud and the aged-care backend separately, start `ruoyi-system`, `ruoyi-auth`, `ruoyi-gateway`, and `care-service`, then run Flyway V1-V19 against the care database.
6. Verify login, tenant directory CRUD and switching, role denial, directory outage `503`, cross-tenant isolation, resident 360, admission state transitions, care execution, master-data uniqueness/status controls, report aggregation, authorized export download, notification read state and audit retention.

## Codespaces Full-Container Stack

`docker-compose.codespaces.yml` and `scripts/start-codespaces-stack.sh` provide a Codespaces-only full-container option. The script initializes an empty Docker volume from tracked RuoYi and aged-care SQL, builds Auth/System/Gateway and `care-service`, then starts the service JARs and Vite in one Compose network. Browser traffic reaches only Vite on port `5173`; Vite proxies `/gateway` to `ruoyi-gateway:8080`.

Secrets are read only from the ignored `.env.codespaces` file. The stack deliberately does not start `device-service`, because the device integration remains a skeleton without approved MQTT broker credentials. This option must not run beside the `infra/codespaces` startup path.

## Test Source

`TenantMemberDirectoryServiceTest` verifies the new System-side membership and permission aggregation source. Existing care-service tests cover care authorization and handover blocking. These test sources have not been executed yet.

## Remaining Production Work

The included RuoYi management UI remains upstream Vue code and is not the aged-care SaaS application UI. The React application remains the product-facing frontend. Before release, provision RuoYi menu/role records through a controlled migration or administrative workflow, execute the authenticated React tenant-switch and business workflows against the real platform, and record cloud test evidence in `docs/11-release-acceptance-report.md`.
