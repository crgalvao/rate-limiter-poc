# Rate Limiter Distribuído de Alta Performance

## Visão Geral
Este projeto implementa um **rate limiter distribuído** em **Java 21** com **Spring Boot**.  
Foi desenvolvido como um desafio técnico, mas segue uma estrutura próxima de produção, priorizando clareza, manutenção e evolução.

---

## Estrutura do Projeto
```
domain/         → Regras de negócio (rate limiter, interface de KV store)
application/    → Serviços que usam o rate limiter e aplicam configurações
infrastructure/ → Implementações técnicas (in-memory e suporte a Redis)
controller/     → Endpoints REST
config/         → Configuração centralizada dos limites por cliente
interceptor/    → Intercepta todas as requisições HTTP, valida o cabeçalho X-Client-ID e aplica as regras do RateLimiterService, bloqueando excessos antes de chegar aos controllers

```

---

## Configuração
Arquivo: `src/main/resources/application.yml`

```yaml
ratelimiter:
  clients:
    - id: "xyz"
      limit: 500
    - id: "abc"
      limit: 200
    - id: "free"
      limit: 3
    - id: "premiumUser"
      limit: 2000
  performance:
    window-seconds: 60
    flush-interval-ms: 100
    shard-count: 10
```

- **clients**: limite de requisições por cliente (janela de 60s).  
- **performance**: parâmetros internos para ajuste de performance.  
- Clientes sem `X-Client-ID` ou não configurados são rejeitados.  

---

## Como Executar

**Opção 1: Docker Compose**
```bash
docker compose up --build
```

**Opção 2: Docker**
```bash
docker build -t ratelimiter-api .
docker run -p 8080:8080 ratelimiter-api
```

---

## Uso da API

**Requisição:**
```bash
curl -v -H "X-Client-ID: xyz" http://localhost:8080/api/products
```

**Resposta (200 OK):**
```
Returning list of products for client: xyz
```

**Resposta limite excedido (429 Too Many Requests):**
```
HTTP/1.1 429 Too Many Requests
```

**Resposta sem cabeçalho (400 Bad Request):**
```bash
curl -v http://localhost:8080/api/products
HTTP/1.1 400 Bad Request
```

---

## Testes
Para rodar a suíte de testes:
```bash
mvn test
```
