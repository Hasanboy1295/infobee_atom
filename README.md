# ATOM AI 플랫폼

이 프로젝트는 풀스택 AI 플랫폼으로, 다음 모듈들을 포함합니다:

- 프론트엔드: Next.js + React + JavaScript
- 백엔드: Java + Spring Boot
- AI 서비스: Python + FastAPI
- 데이터베이스: PostgreSQL
- 선택적 벡터 DB: pgvector / FAISS / Milvus
- 컨테이너화: Docker

## 주요 목표

- 관리자 패널
- 사용자 패널
- 로그인 / 회원가입
- ATOM AI 예측
- CPSR 독성 평가
- 승인 / 반려 워크플로우
- LLM 통합

## 1주차 계획

1. 프로젝트 범위 및 비즈니스 플로우
2. 기술 스택 및 아키텍처
3. 데이터베이스 스키마
4. API 설계
5. 인증 / RBAC
6. 프로젝트 스켈레톤
7. 로드맵

## 브랜치 모델

- master
- develop

## 작업 구조

- frontend/
- backend-java/
- ai-service/
- docs/

## 시작 안내

- 1차 마일스톤: 프로젝트 스켈레톤 + 아키텍처 + DB 설계
- 2차 마일스톤: 관리자 CRUD + 인증
- 3차 마일스톤: 사용자 워크플로우 + AI 통합
- 4차 마일스톤: 테스트 + 배포

## CI 검사

`master` 또는 `develop` 브랜치를 대상으로 한 푸시 및 풀 리퀘스트는 다음 워크플로우(`.github/workflows/ci.yml`)를 실행합니다.

- **backend**: JDK 21 + Maven wrapper 로 백엔드 테스트(`./mvnw test`) 및 패키징
- **frontend**: Node.js 20 + npm ci 로 의존성 설치, 테스트(`npm run test:run`), 빌드(`npm run build`)

> 참고: 백엔드 통합 테스트는 `local` 프로파일과 H2 인메모리 DB를 사용하므로 별도의 PostgreSQL 없이도 CI에서 그대로 실행됩니다.

## 테스트 실행 방법 (로컬)

```bash
# 백엔드 (JDK 21 필요)
cd backend-java && ./mvnw test

# 프론트엔드 (Node.js 20 필요)
cd frontend && npm ci && npm run test:run && npm run build
```
