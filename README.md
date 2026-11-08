# Marketplace Analyzer — API

Backend Java 21 e Spring Boot 3.3.4 para receber produtos coletados pela extensão
Chrome, analisar comentários com Gemini e persistir produtos/avaliações em PostgreSQL.
A coleta de páginas é responsabilidade exclusiva da extensão.

## Execução

Configure as variáveis DB_URL (JDBC PostgreSQL), DB_USERNAME, DB_PASSWORD,
GEMINI_API_KEY, GEMINI_API_URL e CORS_ALLOWED_ORIGINS. Veja
src/main/resources/application.properties. Execute com JDK 21:

```powershell
.\mvnw.cmd spring-boot:run
```

Não é necessário navegador no backend. O Dockerfile usa imagens Temurin 21 para
compilação e execução, sem instalação de bibliotecas gráficas.

## Contratos

- GET /api/products/all?page=0&size=10&sort=NAME_ASC: página de produtos e avaliações.
  Ordenações: NAME_ASC, NAME_DESC, PRICE_ASC e PRICE_DESC.
- POST /api/products/analyze-raw: recebe o JSON abaixo e retorna 201 com o produto
  persistido, id e avaliações com originalText/sentiment.
- POST /api/products/scrape: descontinuado; retorna 410 Gone com ProblemDetail e
  orientação para /api/products/analyze-raw. Não coleta dados, não chama Gemini e
  não persiste nada. A URL não é mais obrigatória, para sempre comunicar a retirada.

```json
{
  "name": "Mouse",
  "currentPrice": 19.90,
  "originalLink": "https://produto.mercadolivre.com.br/MLB-123",
  "ratings": ["Bom"]
}
```

O DTO ScrapedProductDto foi preservado por fazer parte do contrato de ingestão.
Os campos de resposta e sentimentos POSITIVO, NEGATIVO e NEUTRO foram mantidos.

## Arquitetura atual

ProductController recebe o DTO; ProductService delega a processAndSave para mapear,
analisar comentários, associar Product/Rating e salvar o agregado; ProductMapper
produz a resposta. AiService usa RestClient e DTOs próprios do Gemini. A análise
paralela com virtual threads ocorre antes do save, sem nova transação envolvendo
chamadas externas. Não foi implementado um novo Strategy/Registry nesta etapa.

## Retirada do navegador — 07/11/2026

A dependência Playwright, o serviço de scraping, Factory, Strategies, validador de
URL exclusivo do fluxo antigo e tratamento de erro headless foram removidos.
Não há coleta fictícia ou fallback de scraping no backend.

Nos fontes conhecidos, a extensão usa analyze-raw e all; o dashboard usa all.
Não foram encontrados consumidores ativos de scrape nesses projetos. Como não é
possível excluir consumidores externos, foi escolhida a resposta 410 Gone, em vez
da remoção direta da rota. A decisão foi confirmada pelo responsável pelo projeto.

Banco de dados, schema, configuração Gemini e extensão não foram alterados.
Dados de perfil/cache de navegador eventualmente existentes no ambiente de deploy
não foram acessados nem apagados; podem ser descartados operacionalmente após
verificar que não são compartilhados por outro processo.

## Testes

Veja TESTING.md para execução isolada, resultados e limitações. O teste legado de
contexto completo ainda depende do banco e da configuração da aplicação.

## Processamento de marketplaces

Consulte [o contrato e o guia de extensão](docs/marketplaces.md) para adicionar estratégias de validação e normalização sem alterar o fluxo central.
