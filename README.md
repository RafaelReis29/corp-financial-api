# Corp Financial API

<img width="405" height="397" alt="Corp API Logo" src="https://github.com/user-attachments/assets/90e1d154-68ad-4387-bc20-123059565f1a" />

A Corp Financial API é uma API de contas a receber para operações B2B. Uma organização pode integrar seu CRM para cadastrar clientes e contratos, registrar faturas e consultar vencimentos e baixas dos registros associados à sua conta.

Os dados de exemplo usam empresas e personagens da cultura pop, como Tony Stark, Bruce Wayne, Oscorp (Homem-Aranha), Umbrella (Resident Evil), InGen (Jurassic Park), Vought (The Boys) e outras, com status em português, datas e valores em BRL. Uma base de dados fácil de explorar, e com tempo pra passar buscando as referências.

## Visão geral

O fluxo principal começa com o cadastro de empresas, continua com contatos e meios de pagamento vinculados pelo campo companyId, avança para contratos vinculados pelo mesmo campo, e termina com faturas vinculadas pelo campo contractId, com indicação opcional de meio de pagamento pelo campo paymentMethodId.

Cada resposta inclui links de navegação, de modo que listagens apontam para detalhes, detalhes apontam para coleções, o que reduz a necessidade de montar endereços manualmente.

Acesse a documentação: https://corp-financial-api.onrender.com/swagger-ui/index.html#/
- Por lá, você pode conferir todas as operações http, as diferentes entidades e seus schemas, realizar requisições entendendo quais campos usar, os limites de caracteres de cada um e quais são esperados na resposta, além de termos de serviço.

## Recursos disponíveis

1. Empresas, com cadastro, listagem paginada, busca por identificador e atualização com criação condicional. A exclusão exige a chave Admin.
2. Contatos, com vínculo obrigatório a empresa por companyId, além de controle de contato principal por isPrimary.
3. Contratos, com valor total em totalValue, moeda em currency, vigência em startDate e endDate, fase em status.
4. Meios de pagamento, com tipo em type, provedor em provider, apelido em label, referência operacional em details, além de isDefault.
5. Faturas, com descrição em description, valor em amount, vencimento em dueDate, baixa em paidAt, fase em status.

## Execução

Com Java 17, execute `sh mvnw spring-boot:run`. O banco H2 em memória é iniciado com os dados de demonstração. A documentação da versão local fica em `/swagger-ui/index.html` e o contrato OpenAPI em `/v3/api-docs`.

| Variável de ambiente | Uso |
| --- | --- |
| `CORP_API_ADMIN_KEY` | Chave de administração, com 32 a 256 caracteres ASCII visíveis. Configure-a para emitir as primeiras chaves de cliente. |
| `CORP_API_CORS_ORIGINS` | Origens autorizadas, separadas por vírgulas. Por padrão, `http://localhost:3000` e `http://localhost:5173`. Informe a origem do seu front-end na implantação. |
| `CORP_API_REQUESTS_PER_MINUTE` | Limite da chave Admin e das requisições sem chave válida, por IP. O padrão é 120 por minuto. |

Para consultas paginadas, use `page`, `size` e `sort`. Para atualização, envie o corpo completo. Para baixa de fatura, envie o status `PAGO` com `paidAt` preenchido. Para fatura em aberto, use `PENDENTE` com `paidAt` ausente.

## Contas e chaves

A conta identifica a organização que usa a API. Cada empresa cadastrada representa um cliente e pertence a uma única conta. O `docNumber` deve ser único por conta. Duas contas podem cadastrar o mesmo documento e manter registros independentes.

Use `X-API-Key` em todas as operações. No Swagger UI, clique em **Authorize** e informe a credencial.

| Chave | Mensalidade de referência | Limite | Operações |
| --- | ---: | --- | --- |
| Básica (`cf_basic_`) | R$ 49 | 60 req/min por chave | Consulta, criação e atualização de registros associados à conta. Exclusão de registros dependentes quando não houver outros vínculos. |
| Premium (`cf_premium_`) | R$ 149 | 300 req/min por chave | As mesmas operações da Básica, com maior capacidade de chamadas. |
| Demo (`cf_demo_`) | Gratuita | 30 req/min por IP e 300 req/min compartilhadas | `GET` de listas e detalhes nos registros da conta de demonstração. |

As mensalidades descrevem os planos. A API não executa cobrança. Básica e Premium podem excluir os próprios contatos, contratos, faturas e meios de pagamento. Para excluir um registro referenciado por outro, remova antes o vínculo ou o registro dependente. A exclusão de empresas é exclusiva do Admin.

### Experimentar a API

Use a chave pública `cf_demo_0123456789abcdefghijklmnopqrstuvwxyzABCDEFG` e faça um `GET /companies` no Swagger. Os dados da carga inicial pertencem à conta de demonstração.

### Criar uma conta e emitir uma chave

1. Com a chave Admin, envie `POST /accounts` com `{"name":"Minha organização"}` e copie o `id`. No Swagger, o campo de idempotência é preenchido automaticamente na 2.0.0. Use `GET /accounts` para listar as contas.
2. Envie `POST /api-keys` com `{"label":"Financeiro","accountId":"<id da conta>","type":"BASIC"}`. Os tipos disponíveis para emissão são `BASIC` e `PREMIUM`.
3. Use o valor de `apiKey` nas operações da nova conta. Guarde o segredo retornado na emissão. O banco guarda seu hash SHA-256. O Admin pode listar metadados em `GET /api-keys`, consultar uma credencial em `GET /api-keys/{id}` e revogá-la com `DELETE /api-keys/{id}`.

A credencial de cliente identifica a conta cujos registros podem ser acessados. Com a chave Admin, envie `X-Account-Id` para selecionar uma conta. Em `DELETE /companies/{id}`, a seleção é obrigatória. As outras operações com a chave Admin usam a demonstração por padrão. No H2 em memória, contas e chaves emitidas são descartadas ao reiniciar e a demonstração é recriada.

## Idempotência e limite de requisições

Na versão `2.0.0`, todo `POST` precisa de `X-Idempotency-Key`. Para chamadas fora do Swagger, gere um UUID no cliente e reutilize a chave com o mesmo corpo em cada tentativa da operação. Um `POST` válido sem a chave recebe `400 Bad Request`. Nas versões `1.0.0` e `1.0.1`, o servidor processa cada envio separadamente e ignora o cabeçalho.

Durante 24 horas, a mesma credencial recebe o resultado original e o cabeçalho `Idempotency-Replayed: true`. Um reenvio enquanto a criação está em andamento, ou com dados diferentes, retorna `409`. O histórico de idempotência e os limites ficam na memória da instância.

No Swagger UI, **Try it out** preenche o campo vazio com um UUID na `2.0.0`. A chave continua visível depois de **Execute**. Para começar uma nova criação, limpe o campo antes de executar ou informe outra chave. Ao selecionar uma versão 1.x, o campo some da operação.

Cada chamada consome um token. A Básica repõe um token por segundo e a Premium repõe cinco por segundo. Consulte `X-RateLimit-Limit` e `X-RateLimit-Remaining` para acompanhar a capacidade disponível. Na Demo, cada IP tem seu próprio saldo de 30 req/min e todos os IPs também consomem de um saldo compartilhado de 300 req/min. Os cabeçalhos mostram a cota do IP, exceto quando o bloqueio vem da cota compartilhada. Ao receber `429`, aguarde o número de segundos indicado em `Retry-After`. O IP é o endereço remoto informado pelo servidor. Em uma implantação atrás de proxy, configure o servidor para confiar somente no proxy que controla o cabeçalho de encaminhamento.

## Versionamento

A versão da aplicação é **2.0.0**. As regras de conta, autenticação e plano valem para as três versões do contrato HTTP.

Envie `X-API-Version` com `1.0.0`, `1.0.1` ou `2.0.0`. Sem o cabeçalho, a API usa `2.0.0`. Uma versão desconhecida ou malformada retorna `400 Bad Request`. No Swagger, use **Versão padrão** no cabeçalho. Cada operação pode usar a versão escolhida no seu próprio campo `X-API-Version`.

`GET /companies/{id}` devolve os campos da empresa na raiz do JSON em `1.0.0` e `1.0.1`. A versão `2.0.0` agrupa os mesmos campos em `company`. Ambas as representações incluem os links de navegação.

## Respostas

| Status | Quando é retornado |
| --- | --- |
| `200 OK` | Consulta ou atualização bem-sucedida. Listagens retornam `200` mesmo quando a página está vazia. |
| `201 Created` | Criação bem-sucedida. O cabeçalho `Location` aponta para o recurso criado. |
| `204 No Content` | Exclusão bem-sucedida, com corpo vazio. |
| `400 Bad Request` | Versão, corpo, identificador, paginação ou ordenação inválida. |
| `401 Unauthorized` | Chave ausente ou inválida em uma operação protegida. |
| `403 Forbidden` | A credencial não permite a operação, como uma escrita com Demo ou uma exclusão de empresa com Básica ou Premium. |
| `404 Not Found` | Registro não encontrado para a conta selecionada ou conta indicada pelo Admin não encontrada. |
| `409 Conflict` | Conflito de idempotência, valor único duplicado ou vínculo incompatível com a operação. |
| `415 Unsupported Media Type` | Formato do corpo não aceito. |
| `429 Too Many Requests` | Limite excedido, com `Retry-After` em segundos. |
| `503 Service Unavailable` | O armazenamento temporário de idempotência atingiu a capacidade. |

Erros da aplicação usam `application/problem+json` com `status` e `detail`. Quando o corpo falha na validação, `errors` identifica os campos e descreve o problema. A exclusão de um registro de domínio inexistente retorna `404` com corpo vazio.

Um `PUT` em um ID existente retorna `200`. Se o ID não pertencer à conta, o servidor cria um registro associado a essa conta com ID gerado e retorna `201`. Referências enviadas no corpo, como `companyId`, `contractId` e `paymentMethodId`, devem pertencer à mesma conta. O `Location` indica a URI efetiva do novo registro e permite encontrá-lo sem ler o corpo. O link `self` no corpo informa o mesmo destino para clientes que navegam pelos links HATEOAS. Para continuar trabalhando com um registro criado por `PUT`, use a URI retornada.

## Carga inicial

Na inicialização, o sistema cria na conta de demonstração 22 empresas, 47 contatos distribuídos entre elas, 6 meios de pagamento dos tipos PIX, TRANSFERENCIA, BOLETO e CARTAO, 6 contratos ativos com valores, vigência e moeda BRL, e 15 faturas nos estados PAGO, PENDENTE e ATRASADO, o que permite explorar listagens, buscas, vínculos e baixas sem cadastro prévio.

Os estados de contrato incluem ATIVO, RASCUNHO, SUSPENSO, ENCERRADO e CANCELADO, os estados de fatura incluem PENDENTE, PAGO, VENCIDO, ATRASADO e CANCELADO, os tipos de pagamento incluem PIX, BOLETO, TRANSFERENCIA e CARTAO.

## Organização do código

Cada recurso tem entidade, repositório, montador de links e controlador. Um tratador global converte erros de validação, autorização e persistência em respostas HTTP. A política de acesso, o limite de requisições e o mecanismo de idempotência são compartilhados entre os controladores.

## Decisões de modelagem

`Account` identifica a organização que usa a API e é dona de cada `Company`. Contatos, contratos e meios de pagamento herdam a conta pela empresa. Faturas herdam a conta pelo contrato. O isolamento é aplicado nas consultas, escritas e verificações de vínculos. Cada meio de pagamento guarda uma referência operacional em `details`.
