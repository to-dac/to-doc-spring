# AI 서버 API 명세

BE → AI 서버 간 호출 규격. AI 서버 base URL: `http://localhost:8000` (환경변수 `AI_BASE_URL`).

---

## 1. 문서 자동작성

### `POST /api/v1/permit/document`

토지 정보와 인허가 템플릿을 전달하면, AI가 각 질문에 답변을 채워 반환한다.  
`thread_id`가 있으면 해당 세션의 대화 이력을 보조 근거로 활용한다.

---

### Request

```json
{
  "thread_id": "123",
  "land_info": {
    "pnu": "1168010100100010000",
    "address": "서울특별시 강남구 삼성동 1",
    "ldCodeNm": "서울특별시 강남구 삼성동",
    "lndcgrCodeNm": "대",
    "lndpclAr": "330.0",
    "oficlLndpcl": "330.0",
    "prposArea1Nm": "제2종일반주거지역",
    "prposArea2Nm": null,
    "ladUseSittnNm": "도시지역",
    "tpgrphHgCodeNm": "저지",
    "tpgrphFrmCodeNm": "평탄",
    "roadSideCodeNm": "세로(가)",
    "pblntfPclnd": "2500000000",
    "lastUpdtDt": "20240101",
    "building": {
      "hasBuilding": true,
      "bldNm": "삼성빌딩",
      "mainPurpsCdNm": "제2종근린생활시설",
      "etcPurps": null,
      "platArea": 330.0,
      "archArea": 198.0,
      "totArea": 990.0,
      "bcRat": 60.0,
      "vlRat": 300.0,
      "heit": 15.0,
      "grndFlrCnt": 5,
      "ugrndFlrCnt": 1,
      "useAprDay": "20100315",
      "strctCdNm": "철근콘크리트구조"
    },
    "landUses": [
      { "code": "UQA180", "name": "제2종일반주거지역", "conflictType": null }
    ],
    "applicablePermits": [
      {
        "id": 1,
        "templateCode": "building_major_repair_use_change_permit",
        "name": "건축물 대수선·용도변경 허가",
        "description": "기존 건축물의 대수선 또는 용도변경 시 필요한 허가"
      }
    ]
  },
  "template": {
    "id": 1,
    "templateCode": "building_major_repair_use_change_permit",
    "name": "건축물 대수선·용도변경 허가",
    "description": "기존 건축물의 대수선 또는 용도변경 시 필요한 허가",
    "version": "1.0",
    "metadata": null,
    "sections": [
      {
        "id": 10,
        "sectionCode": "applicant_info",
        "name": "신청인 정보",
        "orderNo": 1,
        "questions": [
          {
            "id": 101,
            "layoutKey": "land_address",
            "questionType": "TEXT",
            "displayType": "text",
            "name": "대지 위치",
            "description": null,
            "options": null,
            "validation": "{\"required\": true}",
            "subFields": null,
            "metadata": null,
            "orderNo": 1
          }
        ]
      }
    ]
  }
}
```

#### Request 필드

| 필드 | 필수 | 설명 |
|------|------|------|
| `thread_id` | 선택 | 채팅 세션 ID. 대화 이력 보조 근거로 활용 |
| `land_info` | 필수 | 토지 정보 (`GET /api/land` 응답과 동일 구조) |
| `template` | 필수 | 인허가 서식 전체 (sections → questions 포함) |
| `template.sections[].questions[].id` | 필수 | **답변 저장 시 사용되는 질문 ID** |
| `template.sections[].questions[].questionType` | | `TEXT`, `TEXTAREA`, `RADIO`, `CHECKBOX` 등 |
| `template.sections[].questions[].options` | | RADIO/CHECKBOX 선택지 (JSON 배열 문자열) |

---

### Response

```json
{
  "thread_id": "123",
  "templateCode": "building_major_repair_use_change_permit",
  "sections": [
    {
      "id": 10,
      "sectionCode": "applicant_info",
      "name": "신청인 정보",
      "orderNo": 1,
      "questions": [
        {
          "id": 101,
          "layoutKey": "land_address",
          "questionType": "TEXT",
          "displayType": "text",
          "name": "대지 위치",
          "description": null,
          "options": null,
          "validation": null,
          "subFields": null,
          "metadata": null,
          "orderNo": 1,
          "answer": "서울특별시 강남구 삼성동 1",
          "source": "land_info"
        }
      ]
    }
  ],
  "filled_count": 1,
  "total_count": 1
}
```

#### Response 필드

| 필드 | 설명 |
|------|------|
| `thread_id` | 요청에 사용된 세션 ID |
| `templateCode` | 양식 코드 |
| `sections[].questions[].id` | 질문 ID. BE가 `form_answers.question_id`로 저장 |
| `sections[].questions[].answer` | AI가 채운 답변. null이면 미채움 |
| `sections[].questions[].source` | 답변 근거: `land_info` / `conversation` / `unknown` |
| `filled_count` | 답변이 채워진 질문 수 |
| `total_count` | 전체 질문 수 |

#### BE 저장 처리

응답을 받으면 BE에서 다음을 처리한다.
1. `sections[].questions[]` 순회 → `answer != null`인 항목만 추출
2. `form_submissions` 레코드 생성 (status=DRAFT, session 연결)
3. `form_answers` 레코드 저장 (`question_id`, `answer_value`)
4. `chat_sessions.submission_id` 업데이트

---

## 2. 채팅

### `POST /api/v1/permit/chat`

### Request

```json
{
  "message": "이 땅에 펜션을 지을 수 있나요?",
  "thread_id": "123"
}
```

### Response

```json
{
  "thread_id": "123",
  "reply": "해당 토지는 제2종일반주거지역으로...",
  "permit_type": "building"
}
```

#### `permit_type` → 템플릿 매핑

| `permit_type` | templateCode |
|---|---|
| `building` | `building_major_repair_use_change_permit` |
| `dev_act` | `development_activity_permit` |
| `farmland` | `farmland_conversion_permit` |
| `road` | `road_permit` |
| `river` | `river_permit` |
| `mountain` | `mountain_permit` |
| `null` | 템플릿 변경 없음 |
