# Corp Financial API

<img width="405" height="397" alt="Corp API Logo" src="https://github.com/user-attachments/assets/90e1d154-68ad-4387-bc20-123059565f1a" />

A Corp Financial API é uma API de contas a receber para operações B2B. Uma organização pode integrar seu CRM para cadastrar clientes e contratos, registrar faturas e consultar vencimentos e baixas dos registros associados à sua conta.

Os dados de exemplo usam empresas e personagens da cultura pop, como Tony Stark, Bruce Wayne, Oscorp (Homem-Aranha), Umbrella (Resident Evil), InGen (Jurassic Park), Vought (The Boys) e outras, com status em português, datas e valores em BRL. Uma base de dados fácil de explorar, e com tempo pra passar buscando as referências.

## Visão geral

O fluxo principal começa com o cadastro de empresas, continua com contatos e meios de pagamento vinculados pelo campo companyId, avança para contratos vinculados pelo mesmo campo, e termina com faturas vinculadas pelo campo contractId, com indicação opcional de meio de pagamento pelo campo paymentMethodId.

Cada resposta inclui links de navegação, de modo que listagens apontam para detalhes, detalhes apontam para coleções, o que reduz a necessidade de montar endereços manualmente.

O Swagger UI apresenta os endpoints, os campos obrigatórios, os exemplos de requisição e as respostas de cada operação. Também permite executar chamadas diretamente pelo navegador. [Abrir documentação interativa](https://corp-financial-api.onrender.com/swagger-ui/index.html#/)

O painel de demonstração permite explorar as entidades em uma interface visual. [Abrir painel](https://rafaelreis29.github.io/corp-api-front/)

## Recursos disponíveis

1. Empresas, com cadastro, listagem paginada, busca por identificador e atualização com criação condicional. A exclusão exige a chave Admin.
2. Contatos, com vínculo obrigatório a empresa por companyId, além de controle de contato principal por isPrimary.
3. Contratos, com valor total em totalValue, moeda em currency, vigência em startDate e endDate, fase em status.
4. Meios de pagamento, com tipo em type, provedor em provider, apelido em label, referência operacional em details, além de isDefault.
5. Faturas, com descrição em description, valor em amount, vencimento em dueDate, baixa em paidAt, fase em status.

## Primeiros passos

Para conhecer a API, utilize a chave pública de demonstração. Ela permite consultar os dados fictícios sem criar uma conta.

1. Abra o Swagger UI e clique em **Authorize**.
2. Informe `cf_demo_0123456789abcdefghijklmnopqrstuvwxyzABCDEFG`.
3. Abra `GET /companies`, clique em **Try it out** e depois em **Execute**.
4. Copie o `id` de uma empresa retornada e consulte `GET /companies/{id}` para explorar seus detalhes.

Também é possível fazer a primeira consulta pelo terminal.

```sh
curl 'https://corp-financial-api.onrender.com/companies?page=0&size=5' \
  -H 'X-API-Key: cf_demo_0123456789abcdefghijklmnopqrstuvwxyzABCDEFG' \
  -H 'X-API-Version: 2.0.0'
```

Uma resposta `200 OK` contém a página de empresas e os links de navegação. A chave Demo permite apenas consultas. Para testar criações, atualizações e exclusões, utilize uma chave Básica ou Premium associada a uma conta de cliente.

## Contas e chaves

A conta identifica a organização que usa a API. Cada empresa cadastrada representa um cliente e pertence a uma única conta. O `docNumber` deve ser único por conta. Duas contas podem cadastrar o mesmo documento e manter registros independentes.

Use `X-API-Key` em todas as operações. No Swagger UI, clique em **Authorize** e informe a credencial.

| Chave | Mensalidade de referência | Capacidade e reposição | Operações |
| --- | ---: | --- | --- |
| Básica (`cf_basic_`) | R$ 49 | Saldo de até 60 chamadas por chave, com reposição de 1 por segundo. | Consulta, criação e atualização de registros associados à conta. Exclusão de contatos, contratos, faturas e meios de pagamento sem dependências. |
| Premium (`cf_premium_`) | R$ 149 | Saldo de até 300 chamadas por chave, com reposição de 5 por segundo. | As mesmas operações da Básica, com maior capacidade de chamadas. |
| Demo (`cf_demo_`) | Gratuita | Saldo de até 30 chamadas por IP, com reposição de 1 a cada 2 segundos. Saldo compartilhado de até 300, com reposição de 5 por segundo. | `GET` de listas e detalhes nos registros da conta de demonstração. |

As mensalidades descrevem os planos. A API não executa cobrança. Básica e Premium podem excluir os próprios contatos, contratos, faturas e meios de pagamento. Para excluir um registro referenciado por outro, remova antes o vínculo ou o registro dependente. A exclusão de empresas é exclusiva do Admin.

### Acesso aos dados

A chave de cliente identifica automaticamente a conta cujos registros podem ser acessados. Empresas, contatos, contratos, faturas e meios de pagamento de outras contas ficam fora desse acesso. Guarde sua chave com cuidado, pois ela autoriza as operações da conta.

Os endpoints `/accounts` e `/api-keys` são destinados à administração de contas e credenciais e exigem acesso Admin. A chave Demo e as chaves de cliente não permitem emitir ou revogar credenciais. O valor de uma nova chave é apresentado apenas na emissão.

## Consultas e alterações

Para consultas paginadas, use `page`, `size` e `sort`. A primeira página é `page=0`. A ordenação usa campos do próprio recurso, por exemplo `sort=tradeName,asc` para empresas. Uma página vazia retorna `200 OK`.

Para cadastrar dados relacionados, comece pela empresa e reutilize seu `id` em `companyId` ao criar contatos, contratos ou meios de pagamento. Para uma fatura, informe o `contractId` do contrato e, opcionalmente, um `paymentMethodId`. As referências devem pertencer à mesma conta da chave utilizada.

Uma atualização por `PUT` recebe o corpo completo do registro. Para baixar uma fatura, envie o status `PAGO` com `paidAt` preenchido. Para uma fatura em aberto, use `PENDENTE` com `paidAt` ausente. O Swagger apresenta os campos obrigatórios e os formatos aceitos em cada operação.

## Idempotência e limite de requisições

Na versão `2.0.0`, todo `POST` precisa de `X-Idempotency-Key`. Para chamadas fora do Swagger, gere um UUID no cliente e reutilize a chave com o mesmo corpo em cada tentativa da operação. Um `POST` válido sem a chave recebe `400 Bad Request`. Nas versões `1.0.0` e `1.0.1`, o servidor processa cada envio separadamente e ignora o cabeçalho.

Durante até 24 horas, repetir a chave de idempotência com o mesmo corpo, credencial, conta e endpoint retorna o resultado original e o cabeçalho `Idempotency-Replayed: true`. Um reenvio enquanto a criação está em andamento, ou com dados diferentes, retorna `409`. Use uma nova chave para cada nova criação.

No Swagger UI, **Try it out** preenche o campo vazio com um UUID na `2.0.0`. A chave continua visível depois de **Execute**. Para começar uma nova criação, limpe o campo antes de executar ou informe outra chave. Ao selecionar uma versão 1.x, o campo some da operação.

Cada chamada consome uma unidade do saldo, que é reposto continuamente até a capacidade do plano. Se houver saldo, várias chamadas podem ser feitas em sequência. Em chamadas espaçadas, o saldo pode se recuperar entre as requisições, fazendo o valor de `X-RateLimit-Remaining` permanecer igual.

Consulte `X-RateLimit-Limit` para conhecer a capacidade da cota e `X-RateLimit-Remaining` para acompanhar o saldo disponível. Na Demo, uma chamada consome tanto a cota do IP quanto a compartilhada. Os cabeçalhos mostram a cota do IP, exceto quando o bloqueio vem da cota compartilhada. Ao receber `429`, aguarde o número de segundos indicado em `Retry-After` antes de tentar novamente.

## Versionamento

A versão da aplicação é **2.0.0**. As regras de conta, autenticação e plano valem para as três versões do contrato HTTP.

Envie `X-API-Version` com `1.0.0`, `1.0.1` ou `2.0.0`. Sem o cabeçalho, a API usa `2.0.0`. Uma versão desconhecida ou malformada retorna `400 Bad Request`. No Swagger, use **Versão padrão** no cabeçalho. Cada operação pode usar a versão escolhida no seu próprio campo `X-API-Version`.

`GET /companies/{id}` devolve os campos da empresa na raiz do JSON em `1.0.0` e `1.0.1`. A versão `2.0.0` agrupa os mesmos campos em `company`. Ambas as representações incluem os links de navegação.

## Respostas

| Status | Quando é retornado |
| --- | --- |
| `200 OK` | Consulta ou atualização bem-sucedida. Listagens retornam `200` mesmo quando a página está vazia. |
| `201 Created` | Criação bem-sucedida. O corpo contém o registro criado. |
| `204 No Content` | Exclusão bem-sucedida, com corpo vazio. |
| `400 Bad Request` | Versão, corpo, identificador, paginação ou ordenação inválida. |
| `401 Unauthorized` | Chave ausente ou inválida em uma operação protegida. |
| `403 Forbidden` | A credencial não permite a operação, como uma escrita com Demo ou uma exclusão de empresa com Básica ou Premium. |
| `404 Not Found` | Registro não encontrado para a conta selecionada ou conta indicada pelo Admin não encontrada. |
| `409 Conflict` | Conflito de idempotência, valor único duplicado ou vínculo incompatível com a operação. |
| `415 Unsupported Media Type` | Formato do corpo não aceito. |
| `429 Too Many Requests` | Limite excedido, com `Retry-After` em segundos. |
| `503 Service Unavailable` | Na versão `2.0.0`, o armazenamento temporário de idempotência atingiu a capacidade. |

Erros da aplicação usam `application/problem+json` com `status` e `detail`. Quando o corpo falha na validação, `errors` identifica os campos e descreve o problema. A exclusão de um registro de domínio inexistente retorna `404` com corpo vazio.

Um `PUT` em um ID existente retorna `200`. Se o ID não pertencer à conta, o servidor cria um registro associado a essa conta com ID gerado e retorna `201`. Referências enviadas no corpo, como `companyId`, `contractId` e `paymentMethodId`, devem pertencer à mesma conta. O link `self` no corpo informa o endereço do novo registro. Para continuar trabalhando com um registro criado por `PUT`, use esse link ou o ID retornado no corpo.

## Carga inicial

Na inicialização, o sistema cria na conta de demonstração 22 empresas, 47 contatos distribuídos entre elas, 6 meios de pagamento dos tipos PIX, TRANSFERENCIA, BOLETO e CARTAO, 6 contratos ativos com valores, vigência e moeda BRL, e 15 faturas nos estados PAGO, PENDENTE e ATRASADO, o que permite explorar listagens, buscas, vínculos e baixas sem cadastro prévio.

Os estados de contrato incluem ATIVO, RASCUNHO, SUSPENSO, ENCERRADO e CANCELADO, os estados de fatura incluem PENDENTE, PAGO, VENCIDO, ATRASADO e CANCELADO, os tipos de pagamento incluem PIX, BOLETO, TRANSFERENCIA e CARTAO.

## Ambiente de demonstração

Os registros são mantidos em memória. Reinicializações do serviço recriam os dados de demonstração e descartam alterações, contas e chaves de cliente emitidas anteriormente. O histórico de idempotência e os saldos de requisições também são reiniciados. Considere esse comportamento ao testar integrações e repetir operações após uma interrupção.

## Relacionamentos e isolamento

A conta representa a organização que utiliza a API. Cada empresa pertence a uma conta, enquanto contatos, contratos e meios de pagamento estão vinculados à empresa. As faturas ficam vinculadas ao contrato. Essa estrutura mantém os dados de cada organização separados nas consultas e alterações. Cada meio de pagamento guarda sua referência operacional em `details`.
