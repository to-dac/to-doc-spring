-- Seed data for:
-- [별지 제5호서식] 개발행위 허가신청서
-- (국토의 계획 및 이용에 관한 법률 시행규칙)
--
-- Run after schema DDL.

INSERT INTO form_templates (
    template_code,
    name,
    description,
    source_file,
    version,
    metadata
) VALUES (
    'development_activity_permit',
    '개발행위 허가신청서',
    '국토의 계획 및 이용에 관한 법률 시행규칙 별지 제5호서식 기반 폼 데이터',
    '[별지 제5호서식] 개발행위 허가신청서(국토의 계획 및 이용에 관한 법률 시행규칙).pdf',
    '2014-08-07',
    JSON_OBJECT('law', '국토의 계획 및 이용에 관한 법률 시행규칙', 'formNo', '별지 제5호서식', 'pages', 2, 'processingDays', 15)
);

SET @template_id = LAST_INSERT_ID();

INSERT INTO form_sections (
    template_id,
    section_code,
    name,
    description,
    order_no,
    metadata
) VALUES
(@template_id, 'basic',                  '기본 정보',                NULL, 1, JSON_OBJECT()),
(@template_id, 'applicant',              '신청인',                   NULL, 2, JSON_OBJECT()),
(@template_id, 'permit_details',         '허가신청 사항',            NULL, 3, JSON_OBJECT()),
(@template_id, 'structure_installation', '공작물 설치',              '신청유형이 공작물설치인 경우 작성', 4, JSON_OBJECT('applicationType', '공작물설치')),
(@template_id, 'land_form_change',       '토지형질변경',             '신청유형이 토지형질변경인 경우 작성', 5, JSON_OBJECT('applicationType', '토지형질변경')),
(@template_id, 'earth_stone_extraction', '토석채취',                 '신청유형이 토석채취인 경우 작성', 6, JSON_OBJECT('applicationType', '토석채취')),
(@template_id, 'land_division',          '토지분할',                 '신청유형이 토지분할인 경우 작성', 7, JSON_OBJECT('applicationType', '토지분할')),
(@template_id, 'goods_storage',          '물건적치',                 '신청유형이 물건적치인 경우 작성', 8, JSON_OBJECT('applicationType', '물건적치')),
(@template_id, 'project_info',           '개발행위목적 및 사업기간', NULL, 9, JSON_OBJECT());

INSERT INTO form_questions (
    section_id,
    section_name,
    question_type,
    name,
    description,
    options,
    display_type,
    validation,
    conditional,
    batch_group,
    sub_fields,
    layout_key,
    order_no,
    metadata
)
SELECT
    fs.id,
    fs.name,
    q.question_type,
    q.name,
    q.description,
    q.options,
    q.display_type,
    q.validation,
    q.conditional,
    q.batch_group,
    q.sub_fields,
    q.layout_key,
    q.order_no,
    q.metadata
FROM form_sections fs
JOIN (
    -- ── 기본 정보 ──────────────────────────────────────────────────────────────
    SELECT 'basic' section_code, 'TEXT'   question_type, '접수번호' name, NULL description,
           NULL options, 'text' display_type, NULL validation, NULL conditional,
           NULL batch_group, NULL sub_fields, 'receiptNumber' layout_key, 1 order_no, JSON_OBJECT() metadata
    UNION ALL SELECT 'basic', 'DATE', '접수일', NULL, NULL, 'date', NULL, NULL, NULL, NULL, 'receiptDate', 2, JSON_OBJECT()
    UNION ALL SELECT 'basic', 'CHOICE', '신청유형', NULL,
        JSON_ARRAY('공작물설치', '토지형질변경', '토석채취', '토지분할', '물건적치'),
        'checkboxGroup', NULL, NULL, NULL, NULL, 'applicationType', 3, JSON_OBJECT()

    -- ── 신청인 ─────────────────────────────────────────────────────────────────
    UNION ALL SELECT 'applicant', 'TEXT', '성명(법인인 경우는 그 명칭 및 대표자 성명)', NULL,
        NULL, 'text', NULL, NULL, NULL, NULL, 'applicantName', 1, JSON_OBJECT()
    UNION ALL SELECT 'applicant', 'TEXT', '생년월일(법인인 경우는 법인등록번호)', NULL,
        NULL, 'text', NULL, NULL, NULL, NULL, 'applicantBirthRegistration', 2, JSON_OBJECT()
    UNION ALL SELECT 'applicant', 'TEXT', '주소', NULL,
        NULL, 'textarea', NULL, NULL, NULL, NULL, 'applicantAddress', 3, JSON_OBJECT()
    UNION ALL SELECT 'applicant', 'TEXT', '우편번호', NULL,
        NULL, 'text', NULL, NULL, NULL, NULL, 'applicantPostalCode', 4, JSON_OBJECT()
    UNION ALL SELECT 'applicant', 'TEXT', '전화번호', NULL,
        NULL, 'text', NULL, NULL, NULL, NULL, 'applicantPhone', 5, JSON_OBJECT()
    UNION ALL SELECT 'applicant', 'TEXT', '신청인 서명 또는 인', NULL,
        NULL, 'signature', NULL, NULL, NULL, NULL, 'applicantSignature', 6, JSON_OBJECT()

    -- ── 허가신청 사항 ──────────────────────────────────────────────────────────
    UNION ALL SELECT 'permit_details', 'TEXT', '위치(지번)', NULL,
        NULL, 'text', NULL, NULL, NULL, NULL, 'locationLotNumber', 1, JSON_OBJECT()
    UNION ALL SELECT 'permit_details', 'TEXT', '지목', NULL,
        NULL, 'text', NULL, NULL, NULL, NULL, 'landCategory', 2, JSON_OBJECT()
    UNION ALL SELECT 'permit_details', 'TEXT', '용도지역', NULL,
        NULL, 'text', NULL, NULL, NULL, NULL, 'zoningDistrict', 3, JSON_OBJECT()
    UNION ALL SELECT 'permit_details', 'TEXT', '용도지구', NULL,
        NULL, 'text', NULL, NULL, NULL, NULL, 'useDistrict', 4, JSON_OBJECT()

    -- ── 공작물 설치 ────────────────────────────────────────────────────────────
    UNION ALL SELECT 'structure_installation', 'NUMBER', '신청면적', NULL,
        NULL, 'number', JSON_OBJECT('min', 0),
        JSON_OBJECT('field', 'applicationType', 'includes', '공작물설치'),
        NULL, NULL, 'structureApplicationArea', 1, JSON_OBJECT('unit', '㎡')
    UNION ALL SELECT 'structure_installation', 'NUMBER', '중량', NULL,
        NULL, 'number', JSON_OBJECT('min', 0),
        JSON_OBJECT('field', 'applicationType', 'includes', '공작물설치'),
        NULL, NULL, 'structureWeight', 2, JSON_OBJECT('unit', '톤')
    UNION ALL SELECT 'structure_installation', 'TEXT', '공작물구조', NULL,
        NULL, 'text', NULL,
        JSON_OBJECT('field', 'applicationType', 'includes', '공작물설치'),
        NULL, NULL, 'structureType', 3, JSON_OBJECT()
    UNION ALL SELECT 'structure_installation', 'NUMBER', '부피', NULL,
        NULL, 'number', JSON_OBJECT('min', 0),
        JSON_OBJECT('field', 'applicationType', 'includes', '공작물설치'),
        NULL, NULL, 'structureVolume', 4, JSON_OBJECT('unit', '㎥')

    -- ── 토지형질변경 ───────────────────────────────────────────────────────────
    UNION ALL SELECT 'land_form_change', 'NUMBER', '경사도', NULL,
        NULL, 'number', JSON_OBJECT('min', 0),
        JSON_OBJECT('field', 'applicationType', 'includes', '토지형질변경'),
        NULL, NULL, 'landSlope', 1, JSON_OBJECT('unit', '%')
    UNION ALL SELECT 'land_form_change', 'TEXT', '토질', NULL,
        NULL, 'text', NULL,
        JSON_OBJECT('field', 'applicationType', 'includes', '토지형질변경'),
        NULL, NULL, 'soilType', 2, JSON_OBJECT()
    UNION ALL SELECT 'land_form_change', 'NUMBER', '토석매장량', NULL,
        NULL, 'number', JSON_OBJECT('min', 0),
        JSON_OBJECT('field', 'applicationType', 'includes', '토지형질변경'),
        NULL, NULL, 'earthStoneReserve', 3, JSON_OBJECT('unit', '㎥')
    UNION ALL SELECT 'land_form_change', 'TEXT', '주요수종', NULL,
        NULL, 'text', NULL,
        JSON_OBJECT('field', 'applicationType', 'includes', '토지형질변경'),
        NULL, NULL, 'mainTreeSpecies', 4, JSON_OBJECT()
    UNION ALL SELECT 'land_form_change', 'NUMBER', '입목지', NULL,
        NULL, 'number', JSON_OBJECT('min', 0),
        JSON_OBJECT('field', 'applicationType', 'includes', '토지형질변경'),
        NULL, NULL, 'forestedLandArea', 5, JSON_OBJECT('unit', '㎡')
    UNION ALL SELECT 'land_form_change', 'NUMBER', '무입목지', NULL,
        NULL, 'number', JSON_OBJECT('min', 0),
        JSON_OBJECT('field', 'applicationType', 'includes', '토지형질변경'),
        NULL, NULL, 'nonForestedLandArea', 6, JSON_OBJECT('unit', '㎡')
    UNION ALL SELECT 'land_form_change', 'NUMBER', '신청면적', NULL,
        NULL, 'number', JSON_OBJECT('min', 0),
        JSON_OBJECT('field', 'applicationType', 'includes', '토지형질변경'),
        NULL, NULL, 'landFormApplicationArea', 7, JSON_OBJECT('unit', '㎡')
    UNION ALL SELECT 'land_form_change', 'TEXT', '입목벌채 수종', NULL,
        NULL, 'text', NULL,
        JSON_OBJECT('field', 'applicationType', 'includes', '토지형질변경'),
        NULL, NULL, 'fellingTreeSpecies', 8, JSON_OBJECT()
    UNION ALL SELECT 'land_form_change', 'NUMBER', '입목벌채 나무 수', NULL,
        NULL, 'number', JSON_OBJECT('min', 0),
        JSON_OBJECT('field', 'applicationType', 'includes', '토지형질변경'),
        NULL, NULL, 'fellingTreeCount', 9, JSON_OBJECT('unit', '그루')

    -- ── 토석채취 ───────────────────────────────────────────────────────────────
    UNION ALL SELECT 'earth_stone_extraction', 'NUMBER', '신청면적', NULL,
        NULL, 'number', JSON_OBJECT('min', 0),
        JSON_OBJECT('field', 'applicationType', 'includes', '토석채취'),
        NULL, NULL, 'extractionApplicationArea', 1, JSON_OBJECT('unit', '㎡')
    UNION ALL SELECT 'earth_stone_extraction', 'NUMBER', '부피', NULL,
        NULL, 'number', JSON_OBJECT('min', 0),
        JSON_OBJECT('field', 'applicationType', 'includes', '토석채취'),
        NULL, NULL, 'extractionVolume', 2, JSON_OBJECT('unit', '㎥')

    -- ── 토지분할 ───────────────────────────────────────────────────────────────
    UNION ALL SELECT 'land_division', 'NUMBER', '종전면적', NULL,
        NULL, 'number', JSON_OBJECT('min', 0),
        JSON_OBJECT('field', 'applicationType', 'includes', '토지분할'),
        NULL, NULL, 'previousArea', 1, JSON_OBJECT('unit', '㎡')
    UNION ALL SELECT 'land_division', 'NUMBER', '분할면적', NULL,
        NULL, 'number', JSON_OBJECT('min', 0),
        JSON_OBJECT('field', 'applicationType', 'includes', '토지분할'),
        NULL, NULL, 'divisionArea', 2, JSON_OBJECT('unit', '㎡')

    -- ── 물건적치 ───────────────────────────────────────────────────────────────
    UNION ALL SELECT 'goods_storage', 'NUMBER', '중량', NULL,
        NULL, 'number', JSON_OBJECT('min', 0),
        JSON_OBJECT('field', 'applicationType', 'includes', '물건적치'),
        NULL, NULL, 'storageWeight', 1, JSON_OBJECT('unit', '톤')
    UNION ALL SELECT 'goods_storage', 'NUMBER', '부피', NULL,
        NULL, 'number', JSON_OBJECT('min', 0),
        JSON_OBJECT('field', 'applicationType', 'includes', '물건적치'),
        NULL, NULL, 'storageVolume', 2, JSON_OBJECT('unit', '㎥')
    UNION ALL SELECT 'goods_storage', 'TEXT', '품명', NULL,
        NULL, 'text', NULL,
        JSON_OBJECT('field', 'applicationType', 'includes', '물건적치'),
        NULL, NULL, 'goodsName', 3, JSON_OBJECT()
    UNION ALL SELECT 'goods_storage', 'NUMBER', '평균적치량', NULL,
        NULL, 'number', JSON_OBJECT('min', 0),
        JSON_OBJECT('field', 'applicationType', 'includes', '물건적치'),
        NULL, NULL, 'averageStorageAmount', 4, JSON_OBJECT()
    UNION ALL SELECT 'goods_storage', 'DATE', '적치기간 시작일', NULL,
        NULL, 'date', NULL,
        JSON_OBJECT('field', 'applicationType', 'includes', '물건적치'),
        NULL, NULL, 'storageStartDate', 5, JSON_OBJECT()
    UNION ALL SELECT 'goods_storage', 'DATE', '적치기간 종료일', NULL,
        NULL, 'date', NULL,
        JSON_OBJECT('field', 'applicationType', 'includes', '물건적치'),
        NULL, NULL, 'storageEndDate', 6, JSON_OBJECT()
    UNION ALL SELECT 'goods_storage', 'NUMBER', '적치기간(개월)', NULL,
        NULL, 'number', JSON_OBJECT('min', 0),
        JSON_OBJECT('field', 'applicationType', 'includes', '물건적치'),
        NULL, NULL, 'storageDurationMonths', 7, JSON_OBJECT('unit', '개월')

    -- ── 개발행위목적 및 사업기간 ───────────────────────────────────────────────
    UNION ALL SELECT 'project_info', 'TEXT', '개발행위목적', NULL,
        NULL, 'textarea', NULL, NULL, NULL, NULL, 'developmentPurpose', 1, JSON_OBJECT()
    UNION ALL SELECT 'project_info', 'DATE', '착공일', NULL,
        NULL, 'date', NULL, NULL, NULL, NULL, 'constructionStartDate', 2, JSON_OBJECT()
    UNION ALL SELECT 'project_info', 'DATE', '준공일', NULL,
        NULL, 'date', NULL, NULL, NULL, NULL, 'constructionEndDate', 3, JSON_OBJECT()
    UNION ALL SELECT 'project_info', 'DATE', '신청일', NULL,
        NULL, 'date', NULL, NULL, NULL, NULL, 'applicationDate', 4, JSON_OBJECT()
) q ON q.section_code = fs.section_code
WHERE fs.template_id = @template_id;
