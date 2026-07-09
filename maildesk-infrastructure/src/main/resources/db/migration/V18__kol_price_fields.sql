-- V18: split Feishu brand quote vs final cooperation price on kols.

ALTER TABLE kols
    ADD COLUMN brand_quote TEXT,
    ADD COLUMN final_cooperation_price NUMERIC;

COMMENT ON COLUMN kols.brand_quote IS
    'Brand-side quote imported from Feishu column 品牌报价 (fallback: KOL报价($), 报价).';
COMMENT ON COLUMN kols.final_cooperation_price IS
    'Final cooperation price imported from Feishu column 最终合作价格.';
