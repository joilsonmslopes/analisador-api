# Processamento de marketplaces

O DOM é coletado exclusivamente pela extensão Chrome. O backend valida e normaliza os dados antes do mapeamento, análise de sentimentos e persistência. Nenhuma estratégia acessa páginas ou depende de navegador.

## Contrato

`POST /api/products/analyze-raw` aceita o campo opcional `marketplace`:

```json
{
  "marketplace": "mercado_livre",
  "name": "Mouse",
  "currentPrice": 19.90,
  "originalLink": "https://produto.mercadolivre.com.br/MLB-123",
  "ratings": ["Muito bom"]
}
```

O ID é estável, sensível a maiúsculas e independente do nome da classe. Payloads antigos sem `marketplace` continuam aceitos: o Registry identifica a estratégia pelo host. Um ID explícito desconhecido (inclusive vazio) nunca recorre à inferência. IDs desconhecidos, URLs inválidas, domínios divergentes e inferência ambígua geram HTTP 400 via `GlobalExceptionHandler`.

Atualmente, `mercado_livre` aceita `mercadolivre.com.br` e seus subdomínios, com fronteira de domínio: `mercadolivre.com.br.evil.com` é rejeitado. São aceitas URLs HTTP(S) absolutas sem credenciais, usando porta padrão ou implícita. Outros domínios internacionais não estão registrados.

O MercadoLivreProcessor exige nome não vazio e preço não negativo. Retorna uma cópia com ID explícito, espaços dos textos normalizados, avaliações nulas/em branco removidas e lista ausente convertida em vazia. Preserva ordem e duplicatas das avaliações e o valor do preço. Normaliza host e esquema, remove porta padrão e fragmento da URL, preservando caminho escapado e query (que pode identificar variações).

Não há mudança de schema, entidade ou resposta. A extensão atual pode continuar enviando seu contrato existente.

## Adicionar um marketplace

1. Crie uma classe no pacote da aplicação implementando `MarketplaceProcessor` e anote com `@Component`.
2. Defina `marketplaceId()` com um ID permanente no formato `[a-z][a-z0-9_]*`. Não renomeie IDs já publicados.
3. Implemente `supportsHost` com comparação exata ou sufixo precedido de ponto; evite substring. O Registry fornece o host em minúsculas. Evite sobreposição de domínios para manter a inferência legada inequívoca.
4. Implemente `process` validando as regras específicas e retornando uma cópia normalizada. Reutilize `MarketplaceUrl` para validação sintática. Não faça scraping ou requisições de navegador.
5. Adicione testes das regras, hosts legítimos e falsificados, identificação explícita e legada e normalização. Se necessário, estenda a coleta DOM na extensão e envie o novo ID.

O Spring injeta todas as estratégias no Registry. IDs duplicados ou malformados impedem sua inicialização. Não é necessário alterar `ProductService`, Registry ou um enum central para registrar uma implementação nova.

Execute os testes unitários com:

```sh
mvn -Dtest=MarketplaceProcessorTest,ProductServiceTest,ProductControllerTest,ProductMapperTest,AiServiceTest test
```
