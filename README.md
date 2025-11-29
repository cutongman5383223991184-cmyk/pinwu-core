<div align="center">
  <h1>PinWu-Core (拼物志)</h1>
  <p>
    <strong>A High-Concurrency C2C Trading Platform Engine</strong><br>
    基于 Spring Boot + Elasticsearch 的高并发二手交易核心系统
  </p>
</div>

---

## 📖 Introduction
PinWu-Core is designed to solve the critical challenges in C2C e-commerce, such as **Flash Sales (Seckill)**, **Geo-spatial Search**, and **AI-Assisted Publishing**. It decouples the high-concurrency C-side API from the management dashboard.

本项目致力于解决二手交易场景下的核心技术痛点。区别于传统的 CRUD 后台，PinWu-Core 重点攻克了**秒杀高并发**、**LBS 混合搜索**以及**多模态 AI 辅助发布**等技术难题。

## 🛠️ Tech Stack
* **Core Framework:** Spring Boot 2.5, MyBatis
* **Search Engine:** Elasticsearch 7.x (LBS + Keyword)
* **Concurrency:** Redis (Lua Script, Distributed Lock), RabbitMQ
* **AI Integration:** Qwen-VL (Multimodal LLM)
* **Security:** Spring Security + JWT

## 🌟 Key Features
* **Smart Publishing:** AI-powered image analysis for auto-tagging.
* **Hybrid Search:** Combined Geo-Distance and Keyword search with function score.
* **Reliable Trading:** Redis Lua script for atomic inventory deduction.

## 🚀 Quick Start
```bash
# 1. Clone repository
git clone [https://github.com/cutongman5383223991184-cmyk/pinwu-core.git](https://github.com/YourName/pinwu-core.git)

# 2. Configure Database & Redis
# Check pinwu-admin/src/main/resources/application-druid.yml

# 3. Build & Run
mvn clean install
java -jar pinwu-admin/target/pinwu-admin.jar