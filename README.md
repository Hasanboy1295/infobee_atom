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

`master` 또는 `develop` 브랜치를 대상으로 한 푸시 및 풀 리퀘스트는 백엔드 Maven 테스트/패키지 검사와 프론트엔드 설치, 테스트, 빌드, 그리고 높은 심각도의 프로덕션 의존성 감사를 실행합니다.
