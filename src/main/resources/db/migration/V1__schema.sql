CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    gender        VARCHAR(10)  NOT NULL CHECK (gender IN ('MALE', 'FEMALE', 'DIVERSE')),
    password_hash VARCHAR(100) NOT NULL
);

CREATE TABLE products (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    brand       VARCHAR(100) NOT NULL,
    price_cents INTEGER      NOT NULL CHECK (price_cents >= 0),
    image_url   VARCHAR(500) NOT NULL
);

CREATE TABLE sale_campaigns (
    id        UUID PRIMARY KEY,
    headline  VARCHAR(255) NOT NULL,
    cta_label VARCHAR(100) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    active    BOOLEAN      NOT NULL DEFAULT FALSE
);

-- purchases snapshot product name and price at purchase time (no FK into products)
CREATE TABLE purchases (
    id           UUID PRIMARY KEY,
    user_email   VARCHAR(255) NOT NULL REFERENCES users (email),
    product_name VARCHAR(255) NOT NULL,
    price_cents  INTEGER      NOT NULL CHECK (price_cents >= 0),
    purchased_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_purchases_user_email ON purchases (user_email);
