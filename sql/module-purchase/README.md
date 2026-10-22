# 구매실적 모니터링 모듈

## 개요
기존 플랫폼에 Gradle 서브모듈로 포함되는 구매실적 모니터링 업무 모듈입니다. 월별 Excel을 업로드하면 첫 번째 시트의 요약을 MariaDB에 적재하고, 두 번째 시트는 월별 상세 창에서 볼 수 있도록 저장합니다. 월별 계획·실적 요약표, 꺾은선 그래프, 연간 증감 사유 표를 제공합니다. 독립 실행 애플리케이션이 아닙니다.

- 모듈 디렉터리: `module-purchase/`
- 패키지 루트: `com.company.module.purchase`
- 화면 URL: `/purchase/index.html`
- API Prefix: `/purchase-api`
- 금액 단위: 백만원, VAT 별도
- 인증/인가: 기존 플랫폼 core 사용 (`PURCHASE_READ`, `PURCHASE_WRITE` 권한 등록 필요)

## 주요 기능
- Excel `.xlsx` / `.xls` 업로드. 첫 번째 시트는 월별 요약·증감 사유, 두 번째 시트는 업체별 상세 창 자료로 읽으며 세 번째 이후 시트는 무시
- 파일명에 `2026년 9월`처럼 연·월이 있으면 해당 월을 우선 적용하고, 파일명에 없을 때만 첫 시트 제목을 사용. 첫 시트의 구분별 계획·실적·증감액·증감사유를 파싱
- 첫 시트의 구분 행 합계와 `합계` 행이 다르면 저장을 거부
- 월별 계획·실적 요약표와 그 아래 전체 너비 꺾은선 그래프. 업로드된 월 행 또는 그래프 점을 누르면 새 창에서 해당 월의 두 번째 시트 상세를 확인
- 선택한 연도의 1~12월 계획 대비·전월 대비 증감액 및 증감 사유 표
- 미업로드 월도 연간 표에 표시해 연도별 입력 범위를 한눈에 확인
- 사업부별 실적 합계는 표시하지 않음 (첫 시트에 사업부별 자료가 없음)
- 동일 연월 재업로드 시 SHA-256 동일 파일은 중복 적재하지 않음
- 다른 파일이면 먼저 교체 확인 응답을 반환하며, 확인 후 현재 자료를 교체하고 구버전 및 업로드 이력은 보존
- 원본 Excel 파일은 저장하지 않고 파일명, SHA-256, 파싱한 자료만 DB에 저장

## 생성 테이블
| 테이블 | 설명 |
|---|---|
| `purchase_upload_batch` | 월별 업로드 버전, 요약 금액, 파일 해시 및 업로드 이력 |
| `purchase_month_snapshot` | 연도·월별 현재 반영 버전을 가리키는 자료 포인터 |
| `purchase_category` | 첫 번째 시트의 구분별 구매실적 요약 행 |
| `purchase_detail_sheet` | 두 번째 시트의 업체별 표, 합계, 주요 이슈·개선 방향 JSON 데이터 |

업로드 배치와 월별 상세의 기존 이력은 보존합니다. 논리 삭제는 `DELETED_YN` 공통 컬럼 기준이며 물리 삭제는 사용하지 않습니다.

## API 목록
모든 응답은 플랫폼의 `ApiResponse<T>` 형식을 사용합니다.

| Method | URL | 권한 | 설명 |
|---|---|---|---|
| `POST` | `/purchase-api/uploads?confirmReplace=false` | `PURCHASE_WRITE` | `file` multipart Excel 업로드. 기존 연월 자료가 있으면 교체 확인 요청 |
| `POST` | `/purchase-api/uploads?confirmReplace=true` | `PURCHASE_WRITE` | 확인된 새 파일을 해당 연월의 현재 자료로 반영 |
| `GET` | `/purchase-api/dashboard?year=2026` | `PURCHASE_READ` | 월별 계획/실적 및 누계, `monthlyReasons` 연간 12개월 증감 사유 목록 |
| `GET` | `/purchase-api/detail-sheet?year=2026&month=9` | `PURCHASE_READ` | 해당 월 현재 업로드 버전의 두 번째 시트 상세 행·합계·이슈 조회 |

`monthlyReasons`는 업로드된 달에는 첫 시트의 각 구분 행을 반환하고, 미업로드 달에는 해당 월의 `uploaded=false` 행을 반환합니다. `/detail-sheet`는 현재 월 배치의 두 번째 시트 자료를 반환합니다. 기존에 상세 시트를 저장하지 않은 월은 해당 원본 Excel을 다시 업로드해야 상세 창을 채울 수 있습니다.

### 업로드 응답의 교체 확인
기존 자료가 있고 파일 해시가 다르면 첫 요청은 `requiresReplacementConfirmation=true`를 돌려줍니다. 화면에서 사용자 확인 후 동일 파일을 `confirmReplace=true`로 다시 요청합니다. 확인 취소 시 DB는 변경되지 않습니다. 업로드 전체는 한 트랜잭션에서 배치·상세·현재 월 포인터가 함께 반영됩니다.

## Excel 파싱 기준
- 첫 번째 시트(`workbook.getSheetAt(0)`)에서 요약값을 파싱하고, 두 번째 시트(`workbook.getSheetAt(1)`)를 새 창 상세 화면용으로 읽어 DB에 저장합니다. 세 번째 이후 시트는 읽거나 검증하지 않습니다.
- 파일명에서 `2026년 9월` 또는 `26년 9월` 패턴을 먼저 읽어 업로드 대상 연·월을 정합니다. 파일명에 기간이 없을 때만 첫 시트 제목을 보조 기준으로 사용합니다. 따라서 `2026년 9월 구매실적자료.xlsx`는 2026년 9월로 적재됩니다.
- 첫 시트의 `구분` 행에서 전월 실적, 당월 외자/내자 계획 및 실적, 계획·전월 대비 증감액, 두 증감 사유를 읽습니다.
- 두 번째 시트에서 월 제목, 그룹형 머리글, 공급품목별 14열 상세 행, 내자/외자/총합계, 주요 Issue 사항, 향후 개선 방향을 보존해 상세 창으로 반환합니다.
- 첫 시트의 구분별 금액 합계와 `합계` 행의 당월 계획·실적이 0.01백만원 넘게 다르면 업로드를 거부합니다.
- 표시 금액 단위는 백만원이며 VAT 별도입니다.
- 사업부/협력업체별 실적은 첫 시트에 없으므로 집계하거나 표시하지 않습니다. 첫 시트의 구분 행은 월별 증감 사유 표에 그대로 표시합니다.

## 미리보기 업로드 주의
샌드박스 미리보기 서버는 예시 데이터를 읽기 전용으로 보여주며 업로드는 501로 거절합니다. 실제 파일을 검증·파싱해 DB에 저장하려면 기존 플랫폼에 이 모듈을 통합하고 `/purchase-api/uploads` Spring API 및 MariaDB 연결이 구성된 화면을 사용해야 합니다. 미리보기에서 업로드를 가상 성공 처리하지 않습니다.

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
| SQL 공통 컬럼 및 테이블/컬럼 주석 | 통과 | 네 테이블에 적용 |
| SQL 안전 규칙 | 통과 | 비파괴 DDL, 자동 실행 없음 |
| 자체 standalone Gradle 빌드 | 보류 | 현재 작업 공간에 Gradle 실행 파일과 플랫폼 `:core`가 없어 실제 호스트 통합 빌드는 미실행 |
| 운영 DB·개발서버·운영서버 반영 | 미실행 | 사용자 요청 없이 브랜치/서버에 반영하지 않음 |
