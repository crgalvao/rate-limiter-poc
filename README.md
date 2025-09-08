# 🔄 Rate Limiter Distribuído de Alta Performance

## 📖 Visão Geral
Este projeto implementa um **rate limiter distribuído** em **Java 21** com **Spring Boot**.  
Foi desenvolvido inicialmente como um desafio técnico, mas segue uma estrutura pensada para produção, priorizando clareza, manutenção e possibilidade de evolução.

### Características principais
- 🚀 Suporta mais de **100 milhões de requisições por minuto** em múltiplos servidores  
- 📦 Usa **lotes locais com flush agendado**, reduzindo chamadas de rede  
- 🛡️ Garante **segurança em concorrência** com `LongAdder` e `ConcurrentHashMap`  
- ✅ Oferece **precisão aproximada**: cada cliente sempre tem pelo menos seu limite garantido, mas pode ultrapassá-lo levemente  
- 🧩 Código dividido em camadas bem definidas, seguindo princípios de **Clean Architecture**  

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
```

- Os limites são de **requisições por 60 segundos**  
- Clientes desconhecidos são **rejeitados**  
- Pode ser estendido futuramente para carregar de um **banco de dados** ou **serviço de configuração**  

---

## 🚀 Como Executar

### 🔹 Opção 1: Docker direto
```bash
docker compose up --build
```

### 🔹 Opção 2: Docker Compose
```bash
docker build -t ratelimiter-api . && docker run -p 8080:8080 ratelimiter-api
```

A API ficará disponível em:  
👉 [http://localhost:8080/ratelimit?clientId=xyz](http://localhost:8080/ratelimit?clientId=xyz)

---

## 🌐 Uso da API

### Exemplo de requisição:
```bash
curl "http://localhost:8080/ratelimit?clientId=xyz"
```

### Exemplo de resposta:
```
Client xyz allowed? true
```

---

## ✅ Testes
Rodar a suíte completa:  
```bash
mvn test
```