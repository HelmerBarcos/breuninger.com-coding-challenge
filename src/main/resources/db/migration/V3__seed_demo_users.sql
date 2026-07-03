-- Demo users. Shared demo password: 'breuninger-demo' (BCrypt below).
-- Explicitly non-production data - see README. Every user owns mock purchases
-- (same invariant the registration use case maintains).

INSERT INTO users (id, email, first_name, last_name, gender, password_hash) VALUES
    ('33333333-0000-0000-0000-000000000001', 'admin.inspoteam@breuninger.com', 'Admin', 'INSPO Team', 'DIVERSE', '$2y$10$0g7SK4FMkvG9NtBxVezsWO7MIdd/O9CYwuteseJTcjrPJ6.thNMHm'),
    ('33333333-0000-0000-0000-000000000002', 'felix.junghans@breuninger.de', 'Felix', 'Junghans', 'MALE', '$2y$10$0g7SK4FMkvG9NtBxVezsWO7MIdd/O9CYwuteseJTcjrPJ6.thNMHm'),
    ('33333333-0000-0000-0000-000000000003', 'helmer.barcos@breuninger.de', 'Helmer', 'Barcos', 'MALE', '$2y$10$0g7SK4FMkvG9NtBxVezsWO7MIdd/O9CYwuteseJTcjrPJ6.thNMHm');

INSERT INTO purchases (id, user_email, product_name, price_cents, purchased_at) VALUES
    ('44444444-0000-0000-0000-000000000001', 'admin.inspoteam@breuninger.com', 'Silk Twill Scarf', 21900, NOW() - INTERVAL '30 days'),
    ('44444444-0000-0000-0000-000000000002', 'admin.inspoteam@breuninger.com', 'Oversized Blazer', 32900, NOW() - INTERVAL '18 days'),
    ('44444444-0000-0000-0000-000000000003', 'admin.inspoteam@breuninger.com', 'Suede Penny Loafers', 27900, NOW() - INTERVAL '4 days'),
    ('44444444-0000-0000-0000-000000000011', 'felix.junghans@breuninger.de', 'Double-Breasted Wool Coat', 44900, NOW() - INTERVAL '45 days'),
    ('44444444-0000-0000-0000-000000000012', 'felix.junghans@breuninger.de', 'Slim-Fit Selvedge Jeans', 29900, NOW() - INTERVAL '21 days'),
    ('44444444-0000-0000-0000-000000000013', 'felix.junghans@breuninger.de', 'Leather Chelsea Boots', 34900, NOW() - INTERVAL '7 days'),
    ('44444444-0000-0000-0000-000000000014', 'felix.junghans@breuninger.de', 'Cashmere Crewneck Sweater', 19900, NOW() - INTERVAL '2 days'),
    ('44444444-0000-0000-0000-000000000021', 'helmer.barcos@breuninger.de', 'Pleated Midi Skirt', 25900, NOW() - INTERVAL '60 days'),
    ('44444444-0000-0000-0000-000000000022', 'helmer.barcos@breuninger.de', 'Oversized Blazer', 32900, NOW() - INTERVAL '14 days'),
    ('44444444-0000-0000-0000-000000000023', 'helmer.barcos@breuninger.de', 'Leather Chelsea Boots', 34900, NOW() - INTERVAL '3 days');
