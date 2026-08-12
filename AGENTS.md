# Prototype Instructions

Run the local server yourself and open the preview in the browser available to this environment. Do not give the user server-start instructions when you can run it.

Before making substantial visual changes, use the Product Design plugin's `get-context` skill when the visual source is unclear or no longer matches the current goal. When the user gives durable prototype-specific design feedback, preferences, or decisions, record them in `AGENTS.md`.

When implementing from a selected generated mock, treat that image as the source of truth for layout, component anatomy, density, spacing, color, typography, visible content, and hierarchy.

## Current Product Direction

- The smart aged-care product must present as a restrained, technology-forward SaaS product, not a conventional back-office administration interface.
- Primary navigation lives in the top application bar. Do not introduce a permanent left-side navigation without explicit approval.
- Keep the first screen focused on daily operational decisions: care priorities, occupancy, care tasks, resident attention, and device alerts. Avoid cramming every module into a dashboard.
- The supplied legacy screenshot is domain context only. Do not reproduce its login layout, blue illustrated background, QR code, branding, or visual assets.
- Preserve responsive desktop and mobile behavior when extending this prototype.
- Every business module must include realistic sample records and support visible search/filter, detail, create, edit, and delete interactions in the prototype.
- Account functions must live under the avatar menu rather than the business navigation. The avatar menu must expose personal center, system management, and a confirmation-protected logout action.
- The authentication experience is a separate SaaS entry surface, not part of the application navigation. It must support login, organization registration, password recovery, demo entry, and a logout return path.
- The first operational enhancement phase prioritizes a resident 360 profile and a care-plan execution loop: assessment, risk signal, plan, daily task, service record, handover, and exception follow-up must remain visibly connected.
- System management remains an avatar-accessed workspace, never a primary business-navigation item. It must make tenant context, member roles, module permissions, data scope, and tenant-isolation status explicit; switching tenants must be visible and cannot imply unrestricted cross-tenant access.
- Phase 5 operational workflows must be decision-oriented: admission connects pending assessment, candidate selection, available bed allocation, and admission confirmation; device incidents connect alert priority, acknowledgement, dispatch, closure, and an auditable outcome. Do not reduce either workflow to a static list.
- A floating bottom-right care assistant is available throughout the authenticated workspace. It uses a warm, companion-oriented robot asset and provides system-operation and general aged-care knowledge guidance. It must state that it is informational only and never replace medical judgment, emergency response, or institutional policy.
- The assistant may show user-facing retrieval and answer-assembly states plus a typewriter response, but it must never expose private model reasoning. Device Center defaults to an equipment overview with asset, status, use, inspection, and maintenance context; alert handling is a distinct subordinate view rather than the landing view.
- After a user sends an assistant message, the current thinking state and newest answer must remain automatically visible without manual scrolling; preserve a spacious conversation area once chat history exists.
- Business closure takes precedence over server-side AI integration. Until the core resident, admission, care, organization, reporting, notification, and audit workflows are complete, the assistant may continue using constrained preset answers without blocking those workflows.
- Codespaces login uses account and password only; captcha is disabled in both the React login flow and the Gateway Nacos configuration.

## Cloud-First Engineering Rules

- Before modifying a feature, read the matching product, architecture, API, data-model, security, and test documentation under `docs/` together with the affected source files.
- Do not install runtimes, plugins, dependencies, databases, containers, or services on the local computer. Dependency installation, build, test, and deployment commands run only in GitHub Codespaces, GitHub Actions, or an explicitly authorized cloud environment.
- Never commit `.env` files, tokens, passwords, personal data, database dumps, `node_modules/`, `dist/`, or test artifacts.
- Every functional change must update its related documentation and tests. Before merge or release, GitHub must pass lint, type checks, unit tests, integration tests, production build, and Playwright end-to-end tests.
- Never claim that a cloud check, deployment, or production release succeeded without recorded evidence from the corresponding cloud environment.
