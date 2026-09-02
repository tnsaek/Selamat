# Social Media Platform Design Progress Report

## 1. Current Status

The project has moved from planning into an implemented local MVP. The current system is a Spring Boot modular monolith with an Angular web client, MySQL persistence, Flyway migrations, JWT authentication, local media storage, and admin moderation/reporting workflows.

The implemented backend modules include:

- Authentication and JWT security
- Users, roles, and profiles
- Posts, feed, comments, reactions, and follows
- Media upload and serving from the local uploads directory
- Messages and conversations
- Notifications and unread counts
- Reports, admin review, moderation actions, and moderation audit/status views
- Shared exception handling, validation, and API response models

The Angular UI currently includes:

- Login, signup, forgot password, and reset password pages
- Feed creation, browsing, comments, reactions, and post media display
- Profile editing with profile image, cover image, bio, and structured address fields
- Discover/search and follow flows
- Messages page
- Notifications page with direct navigation support for post-related notifications
- Admin dashboard and report moderation pages

## 2. Persistence Design

The database is implemented with MySQL and Flyway migrations. Hibernate validates the schema at startup, so every mapped entity table and column must exist in the database.

Current notable persistence decisions:

- IDs use UUID values stored as compact binary values.
- Audit columns such as `created_at` and `updated_at` are expected to be supplied by the application or migration defaults.
- Profiles now use structured address fields: `street`, `city`, `state`, and `country`.
- The old profile `location` field has been removed from the application model and should be removed from the database by migration.
- Refresh tokens are persisted in the `refresh_tokens` table.
- Media metadata is stored in the database while uploaded files are stored on disk.

## 3. API Design

The OpenAPI document has been refreshed to match the implemented controller surface. It now includes authentication, password reset, users, profiles, posts, feed, comments, reactions, follows, media, messages, notifications, reports, and admin moderation endpoints.

Recent OpenAPI corrections include:

- Added missing password reset, user search, follow status, message detail/read, notification count/read-all, admin report summary/detail/target, and admin moderation endpoints.
- Removed stale `location` from profile schemas.
- Updated profile schemas to use structured address fields.
- Updated message schemas to use `sender` and `recipient`.
- Updated notification schemas to use `read`.
- Updated report schemas to use `reporter`, `resolver`, and `resolutionNote`.
- Validated YAML parsing, schema references, and duplicate operation IDs.

## 4. Design Artifacts

The design folder contains PlantUML diagrams and SQL design notes, but several artifacts are older than the current implementation and need to be refreshed.

Priority updates:

1. `system-architecture.puml` should represent the current local MVP architecture instead of future infrastructure.
2. `module-diagram.puml` should match the current backend module/package layout.
3. `core-entities-class-diagram.puml` should match the implemented entities and DTO relationships.
4. `database-schema.sql` and ER diagrams should match the latest Flyway schema.
5. Sequence diagrams should be checked against the current controller/service flows.
6. `mysql-er-diagram.puml` is empty and should either be populated or removed.

## 5. Testing Status

Backend and frontend tests have been expanded across many areas. UI coverage has improved substantially, but the latest reported frontend coverage is not yet 100%.

Latest reported UI coverage:

- Statements: 89.82%
- Branches: 85.49%
- Functions: 83.96%
- Lines: 93.08%
- Tests: 152 passing

Remaining frontend gaps are concentrated in larger component templates and complex page behavior, especially feed, admin reports, messages, notifications, and profile views.

## 6. Known Gaps

The current implementation is suitable as a local MVP, but several items are still future architecture rather than implemented behavior:

- External object storage and CDN for media
- Background queue processing
- Search index service
- External email, push, or SMS notification providers
- OAuth/social login
- Analytics integration
- Maps/geocoding integration
- AI or third-party media moderation
- Production deployment architecture
- End-to-end browser test suite

## 7. Next Design Updates

The next design update should be `system-architecture.puml`, followed by the module diagram and database/ER diagrams. Those diagrams should clearly separate implemented components from future/planned infrastructure so the design package does not imply features that are not currently built.
