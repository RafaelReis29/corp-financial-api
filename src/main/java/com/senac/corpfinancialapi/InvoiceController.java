package com.senac.corpfinancialapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(version = "1+")
@Tag(name = "invoices", description = "Gestão de faturas a pagar dos contratos. Cada fatura pertence a um contrato pelo campo contractId e pode indicar um meio de pagamento pelo campo paymentMethodId. O campo status indica a fase da fatura. Valores usados: PENDENTE, PAGO, VENCIDO e CANCELADO.")
public class InvoiceController {

    private final InvoiceRepository repository;
    private final IdempotentPosts posts;
    private final AccountScope scope;
    private final InvoiceModelAssembler assembler;
    private final PagedResourcesAssembler<Invoice> pagedResourcesAssembler;

    public InvoiceController(InvoiceRepository repository, InvoiceModelAssembler assembler, PagedResourcesAssembler<Invoice> pagedResourcesAssembler, IdempotentPosts posts, AccountScope scope) {
        this.repository = repository;
        this.posts = posts;
        this.scope = scope;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Listar faturas", description = "Retorna lista paginada de faturas com links de navegação. Use page, size e sort para navegar.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @ApiResponse(responseCode = "400", description = "Paginação ou campo de ordenação inválido")
    @GetMapping("/invoices")
    public ResponseEntity<PagedModel<EntityModel<Invoice>>> getAllInvoices(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Invoice> invoicePage = repository.findAllByContract_Company_Account_Id(scope.accountId(), scope.validateSort(pageable, Invoice.class));
        PagedModel<EntityModel<Invoice>> pagedModel = pagedResourcesAssembler.toModel(invoicePage, assembler);
        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Criar fatura", description = "Cria uma fatura vinculada a um contrato. Envie contractId com o id de um contrato associado à sua conta, description, amount, dueDate, status e paymentMethodId de um meio de pagamento da mesma conta, quando houver. Para uma fatura em aberto, use status PENDENTE e deixe paidAt ausente. Ao dar baixa, envie status PAGO com paidAt preenchido.")
    @ApiResponse(responseCode = "201", description = "Fatura criada com sucesso")
    @ApiResponse(responseCode = "400", description = "Corpo da requisição inválido")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true)
    @PostMapping("/invoices")
    public ResponseEntity<EntityModel<Invoice>> newInvoice(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados da fatura",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Invoice.class),
                            examples = {
                                    @ExampleObject(name = "Fatura pendente", value = "{ \"contractId\": 1, \"description\": \"Parcela 01/12\", \"amount\": 12500.00, \"dueDate\": \"2026-11-10\", \"status\": \"PENDENTE\", \"paymentMethodId\": 1 }"),
                                    @ExampleObject(name = "Fatura paga", value = "{ \"contractId\": 1, \"description\": \"Parcela 01/12\", \"amount\": 12500.00, \"dueDate\": \"2026-11-10\", \"paidAt\": \"2026-11-09T10:00:00\", \"status\": \"PAGO\", \"paymentMethodId\": 1 }")
                            }))
            @RequestBody @Valid Invoice newInvoice) {
        return posts.execute(newInvoice, () -> createInvoice(newInvoice));
    }

    private ResponseEntity<EntityModel<Invoice>> createInvoice(Invoice newInvoice) {
        scope.requireInvoiceReferences(newInvoice.getContractId(), newInvoice.getPaymentMethodId());
        Invoice saved = repository.save(newInvoice);
        EntityModel<Invoice> entityModel = assembler.toModel(saved);
        return ResponseEntity.created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(entityModel);
    }

    @Operation(summary = "Buscar fatura por id", description = "Retorna uma fatura com links de navegação. Use o id retornado na listagem ou na criação.")
    @ApiResponse(responseCode = "200", description = "Fatura encontrada")
    @ApiResponse(responseCode = "400", description = "Identificador inválido")
    @ApiResponse(responseCode = "404", description = "Fatura não encontrada")
    @GetMapping("/invoices/{id}")
    public EntityModel<Invoice> getInvoiceById(@Parameter(description = "Id da fatura") @PathVariable @Positive Long id) {
        Invoice invoice = repository.findByIdAndContract_Company_Account_Id(id, scope.accountId()).orElseThrow(() -> new InvoiceNotFoundException(id));
        return assembler.toModel(invoice);
    }

    @Operation(summary = "Atualizar ou criar fatura", description = "Atualiza o registro com o id informado. Se o ID não estiver associado à conta, cria um registro para essa conta com ID gerado pelo servidor. Para dar baixa, envie status PAGO com paidAt preenchido.")
    @ApiResponse(responseCode = "200", description = "Fatura atualizada com sucesso")
    @ApiResponse(responseCode = "400", description = "Identificador ou corpo da requisição inválido")
    @ApiResponse(responseCode = "201", description = "Registro criado com id gerado pelo servidor")
    @PutMapping("/invoices/{id}")
    public ResponseEntity<EntityModel<Invoice>> updateInvoice(@Parameter(description = "Id da fatura") @PathVariable @Positive Long id, @RequestBody @Valid Invoice newInvoice) {
        Invoice invoice = repository.findByIdAndContract_Company_Account_Id(id, scope.accountId()).orElse(null);
        if (invoice == null) {
            return createInvoice(newInvoice);
        }
        scope.requireInvoiceReferences(newInvoice.getContractId(), newInvoice.getPaymentMethodId());
        invoice.setContractId(newInvoice.getContractId());
        invoice.setDescription(newInvoice.getDescription());
        invoice.setAmount(newInvoice.getAmount());
        invoice.setDueDate(newInvoice.getDueDate());
        invoice.setPaidAt(newInvoice.getPaidAt());
        invoice.setStatus(newInvoice.getStatus());
        invoice.setPaymentMethodId(newInvoice.getPaymentMethodId());
        Invoice saved = repository.save(invoice);
        return ResponseEntity.ok(assembler.toModel(saved));
    }

    @Operation(summary = "Excluir fatura", description = "Exclui a fatura com o id informado. A resposta não possui conteúdo no corpo.")
    @ApiResponse(responseCode = "204", description = "Fatura excluída com sucesso", content = @Content)
    @ApiResponse(responseCode = "400", description = "Identificador inválido")
    @ApiResponse(responseCode = "404", description = "Fatura não encontrada", content = @Content)
    @DeleteMapping("/invoices/{id}")
    public ResponseEntity<Void> deleteInvoice(@Parameter(description = "Id da fatura") @PathVariable @Positive Long id) {
        var invoice = repository.findByIdAndContract_Company_Account_Id(id, scope.accountId());
        if (invoice.isEmpty())
            return ResponseEntity.notFound().build();
        repository.delete(invoice.get());
        return ResponseEntity.noContent().build();
    }
}
