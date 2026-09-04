# OfferPilot 离线 RAG 评测

1. 复制 `rag-eval.example.json`，为每个问题填写真实的 `expectedDocumentIds`、`expectedSourceUrls` 或 `expectedKeywords`。
2. 确保 OfferPilot 已启动，并已导入评测所需文档。
3. 在 `backend` 目录运行：

```powershell
.\run-rag-eval.ps1 -Dataset .\rag-eval.example.json
```

默认请求 `http://localhost/api/v1/knowledge/evaluate`。如果后端直接暴露在 8080 端口，可传入：

```powershell
.\run-rag-eval.ps1 -BaseUrl http://localhost:8080
```

输出包括 Hit@K、MRR、关键词覆盖率、平均检索耗时和每条用例的命中详情。该离线接口不会调用聊天模型，也不会影响正常问答。
