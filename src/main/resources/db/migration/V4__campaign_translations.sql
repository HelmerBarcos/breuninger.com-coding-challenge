-- Campaign texts are DATA, so they are localized as data (ADR-011): nullable
-- English columns next to the German base columns. A missing translation
-- falls back to German - the shop's default locale.
ALTER TABLE sale_campaigns
    ADD COLUMN headline_en  VARCHAR(255),
    ADD COLUMN cta_label_en VARCHAR(100);

UPDATE sale_campaigns
SET headline_en  = 'Mid-Season Sale - up to 30% off',
    cta_label_en = 'Shop now'
WHERE id = '22222222-0000-0000-0000-000000000001';
