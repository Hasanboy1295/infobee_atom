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

## ATOM 예측의 ML 모델 (EDBO 데이터 기반)

`/predict/atom`은 기존의 해석 가능한 결정론적 반응-동역학 근사 엔진 위에, ATOM 연구 데이터(`01_public_validation/edbo_direct_arylation_experiment_index.csv`, 1728 반응)로 학습한 **scikit-learn ML 모델**(HistGradientBoosting)을 함께 제공합니다.

- **모델 산출물**: `ai-service/atom_model.joblib` (`ai-service/train_model.py`로 재학습 가능)
- **성능**: holdout 테스트에서 MAE≈6.7%, R²≈0.785 (평균 예측의 MAE≈19.4%와 비교)
- **특징**: 분자 조성 기술자(SMILES에서 원자수/대략적 분자량) + 온도 + 농도를 입력으로 사용
- **동작 방식**:
  - 입력 JSON에 `baseSmiles` / `ligandSmiles` / `solventSmiles`(선택)를 제공하면 ML 결과가 `resultData.mlPrediction`에 포함됩니다.
  - SMILES가 없으면 결정론적 엔진 결과만 반환합니다 (ML 결과는 `null`/`unavailable`).
  - ML 모델이 없어도(SMILES 누락 또는 모델 로드 실패) 서비스는 결정론적 엔진으로 정상 동작합니다.
- **요청 예시**:
  ```json
  {
    "inputConditions": {
      "temperatureC": 105,
      "concentrationMgMl": 10,
      "reactionTimeMin": 120,
      "baseSmiles": "O=C([O-])C.[K+]",
      "ligandSmiles": "CC(C)(C)P(C1=CC=CC=C1)C(C)(C)C",
      "solventSmiles": "CC(N(C)C)=O"
    }
  }
  ```
  응답의 `resultData`에 `mlPrediction.predictedYieldPercent`, `trainR2`, `trainMae`, `nSamples`이 포함됩니다.

### ML 모델 재학습

```bash
cd ai-service
python train_model.py   # ai-service/atom_model.joblib 재생성
```

