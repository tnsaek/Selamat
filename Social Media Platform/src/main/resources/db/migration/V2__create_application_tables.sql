CREATE TABLE IF NOT EXISTS users (
    id BINARY(16) NOT NULL,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS roles (
    id BINARY(16) NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BINARY(16) NOT NULL,
    role_id BINARY(16) NOT NULL,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS profiles (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    display_name VARCHAR(100),
    bio VARCHAR(500),
    avatar_url VARCHAR(500),
    cover_image_url VARCHAR(500),
    location VARCHAR(120),
    website_url VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_profiles_user_id (user_id),
    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS posts (
    id BINARY(16) NOT NULL,
    author_id BINARY(16) NOT NULL,
    content TEXT,
    visibility VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    comment_count BIGINT NOT NULL,
    reaction_count BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_posts_author_id (author_id),
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS comments (
    id BINARY(16) NOT NULL,
    post_id BINARY(16) NOT NULL,
    author_id BINARY(16) NOT NULL,
    parent_comment_id BINARY(16),
    content TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    reaction_count BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_comments_post_id (post_id),
    KEY idx_comments_author_id (author_id),
    KEY idx_comments_parent_comment_id (parent_comment_id),
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES comments (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS follows (
    id BINARY(16) NOT NULL,
    follower_id BINARY(16) NOT NULL,
    followed_id BINARY(16) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_follows_follower_id (follower_id),
    KEY idx_follows_followed_id (followed_id),
    CONSTRAINT fk_follows_follower FOREIGN KEY (follower_id) REFERENCES users (id),
    CONSTRAINT fk_follows_followed FOREIGN KEY (followed_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS media (
    id BINARY(16) NOT NULL,
    uploader_id BINARY(16) NOT NULL,
    post_id BINARY(16),
    url VARCHAR(500) NOT NULL,
    media_type VARCHAR(30) NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    alt_text VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_media_uploader_id (uploader_id),
    KEY idx_media_post_id (post_id),
    CONSTRAINT fk_media_uploader FOREIGN KEY (uploader_id) REFERENCES users (id),
    CONSTRAINT fk_media_post FOREIGN KEY (post_id) REFERENCES posts (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS messages (
    id BINARY(16) NOT NULL,
    sender_id BINARY(16) NOT NULL,
    recipient_id BINARY(16) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    sent_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    delivered_at DATETIME(6),
    read_at DATETIME(6),
    PRIMARY KEY (id),
    KEY idx_messages_sender_id (sender_id),
    KEY idx_messages_recipient_id (recipient_id),
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id),
    CONSTRAINT fk_messages_recipient FOREIGN KEY (recipient_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notifications (
    id BINARY(16) NOT NULL,
    recipient_id BINARY(16) NOT NULL,
    actor_id BINARY(16),
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    body VARCHAR(500) NOT NULL,
    target_type VARCHAR(50),
    target_id BINARY(16),
    is_read BIT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    read_at DATETIME(6),
    PRIMARY KEY (id),
    KEY idx_notifications_recipient_id (recipient_id),
    KEY idx_notifications_actor_id (actor_id),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id),
    CONSTRAINT fk_notifications_actor FOREIGN KEY (actor_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reactions (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    post_id BINARY(16),
    comment_id BINARY(16),
    reaction_type VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_reactions_user_id (user_id),
    KEY idx_reactions_post_id (post_id),
    KEY idx_reactions_comment_id (comment_id),
    CONSTRAINT fk_reactions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reactions_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_reactions_comment FOREIGN KEY (comment_id) REFERENCES comments (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reports (
    id BINARY(16) NOT NULL,
    reporter_id BINARY(16) NOT NULL,
    resolver_id BINARY(16),
    target_type VARCHAR(50) NOT NULL,
    target_id BINARY(16) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    details TEXT,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resolved_at DATETIME(6),
    PRIMARY KEY (id),
    KEY idx_reports_reporter_id (reporter_id),
    KEY idx_reports_resolver_id (resolver_id),
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id),
    CONSTRAINT fk_reports_resolver FOREIGN KEY (resolver_id) REFERENCES users (id)
) ENGINE=InnoDB;
