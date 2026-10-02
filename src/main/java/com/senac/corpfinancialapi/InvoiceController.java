package com.senac.corpfinancialapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "invoices", description = "Gestão de faturas a pagar dos contratos. Cada fatura pertence a um contrato pelo campo contractId e pode indicar um meio de pagamento pelo campo paymentMethodId. O campo status indica a fase da fatura. Valores usados: PENDENTE, PAGO, VENCIDO e CANCELADO.")
public class InvoiceController {

    private final InvoiceRepository repository;
    private final InvoiceModelAssembler assembler;
    private final PagedResourcesAssembler<Invoice> pagedResourcesAssembler;

    public InvoiceController(InvoiceRepository repository, InvoiceModelAssembler assembler, PagedResourcesAssembler<Invoice> pagedResourcesAssembler) {
        this.repository = repository;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Listar faturas", description = "Retorna lista paginada de faturas com links de navegação. Use page, size e sort para navegar.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping("/invoices")
    public ResponseEntity<PagedModel<EntityModel<Invoice>>> getAllInvoices(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Invoice> invoicePage = repository.findAll(pageable);
        PagedModel<EntityModel<Invoice>> pagedModel = pagedResourcesAssembler.toModel(invoicePage, assembler);
        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Criar fatura", description = "Cria uma fatura vinculada a um contrato. Envie contractId com o id de um contrato existente, description, amount, dueDate, status e paymentMethodId quando houver. Para uma fatura em aberto, use status PENDENTE e deixe paidAt ausente. Ao dar baixa, envie status PAGO com paidAt preenchido.")
    @ApiResponse(responseCode = "201", description = "Fatura criada com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos. Confira os campos obrigatórios")
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
        Invoice saved = repository.save(newInvoice);
        EntityModel<Invoice> entityModel = assembler.toModel(saved);
        return ResponseEntity.created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(entityModel);
    }

    @Operation(summary = "Buscar fatura por id", description = "Retorna uma fatura com links de navegação. Use o id retornado na listagem ou na criação.")
    @ApiResponse(responseCode = "200", description = "Fatura encontrada")
    @ApiResponse(responseCode = "404", description = "Fatura não encontrada")
    @GetMapping("/invoices/{id}")
    public EntityModel<Invoice> getInvoiceById(@Parameter(description = "Id da fatura") @PathVariable Long id) {
        Invoice invoice = repository.findById(id).orElseThrow(() -> new InvoiceNotFoundException(id));
        return assembler.toModel(invoice);
    }

    @Operation(summary = "Atualizar ou criar fatura", description = "Atualiza todos os campos da fatura com o id informado. Se o id não existir, cria uma fatura nova com id gerado pelo servidor. Para dar baixa, envie status PAGO com paidAt preenchido.")
    @ApiResponse(responseCode = "200", description = "Fatura atualizada com sucesso")
    @ApiResponse(responseCode = "201", description = "Fatura criada com id gerado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos. Confira os campos obrigatórios")
    @PutMapping("/invoices/{id}")
    public ResponseEntity<EntityModel<Invoice>> updateOrCreateInvoice(@Parameter(description = "Id da fatura") @PathVariable Long id, @RequestBody @Valid Invoice newInvoice) {
        return repository.findById(id)
                .map(invoice -> {
                    invoice.setContractId(newInvoice.getContractId());
                    invoice.setDescription(newInvoice.getDescription());
                    invoice.setAmount(newInvoice.getAmount());
                    invoice.setDueDate(newInvoice.getDueDate());
                    invoice.setPaidAt(newInvoice.getPaidAt());
                    invoice.setStatus(newInvoice.getStatus());
                    invoice.setPaymentMethodId(newInvoice.getPaymentMethodId());
                    Invoice saved = repository.save(invoice);
                    return ResponseEntity.ok(assembler.toModel(saved));
                })
                .orElseGet(() -> {
                    Invoice saved = repository.save(newInvoice);
                    EntityModel<Invoice> entityModel = assembler.toModel(saved);
                    return ResponseEntity.created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(entityModel);
                });
    }

    @Operation(summary = "Excluir fatura", description = "Exclui a fatura com o id informado. A resposta não possui conteúdo no corpo.")
    @ApiResponse(responseCode = "204", description = "Fatura excluída com sucesso")
    @ApiResponse(responseCode = "404", description = "Fatura não encontrada")
    @DeleteMapping("/invoices/{id}")
    public ResponseEntity<?> deleteInvoice(@Parameter(description = "Id da fatura") @PathVariable Long id) {
        var invoice = repository.findById(id);
        if (invoice.isEmpty())
            return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
