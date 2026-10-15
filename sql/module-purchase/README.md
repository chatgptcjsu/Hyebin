# 구매실적 모니터링 모듈

## 개요
기존 플랫폼에 Gradle 서브모듈로 포함되는 구매실적 모니터링 업무 모듈입니다. 1~12월 구매실적 Excel 파일을 업로드하면 검증 후 MariaDB에 적재하고, 월별/누계 계획·실적 및 사업부·협력업체별 구매 구성을 대시보드에 표시합니다. 독립 실행 애플리케이션이 아닙니다.

- 모듈 디렉터리: `module-purchase/`
- 패키지 루트: `com.company.module.purchase`
- 화면 URL: `/purchase/index.html`
- API Prefix: `/purchase-api`
- 금액 단위: 백만원, VAT 별도
- 인증/인가: 기존 플랫폼 core 사용 (`PURCHASE_READ`, `PURCHASE_WRITE` 권한 등록 필요)

## 주요 기능
- Excel `.xlsx` / `.xls` 업로드 및 `구매실적요약`, `기계설비공사` 시트 검증
- 제목에서 연도·월을 식별하고 당월 계획/당월 실적 요약 및 상세 행을 파싱
- 요약 금액과 상세 합계를 대조하고 차이가 있으면 저장을 거부
- 월별 계획·실적·달성률·증감 및 누계 추이 대시보드
- 사업부별/협력업체별 실적, 월별 검색·페이지 상세내역
- 동일 연월 재업로드 시 SHA-256 동일 파일은 중복 적재하지 않음
- 다른 파일이면 먼저 교체 확인 응답을 반환하며, 확인 후 현재 자료를 교체하고 구버전 및 업로드 이력은 보존
- 원본 Excel 파일은 저장하지 않고 파일명, SHA-256, 파싱한 자료만 DB에 저장

## 생성 테이블
| 테이블 | 설명 |
|---|---|
| `purchase_upload_batch` | 월별 업로드 버전, 요약 금액, 파일 해시 및 업로드 이력 |
| `purchase_month_snapshot` | 연도·월별 현재 반영 버전을 가리키는 자료 포인터 |
| `purchase_line` | 업로드 원본의 업체별 구매실적 상세 행 |

업로드 배치와 월별 상세의 기존 이력은 보존합니다. 논리 삭제는 `DELETED_YN` 공통 컬럼 기준이며 물리 삭제는 사용하지 않습니다.

## API 목록
모든 응답은 플랫폼의 `ApiResponse<T>` 형식을 사용합니다.

| Method | URL | 권한 | 설명 |
|---|---|---|---|
| `POST` | `/purchase-api/uploads?confirmReplace=false` | `PURCHASE_WRITE` | `file` multipart Excel 업로드. 기존 연월 자료가 있으면 교체 확인 요청 |
| `POST` | `/purchase-api/uploads?confirmReplace=true` | `PURCHASE_WRITE` | 확인된 새 파일을 해당 연월의 현재 자료로 반영 |
| `GET` | `/purchase-api/dashboard?year=2026` | `PURCHASE_READ` | 월별 계획/실적, 연간 누계, 사업부 및 업체 집계 |
| `GET` | `/purchase-api/uploads?year=2026` | `PURCHASE_READ` | 해당 연도 업로드 및 교체 이력 |
| `GET` | `/purchase-api/details?year=2026&month=1&page=0&size=50` | `PURCHASE_READ` | 월 상세 내역. `businessUnit`, `supplierName` 필터 선택 가능 |

### 업로드 응답의 교체 확인
기존 자료가 있고 파일 해시가 다르면 첫 요청은 `requiresReplacementConfirmation=true`를 돌려줍니다. 화면에서 사용자 확인 후 동일 파일을 `confirmReplace=true`로 다시 요청합니다. 확인 취소 시 DB는 변경되지 않습니다. 업로드 전체는 한 트랜잭션에서 배치·상세·현재 월 포인터가 함께 반영됩니다.

## Excel 파싱 기준
- 시트명: `구매실적요약`, `기계설비공사`
- 기간: 요약 시트 제목의 `26년 1월` 또는 `2026년 1월` 패턴
- 요약: `구매실적요약` 시트의 `기계설비공사` 행, 당월 계획 합계와 당월 실적 합계
- 상세: `기계설비공사` 시트의 항목 열이 `기계설비공사`이고 협력업체명이 있는 행
- 상세 열: 사업부, 내/외자, 업체 코드, 단위, 전월 실적, 당월 계획, 당월 실적, 비고, 협력업체명
- 집계에는 당월 실적만 사용합니다. 전월 실적은 참고 데이터로 보관하여 중복 집계하지 않습니다.
- 엑셀 서식, 병합셀, 행 수가 바뀌더라도 제목·시트·항목값을 탐색하며, 요약과 상세 금액 차이가 0.01백만원 초과이면 업로드를 거부합니다.

## SQL 적용
1. 운영자가 DDL을 검토합니다.
2. 승인된 MariaDB에 `01_schema.sql`을 적용합니다.
3. `02_seed_data.sql`에는 업무 seed 데이터가 없습니다.
4. 운영 데이터 반영은 로그인 후 화면에서 월별 Excel을 업로드합니다.

`01_schema.sql`은 `CREATE TABLE IF NOT EXISTS`를 사용하며 `DROP`, `TRUNCATE`, 데이터 전체 삭제를 포함하지 않습니다. 이 모듈에서는 SQL을 자동 실행하지 않습니다.

## 플랫폼 통합 시 운영자 작업
업무 모듈 생성 규칙에 따라 이 변경사항은 기존 플랫폼 설정 파일을 수정하지 않습니다. 플랫폼 운영자가 모듈을 통합할 때 확인할 항목:

1. `settings.gradle`에 `include 'module-purchase'` 추가
2. `app/build.gradle`에 `implementation project(':module-purchase')` 추가
3. `module-purchase`에 필요한 Spring Boot, Data JPA, Validation, Lombok, Apache POI 의존성 버전을 기존 플랫폼의 dependency management와 맞춤
4. `com.company.module` 패키지가 Component Scan, Entity Scan, JPA Repository Scan 범위에 포함되는지 확인
5. 운영자가 `01_schema.sql`을 검토·적용하고 `PURCHASE_READ`, `PURCHASE_WRITE` 권한과 메뉴를 core에 등록
6. 플랫폼의 업로드 최대 용량/요청 제한이 업로드 대상 파일보다 큰지 확인
7. 기존 core의 `ApiResponse`, `CurrentUserProvider`, `ErrorCode.INVALID_INPUT_VALUE` API 시그니처가 모듈 참조와 일치하는지 빌드로 검증

필수 core 의존: `com.company.core.common.response.ApiResponse`, `com.company.core.common.exception.BusinessException`, `ErrorCode.INVALID_INPUT_VALUE`, `com.company.core.security.CurrentUserProvider`. 이 모듈은 인증·인가 구현을 추가하지 않습니다.

## 메뉴 등록 제안
- 메뉴명: 구매실적 모니터링
- 메뉴 위치: 구매관리 > 구매실적 모니터링 (플랫폼 운영자 최종 확인)
- 메뉴 URL: `/purchase/index.html`
- API Prefix: `/purchase-api`
- 조회 권한: `PURCHASE_READ`
- 업로드 권한: `PURCHASE_WRITE`
- 메뉴/권한 데이터베이스 구조를 알 수 없으므로 core 메뉴·권한 INSERT SQL은 생성하지 않았습니다.

## 검수 체크리스트
| 검수 항목 | 결과 | 비고 |
|---|---|---|
| 인증/사용자/권한/메뉴 기능을 새로 만들지 않음 | 통과 | core 공통 Provider·권한 어노테이션만 참조 |
| 독립 main class 및 Spring Boot plugin/bootJar 미생성 | 통과 | Java library/plain jar 서브모듈 |
| `application.yml`, 보안 설정, 배포 설정 미생성 | 통과 | 금지 파일 미생성 |
| API prefix 및 `ApiResponse<T>` 사용 | 통과 | `/purchase-api/**` |
| Entity Setter/Data 어노테이션 미사용 | 통과 | 명시적 Builder/업무 메서드 사용 |
| SQL 공통 컬럼 및 테이블/컬럼 주석 | 통과 | 세 테이블에 적용 |
| SQL 안전 규칙 | 통과 | 비파괴 DDL, 자동 실행 없음 |
| 자체 standalone Gradle 빌드 | 보류 | 현재 작업 공간에 Gradle 실행 파일과 플랫폼 `:core`가 없어 실제 호스트 통합 빌드는 미실행 |
| 운영 DB·개발서버·운영서버 반영 | 미실행 | 사용자 요청 없이 브랜치/서버에 반영하지 않음 |
