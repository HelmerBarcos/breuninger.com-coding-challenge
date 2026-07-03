-- Reference catalog data: 8 products and one active sale campaign.
-- Image URLs point at a placeholder service on purpose (no real assets in this repo).

INSERT INTO products (id, name, brand, price_cents, image_url) VALUES
    ('11111111-0000-0000-0000-000000000001', 'Cashmere Crewneck Sweater', 'Breuninger Collection', 19900, 'https://placehold.co/600x800?text=Cashmere+Sweater'),
    ('11111111-0000-0000-0000-000000000002', 'Double-Breasted Wool Coat', 'BOSS', 44900, 'https://placehold.co/600x800?text=Wool+Coat'),
    ('11111111-0000-0000-0000-000000000003', 'Leather Chelsea Boots', 'Santoni', 34900, 'https://placehold.co/600x800?text=Chelsea+Boots'),
    ('11111111-0000-0000-0000-000000000004', 'Pleated Midi Skirt', 'Max Mara', 25900, 'https://placehold.co/600x800?text=Midi+Skirt'),
    ('11111111-0000-0000-0000-000000000005', 'Silk Twill Scarf', 'Gucci', 21900, 'https://placehold.co/600x800?text=Silk+Scarf'),
    ('11111111-0000-0000-0000-000000000006', 'Slim-Fit Selvedge Jeans', 'Jacob Cohen', 29900, 'https://placehold.co/600x800?text=Selvedge+Jeans'),
    ('11111111-0000-0000-0000-000000000007', 'Suede Penny Loafers', 'Tod''s', 27900, 'https://placehold.co/600x800?text=Penny+Loafers'),
    ('11111111-0000-0000-0000-000000000008', 'Oversized Blazer', 'Anine Bing', 32900, 'https://placehold.co/600x800?text=Oversized+Blazer');

INSERT INTO sale_campaigns (id, headline, cta_label, image_url, active) VALUES
    ('22222222-0000-0000-0000-000000000001', 'Mid-Season Sale – bis zu 30%', 'Jetzt shoppen', 'https://placehold.co/1200x400?text=Mid-Season+Sale', TRUE);
