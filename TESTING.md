# Testes de caracterização — etapa 1

## Remoção do navegador — resultado atual

O backend não contém mais serviço de scraping ou dependência de navegador.
Os três testes exclusivos do caminho por URL foram retirados juntamente com esse
comportamento; os oito testes de ingestão raw/listagem foram preservados e usam
o construtor sem scraper. Dois testes MVC novos verificam 410 Gone para a rota
aposentada, com e sem URL, e nenhuma interação com ProductService.

Resultado: **32 testes aprovados, zero falhas/erros/ignorados**, e `clean package`
aprovado com as quatro classes isoladas. A limpeza evita empacotar classes antigas.

```powershell
.\mvnw.cmd -o -B "-Dtest=ProductControllerTest,ProductServiceTest,ProductMapperTest,AiServiceTest" clean package
```

ScrapedProductDto, ingestão raw, consultas e integração Gemini foram preservados.
O contexto completo não foi executado, pois ainda depende do banco/configuração;
a dependência de Chromium na inicialização foi eliminada. Nenhum banco foi acessado.
As seções abaixo registram etapas anteriores e seus resultados históricos.

## Consolidação do processamento — 07/11/2026

`ProductService.createFromUrl` mantém o scraping e `createFromRawData` mantém a
entrada por DTO. Ambos delegam ao método privado `processAndSave`, que concentra
mapeamento, análise, avaliações, associação bidirecional, save e resposta.
Não foram adicionadas transações nem alterados contratos, schema ou integração IA.

Três testes foram adicionados em `ProductServiceTest`: equivalência das respostas
e dos agregados persistidos, equivalência quando uma análise falha e propagação
da falha do scraper sem chamar IA/persistência. A igualdade de entidades com relações
circulares não é usada: as referências são verificadas por identidade.

Resultado atual: **33 testes aprovados**, sem erros ou ignorados, e **build Maven
package aprovado**, incluindo repackage do Spring Boot. A execução selecionou as
mesmas quatro classes abaixo. O teste de contexto completo permaneceu fora da seleção.
O build precisou baixar dependências ausentes do plugin Maven após uma tentativa
offline; não houve alteração no pom ou acesso a serviços da aplicação.

```powershell
.\mvnw.cmd -B "-Dtest=ProductControllerTest,ProductServiceTest,ProductMapperTest,AiServiceTest" package
```

## Registro da caracterização inicial

Executar somente a rede de segurança isolada (Java 21):

```powershell
.\mvnw.cmd -B "-Dtest=ProductControllerTest,ProductServiceTest,ProductMapperTest,AiServiceTest" test
```

Na execução de 07/11/2026, o wrapper não conseguiu inicializar no sandbox. Foi usado
o Maven 3.10.0 já instalado em `.m2/wrapper/dists`, em modo offline (`-o`), com
`-Dmaven.repo.local=C:\Users\joilsonmslopes\.m2\repository` e a mesma seleção de testes.
Resultado: **30 testes, zero falhas, zero erros, zero ignorados**.

## Escopo

- `ProductControllerTest` (5): MockMvc standalone, JSON de entrada/saída, status 201,
  paginação padrão/explícita, enum de sentimento e ProblemDetail em falhas do serviço.
  Não carrega WebConfig, interceptor, datasource ou aplicação Spring completa.
- `ProductServiceTest` (8): mapper real, repositories/IA/scraper com Mockito; montagem
  do agregado, referência de cada Rating ao mesmo Product, ordem de comentários e
  sentimentos, save, retorno do identificador, listas vazias/nulas, falha de tarefas,
  falha de persistência e paginação/ordenação.
- `ProductMapperTest` (3): campos escalares, responsabilidades da conversão, dados
  nulos, conversão de sentimentos e preservação das referências existentes.
- `AiServiceTest` (14, incluindo parametrizações): RestClient real com transporte
  substituído por MockRestServiceServer; prompt/método/URI, sentimentos, respostas
  ausentes/incompletas/inválidas, erros 429/500/503 e URL não configurada. A criação
  interna do RestClient é interceptada com Mockito estático somente no construtor.

Nenhuma chamada real a Gemini, PostgreSQL ou Playwright. Nenhuma dependência nova.
O teste preexistente `AnalisadorApiApplicationTests.contextLoads` não foi executado:
ele inicia o contexto completo e depende de configuração externa e do navegador.
Não foi removido, desabilitado nem alterado. Portanto este resultado não afirma que
um `mvn test` sem filtro passe.

## Comportamentos problemáticos registrados, sem correção

1. AiService converte erros HTTP, respostas incompletas e JSON inválido em NEUTRO,
   assim como sentimentos desconhecidos. Não distingue falha técnica de neutralidade.
2. Se uma tarefa de análise lança exceção, ProductService descarta esse comentário
   e ainda salva os demais. Se todas falham, salva um produto sem avaliações.
3. Lista de comentários nula provoca NullPointerException antes de salvar.
4. URL Gemini ausente provoca NullPointerException antes do bloco de fallback.

Esses testes caracterizam o comportamento atual; não o apresentam como a regra ideal.
Mudanças intencionais posteriores devem atualizar o requisito e a asserção correspondente.

## Limite da persistência simulada

Os testes verificam o agregado entregue ao repository, o vínculo bidirecional e a
propagação de erros de save. Não verificam SQL, cascade efetiva do Hibernate, commit,
rollback ou constraints em PostgreSQL. Isso exige testes de integração com banco
isolado em uma etapa posterior. O schema e os mapeamentos de produção não mudaram.
