CREATE TABLE service_offering (
    id             UUID PRIMARY KEY,
    code           VARCHAR(40)  NOT NULL UNIQUE,
    category       VARCHAR(20)  NOT NULL,
    name_ar        VARCHAR(120) NOT NULL,
    name_en        VARCHAR(120) NOT NULL,
    description_ar VARCHAR(600) NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order     INT          NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT service_offering_category_chk CHECK (category IN ('ACADEMIC', 'BUSINESS'))
);

-- Only services the office has agreed to offer. Writing theses or papers on a student's behalf is NOT offered.
INSERT INTO service_offering (id, code, category, name_ar, name_en, description_ar, sort_order) VALUES
 (gen_random_uuid(), 'STAT_ANALYSIS',   'ACADEMIC', 'التحليل الإحصائي', 'Statistical analysis',
  'تحليل بيانات الاستبيانات والدراسات (SPSS، AMOS، SmartPLS) مع جداول منسقة وتفسير للنتائج.', 10),
 (gen_random_uuid(), 'PROOFREADING',    'ACADEMIC', 'التدقيق اللغوي والتنسيق', 'Proofreading & formatting',
  'تدقيق لغوي ونحوي وتنسيق كامل حسب دليل الجامعة: الفهارس والجداول والمراجع.', 20),
 (gen_random_uuid(), 'TRANSLATION',     'ACADEMIC', 'الترجمة الأكاديمية', 'Academic translation',
  'ترجمة الملخصات والمقالات والمراجع بين العربية والإنجليزية.', 30),
 (gen_random_uuid(), 'REFERENCES',      'ACADEMIC', 'البحث عن المراجع وتنظيمها', 'Literature search',
  'البحث عن الدراسات السابقة والمراجع الحديثة وتنظيمها بأسلوب التوثيق المطلوب.', 40),
 (gen_random_uuid(), 'COACHING',        'ACADEMIC', 'الإرشاد البحثي', 'Research coaching',
  'جلسات توجيه في خطة البحث والمنهجية وبناء الفصول، والكتابة تبقى للطالب.', 50),
 (gen_random_uuid(), 'FEASIBILITY',     'BUSINESS', 'دراسة الجدوى', 'Feasibility study',
  'دراسة سوقية وفنية ومالية لمشروعك، مناسبة لجهات التمويل والترخيص.', 60),
 (gen_random_uuid(), 'MARKET_RESEARCH', 'BUSINESS', 'دراسة السوق', 'Market research',
  'تحليل السوق والمنافسين والعملاء المستهدفين لدعم قرارك الاستثماري.', 70),
 (gen_random_uuid(), 'SURVEY',          'BUSINESS', 'تصميم الاستبيانات وتحليلها', 'Survey design & analysis',
  'تصميم استبيان احترافي وجمع وتحليل النتائج في تقرير واضح.', 80);
