# Corp Financial API

Este projeto é uma API de sistema financeiro corporativo, com operação entre empresas, contatos, contratos, meios de pagamento e faturas, em que cada fatura pertence a um contrato, cada contrato pertence a uma empresa, cada contato pertence a uma empresa, o que mantém rastreabilidade entre cobrança, contrato e cliente.

Os dados de exemplo usam empresas e personagens da cultura pop, como Stark, Wayne, Oscorp, Umbrella, InGen, Vought e outras, com status em português, datas e valores em BRL, o que deixa a base inicial reconhecível e fácil de explorar.

## Visão geral

O fluxo principal começa com o cadastro de empresas, continua com contatos e meios de pagamento vinculados pelo campo companyId, avança para contratos vinculados pelo mesmo campo, e termina com faturas vinculadas pelo campo contractId, com indicação opcional de meio de pagamento pelo campo paymentMethodId.

Cada resposta inclui links de navegação, de modo que listagens apontam para detalhes, detalhes apontam para coleções, o que reduz a necessidade de montar endereços manualmente.

## Recursos disponíveis

1. Empresas, com cadastro, listagem paginada, busca por identificador, atualização com criação condicional e exclusão.
2. Contatos, com vínculo obrigatório a empresa por companyId, além de controle de contato principal por isPrimary.
3. Contratos, com valor total em totalValue, moeda em currency, vigência em startDate e endDate, fase em status.
4. Meios de pagamento, com tipo em type, provedor em provider, apelido em label, referência operacional em details, além de isDefault.
5. Faturas, com descrição em description, valor em amount, vencimento em dueDate, baixa em paidAt, fase em status.

## Execução

Com Java e Maven instalados, basta abrir o projeto na IDE e executar a classe principal, pois o banco em memória inicia junto com a aplicação, sem dependência externa, e a documentação interativa fica disponível para exploração dos recursos.

Para consultas paginadas, use página, tamanho e ordenação, por exemplo com page, size e sort. Para atualização, envie o corpo completo. Para baixa de fatura, envie status PAGO com paidAt preenchido, enquanto fatura em aberto usa PENDENTE com paidAt ausente.

## Respostas

Os códigos usados são 200 para consulta e atualização com sucesso, 201 para criação com sucesso com Location no cabeçalho, 204 para exclusão com sucesso sem corpo, 400 para corpo inválido ou campo obrigatório ausente, 404 para identificador inexistente.

## Carga inicial

Na inicialização, o sistema cria 22 empresas, 47 contatos distribuídos entre elas, 6 meios de pagamento dos tipos PIX, TRANSFERENCIA, BOLETO e CARTAO, 6 contratos ativos com valores, vigência e moeda BRL, e 15 faturas nos estados PAGO, PENDENTE e ATRASADO, o que permite explorar listagens, buscas, vínculos e baixas sem cadastro prévio.

Os estados de contrato incluem ATIVO, RASCUNHO, SUSPENSO, ENCERRADO e CANCELADO, os estados de fatura incluem PENDENTE, PAGO, VENCIDO, ATRASADO e CANCELADO, os tipos de pagamento incluem PIX, BOLETO, TRANSFERENCIA e CARTAO.

## Organização do código

O código repete o mesmo padrão por recurso, com entidade para os dados, repositório para persistência, montador para links de navegação, controlador para os endpoints, além de exceção específica com tratamento para 404 e classe de carga inicial, o que mantém previsibilidade entre módulos.

## Decisões de modelagem

O documento da empresa fica em docNumber como identificador de negócio, contrato usa somente status sem campo active separado para evitar duplicidade de estado, fatura chega até a empresa pelo contrato sem guardar empresa direta para evitar divergência, meio de pagamento guarda somente referência operacional em details sem dado sensível completo, em linha com sistema legado que mantém o dado real no provedor.
