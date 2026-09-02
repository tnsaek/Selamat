CREATE TABLE users (
    id BINARY(16) NOT NULL,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE TABLE profiles (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    display_name VARCHAR(100),
    bio VARCHAR(500),
    avatar_url VARCHAR(500),
    cover_image_url VARCHAR(500),
    street VARCHAR(255),
    city VARCHAR(120),
    state VARCHAR(120),
    country VARCHAR(120),
    website_url VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE roles (
    id BINARY(16) NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE user_roles (
    user_id BINARY(16) NOT NULL,
    role_id BINARY(16) NOT NULL,
    assigned_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT
);

CREATE TABLE refresh_tokens (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE password_reset_tokens (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    used_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_password_reset_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE posts (
    id BINARY(16) NOT NULL,
    author_id BINARY(16) NOT NULL,
    content TEXT,
    visibility VARCHAR(30) NOT NULL DEFAULT 'PUBLIC',
    status VARCHAR(30) NOT NULL DEFAULT 'PUBLISHED',
    comment_count BIGINT NOT NULL DEFAULT 0,
    reaction_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_posts_visibility CHECK (visibility IN ('PUBLIC', 'FOLLOWERS_ONLY', 'PRIVATE')),
    CONSTRAINT chk_posts_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'HIDDEN', 'DELETED'))
);

CREATE TABLE media (
    id BINARY(16) NOT NULL,
    uploader_id BINARY(16) NOT NULL,
    post_id BINARY(16),
    url VARCHAR(500) NOT NULL,
    media_type VARCHAR(30) NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    alt_text VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'VISIBLE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_media_uploader FOREIGN KEY (uploader_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_media_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE SET NULL,
    CONSTRAINT chk_media_type CHECK (media_type IN ('IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT')),
    CONSTRAINT chk_media_status CHECK (status IN ('VISIBLE', 'HIDDEN', 'DELETED'))
);

CREATE TABLE comments (
    id BINARY(16) NOT NULL,
    post_id BINARY(16) NOT NULL,
    author_id BINARY(16) NOT NULL,
    parent_comment_id BINARY(16),
    content TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'VISIBLE',
    reaction_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES comments (id) ON DELETE CASCADE,
    CONSTRAINT chk_comments_status CHECK (status IN ('VISIBLE', 'HIDDEN', 'DELETED'))
);

CREATE TABLE reactions (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    post_id BINARY(16),
    comment_id BINARY(16),
    reaction_type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_reactions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_reactions_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_reactions_comment FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE,
    CONSTRAINT uq_reactions_user_post UNIQUE (user_id, post_id),
    CONSTRAINT uq_reactions_user_comment UNIQUE (user_id, comment_id),
    CONSTRAINT chk_reactions_target CHECK (
        (post_id IS NOT NULL AND comment_id IS NULL)
        OR (post_id IS NULL AND comment_id IS NOT NULL)
    ),
    CONSTRAINT chk_reactions_type CHECK (reaction_type IN ('LIKE', 'LOVE', 'LAUGH', 'SAD', 'ANGRY'))
);

CREATE TABLE follows (
    id BINARY(16) NOT NULL,
    follower_id BINARY(16) NOT NULL,
    followed_id BINARY(16) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACCEPTED',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_follows_follower FOREIGN KEY (follower_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_follows_followed FOREIGN KEY (followed_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_follows_pair UNIQUE (follower_id, followed_id),
    CONSTRAINT chk_follows_not_self CHECK (follower_id <> followed_id),
    CONSTRAINT chk_follows_status CHECK (status IN ('PENDING', 'ACCEPTED', 'BLOCKED'))
);

CREATE TABLE messages (
    id BINARY(16) NOT NULL,
    sender_id BINARY(16) NOT NULL,
    recipient_id BINARY(16) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SENT',
    sent_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    delivered_at TIMESTAMP(6),
    read_at TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_messages_status CHECK (status IN ('SENT', 'DELIVERED', 'READ', 'DELETED'))
);

CREATE TABLE notifications (
    id BINARY(16) NOT NULL,
    recipient_id BINARY(16) NOT NULL,
    actor_id BINARY(16),
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    body VARCHAR(500) NOT NULL,
    target_type VARCHAR(50),
    target_id BINARY(16),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    read_at TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_notifications_type CHECK (
        notification_type IN ('FOLLOW', 'COMMENT', 'REACTION', 'MESSAGE', 'REPORT_UPDATE', 'SYSTEM')
    )
);

CREATE TABLE reports (
    id BINARY(16) NOT NULL,
    reporter_id BINARY(16) NOT NULL,
    resolver_id BINARY(16),
    target_type VARCHAR(50) NOT NULL,
    target_id BINARY(16) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    details TEXT,
    resolution_note TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resolved_at TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_resolver FOREIGN KEY (resolver_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_reports_target_type CHECK (target_type IN ('USER', 'POST', 'COMMENT', 'MESSAGE', 'MEDIA')),
    CONSTRAINT chk_reports_status CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'RESOLVED', 'REJECTED'))
);

CREATE TABLE moderation_audit_logs (
    id BINARY(16) NOT NULL,
    actor_id BINARY(16) NOT NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BINARY(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_moderation_audit_logs_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_moderation_audit_logs_target_type CHECK (target_type IN ('USER', 'POST', 'COMMENT', 'MESSAGE', 'MEDIA'))
);

CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_posts_author_created ON posts (author_id, created_at);
CREATE INDEX idx_posts_visibility_status_created ON posts (visibility, status, created_at);
CREATE INDEX idx_media_post ON media (post_id);
CREATE INDEX idx_media_status_created ON media (status, created_at);
CREATE INDEX idx_comments_post_created ON comments (post_id, created_at);
CREATE INDEX idx_comments_author_created ON comments (author_id, created_at);
CREATE INDEX idx_reactions_post ON reactions (post_id);
CREATE INDEX idx_reactions_comment ON reactions (comment_id);
CREATE INDEX idx_follows_follower_status ON follows (follower_id, status);
CREATE INDEX idx_follows_followed_status ON follows (followed_id, status);
CREATE INDEX idx_messages_sender_sent ON messages (sender_id, sent_at);
CREATE INDEX idx_messages_recipient_sent ON messages (recipient_id, sent_at);
CREATE INDEX idx_notifications_recipient_read_created ON notifications (recipient_id, is_read, created_at);
CREATE INDEX idx_reports_target ON reports (target_type, target_id);
CREATE INDEX idx_reports_status_created ON reports (status, created_at);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);
CREATE INDEX idx_moderation_audit_logs_target ON moderation_audit_logs (target_type, target_id);
