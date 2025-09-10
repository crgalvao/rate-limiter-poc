# 🔄 Rate Limiter Distribuído de Alta Performance

## 📖 Visão Geral
Este projeto implementa um **rate limiter distribuído** em **Java 21** com **Spring Boot**.  
Foi desenvolvido inicialmente como um desafio técnico, mas segue uma estrutura pensada para produção, priorizando clareza, manutenção e possibilidade de evolução.

---

## 🏗️ Estrutura do Projeto
```
domain/         → Regras de negócio (rate limiter, interface de KV store)
application/    → Serviços que usam o rate limiter e aplicam configurações
infrastructure/ → Implementações técnicas (in-memory e pronto para Redis)
controller/     → Endpoints REST
config/         → Configuração centralizada dos limites por cliente
```

---

## ⚙️ Configuração de Clientes e Limites
Os limites estão definidos em `src/main/resources/application.yml`:

```yaml
ratelimiter:
  clients:
    - id: "xyz"
      limit: 500
    - id: "abc"
      limit: 200
    - id: "premiumUser"
      limit: 2000
  performance:
    window-seconds: 60
    flush-interval-ms: 100
    shard-count: 10
```

- clients: Define o limite de requisições por 60 segundos para cada id de cliente.
- performance: Permite ajustar os parâmetros internos do algoritmo para otimizar a performance.
- clientes não identificados ou sem o cabeçalho X-Client-ID são rejeitados.

---

## 🚀 Como Executar

### 🔹 Opção 1: Docker direto
```bash
docker compose up --build
```

### 🔹 Opção 2: Docker Compose
```bash
docker build -t ratelimiter-api .
docker run -p 8080:8080 ratelimiter-api
```

## 🌐 Uso da API

### Exemplo de requisição:
```bash
curl -v -H "X-Client-ID: xyz" http://localhost:8080/api/products
```

### Exemplo de resposta:
```
HTTP/1.1 200 OK
Content-Type: text/plain;charset=UTF-8

Returning list of products for client: xyz

```

### Resposta de limite excedido (429 Too Many Requests):
```
Após exceder o limite configurado, a mesma requisição receberá:

HTTP/1.1 429 Too Many Requests

### Resposta para cabeçalho ausente (400 Bad Request):
```
Bash
curl -v http://localhost:8080/api/products
HTTP/1.1 400 Bad Request

```
---

## ✅ Testes
Rodar a suíte completa:  
```bash
mvn test
```