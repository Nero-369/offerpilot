# OfferPilot Backend

## v5 Qwen RAG architecture

`POST /api/v1/knowledge` imports policy/JD text. The backend normalizes paragraphs,
creates 900-character chunks with 140-character overlap, calls Qwen
`text-embedding-v4` in batches, and stores 1024-dimensional vectors in PostgreSQL
with a pgvector HNSW cosine index.

`POST /api/v1/chat` embeds the question, retrieves the five nearest chunks,
augments the Offer and city context, then calls Qwen through Spring AI. The API
returns citations and a trace containing embedding, vector search and LLM latency.

Required environment variables:

```env
AI_ENABLED=true
QWEN_API_KEY=replace-locally-never-commit
QWEN_CHAT_MODEL=qwen-plus
QWEN_EMBEDDING_MODEL=text-embedding-v4
QWEN_API_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
AI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
```

Deployment shortcuts:

```bash
chmod +x deploy.sh
./deploy.sh frontend
./deploy.sh backend
./deploy.sh all
./deploy.sh status
./deploy.sh logs backend
```

Spring Boot 4 + Spring AI 2 backend for the OfferPilot decision agent.

## Local stack

1. Copy `.env.example` to `.env` and replace the passwords.
2. Keep `AI_ENABLED=false` to run without a model key, or set an API key and enable it.
3. Run `docker compose up --build -d`.
4. Open `http://localhost`; health endpoint: `http://localhost/actuator/health`.

Only ports 80/443 should be opened on a cloud security group. PostgreSQL and Redis are intentionally isolated on the private Docker network.

## Core API

- `POST /api/v1/offers` creates an Offer.
- `GET /api/v1/offers` lists Offers.
- `POST /api/v1/analyses?offerId={id}` starts an asynchronous analysis.
- `GET /api/v1/analyses/{id}` returns progress and the structured report.

The current tax/social-insurance calculator is an engineering demo and must be replaced with versioned, city-specific policy rules before real decision use.
