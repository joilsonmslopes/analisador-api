# 🟢 Marketplace Analyzer - API (Backend)

> Motor de Inteligência Competitiva que realiza web scraping de e-commerces (Mercado Livre) e processa a análise de sentimento dos consumidores utilizando a IA do Google Gemini.

## 🚀 Tecnologias e Stack

* **Linguagem:** Java 21
* **Framework:** Spring Boot 3.3.4
* **Web Scraping:** Playwright Chromium (Headless)
* **Inteligência Artificial:** Google Gemini API (via Spring RestClient)
* **Banco de Dados:** PostgreSQL (Supabase)
* **Concorrência:** Virtual Threads (Project Loom)

## 🧠 Decisões Arquiteturais e de Engenharia

Este projeto foi construído focando em **Clean Architecture**, princípios **SOLID** e alta performance:

1. **Concorrência de Alta Performance com Virtual Threads:**
   * **O Problema:** O envio de dezenas de avaliações sequenciais para a API do Gemini criava um gargalo de rede (N+1 I/O Blocking), travando a resposta HTTP.
   * **A Solução:** Implementação do padrão de concorrência estruturada (`Structured Concurrency`) com `Executors.newVirtualThreadPerTaskExecutor()` do Java 21. As requisições para a LLM agora ocorrem em paralelo, escalando maciçamente sem esgotar as threads do Sistema Operacional.

2. **Gerenciamento de Ciclo de Vida do Navegador (Prevenção de OOM):**
   * **O Problema:** Instanciar um navegador completo a cada requisição de scraping causava picos de consumo de RAM (Out-Of-Memory) e lentidão.
   * **A Solução:** O `Playwright` e o `BrowserContext` foram configurados como *Singletons* gerenciados pelo Spring (`@PostConstruct` e `@PreDestroy`). Cada extração ocorre isoladamente em uma nova aba (`Page`), que é destruída de forma segura no bloco `finally`, mantendo o consumo de memória linear.

3. **Integração REST Imutável e Type-Safe:**
   * **A Solução:** Remoção de bibliotecas pesadas de abstração. A comunicação com o Gemini foi feita diretamente com o `RestClient` nativo do Spring Boot 3, mapeando o JSON de resposta para **Records** do Java 21 (`GeminiResponse`, `Candidate`, `Part`). Isso elimina *warnings*, *casts* inseguros e garante a imutabilidade dos DTOs.

4. **Segurança de Borda (Global CORS):**
   * Configuração centralizada utilizando `WebMvcConfigurer`, restringindo o consumo da API exclusivamente à porta do frontend React (`http://localhost:5173`), sem vazamento de anotações soltas nos Controllers.

## ⚙️ Como Executar Localmente

### Pré-requisitos
* Java Development Kit (JDK 21+)
* Maven
* Chave de API do Google Gemini (Google AI Studio)
* Banco de Dados PostgreSQL

### Configuração
1. Clone o repositório e acesse a pasta do backend:
   ```bash
   git clone https://github.com/seu-usuario/marketplace-analyzer.git
   cd marketplace-analyzer/backend
   ```

2. Configure o arquivo `src/main/resources/application.properties`:
   ```properties
   # Supabase / Postgres
   spring.datasource.url=jdbc:postgresql://<SEU_HOST>:5432/postgres
   spring.datasource.username=<SEU_USER>
   spring.datasource.password=<SUA_SENHA>
   spring.jpa.hibernate.ddl-auto=update
   
   # Google Gemini API
   gemini.api.key=SUA_CHAVE_AQUI
   gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent
   ```

3. Inicie o servidor Spring Boot:
   ```bash
   ./mvnw spring-boot:run
   ```
   A API estará disponível em `http://localhost:8080`.

## 📡 Endpoints (REST)

| Método | Rota | Descrição |
| :--- | :--- | :--- |
| `GET` | `/api/products/all` | Retorna todos os produtos do banco e o histórico de análises de sentimento. |
| `POST` | `/api/products/scrape?url={URL}` | Recebe um link do Mercado Livre, realiza o scraping, analisa o sentimento via IA e salva os resultados. |