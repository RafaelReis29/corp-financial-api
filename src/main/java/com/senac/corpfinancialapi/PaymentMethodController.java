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
@Tag(name = "payment-methods", description = "Gestão de meios de pagamento das empresas. Cada registro pertence a uma empresa pelo campo companyId. O campo details guarda a referência operacional, por exemplo agência e conta. Valores de type usados: PIX, BOLETO, TRANSFERENCIA e CARTAO.")
public class PaymentMethodController {

    private final PaymentMethodRepository repository;
    private final PaymentMethodModelAssembler assembler;
    private final PagedResourcesAssembler<PaymentMethod> pagedResourcesAssembler;

    public PaymentMethodController(PaymentMethodRepository repository, PaymentMethodModelAssembler assembler, PagedResourcesAssembler<PaymentMethod> pagedResourcesAssembler) {
        this.repository = repository;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Listar meios de pagamento", description = "Retorna lista paginada de meios de pagamento com links de navegação. Use page, size e sort para navegar.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping("/payment-methods")
    public ResponseEntity<PagedModel<EntityModel<PaymentMethod>>> getAllPaymentMethods(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<PaymentMethod> paymentMethodPage = repository.findAll(pageable);
        PagedModel<EntityModel<PaymentMethod>> pagedModel = pagedResourcesAssembler.toModel(paymentMethodPage, assembler);
        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Criar meio de pagamento", description = "Cria um meio de pagamento vinculado a uma empresa. Envie companyId com o id de uma empresa existente, type, provider, label, details, isDefault e active. O campo label é o apelido interno da conta. O campo details guarda apenas referência operacional, sem dados sensíveis completos.")
    @ApiResponse(responseCode = "201", description = "Meio de pagamento criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos. Confira os campos obrigatórios")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true)
    @PostMapping("/payment-methods")
    public ResponseEntity<EntityModel<PaymentMethod>> newPaymentMethod(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados do meio de pagamento",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaymentMethod.class),
                            examples = {
                                    @ExampleObject(name = "Pix", value = "{ \"companyId\": 1, \"type\": \"PIX\", \"provider\": \"Banco Stark\", \"label\": \"Conta Principal\", \"details\": \"Ag 0001 Cc 12345-6\", \"isDefault\": true, \"active\": true }"),
                                    @ExampleObject(name = "Boleto", value = "{ \"companyId\": 1, \"type\": \"BOLETO\", \"provider\": \"Banco Wayne\", \"label\": \"Cobranca\", \"details\": \"Carteira 09\", \"isDefault\": false, \"active\": true }")
                            }))
            @RequestBody @Valid PaymentMethod newPaymentMethod) {
        PaymentMethod saved = repository.save(newPaymentMethod);
        EntityModel<PaymentMethod> entityModel = assembler.toModel(saved);
        return ResponseEntity.created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(entityModel);
    }

    @Operation(summary = "Buscar meio de pagamento por id", description = "Retorna um meio de pagamento com links de navegação. Use o id retornado na listagem ou na criação.")
    @ApiResponse(responseCode = "200", description = "Meio de pagamento encontrado")
    @ApiResponse(responseCode = "404", description = "Meio de pagamento não encontrado")
    @GetMapping("/payment-methods/{id}")
    public EntityModel<PaymentMethod> getPaymentMethodById(@Parameter(description = "Id do meio de pagamento") @PathVariable Long id) {
        PaymentMethod paymentMethod = repository.findById(id).orElseThrow(() -> new PaymentMethodNotFoundException(id));
        return assembler.toModel(paymentMethod);
    }

    @Operation(summary = "Atualizar ou criar meio de pagamento", description = "Atualiza todos os campos do meio de pagamento com o id informado. Se o id não existir, cria um registro novo com id gerado pelo servidor.")
    @ApiResponse(responseCode = "200", description = "Meio de pagamento atualizado com sucesso")
    @ApiResponse(responseCode = "201", description = "Meio de pagamento criado com id gerado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos. Confira os campos obrigatórios")
    @PutMapping("/payment-methods/{id}")
    public ResponseEntity<EntityModel<PaymentMethod>> updateOrCreatePaymentMethod(@Parameter(description = "Id do meio de pagamento") @PathVariable Long id, @RequestBody @Valid PaymentMethod newPaymentMethod) {
        return repository.findById(id)
                .map(paymentMethod -> {
                    paymentMethod.setCompanyId(newPaymentMethod.getCompanyId());
                    paymentMethod.setType(newPaymentMethod.getType());
                    paymentMethod.setProvider(newPaymentMethod.getProvider());
                    paymentMethod.setLabel(newPaymentMethod.getLabel());
                    paymentMethod.setDetails(newPaymentMethod.getDetails());
                    paymentMethod.setIsDefault(newPaymentMethod.getIsDefault());
                    paymentMethod.setActive(newPaymentMethod.getActive());
                    PaymentMethod saved = repository.save(paymentMethod);
                    return ResponseEntity.ok(assembler.toModel(saved));
                })
                .orElseGet(() -> {
                    PaymentMethod saved = repository.save(newPaymentMethod);
                    EntityModel<PaymentMethod> entityModel = assembler.toModel(saved);
                    return ResponseEntity.created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(entityModel);
                });
    }

    @Operation(summary = "Excluir meio de pagamento", description = "Exclui o meio de pagamento com o id informado. A resposta não possui conteúdo no corpo.")
    @ApiResponse(responseCode = "204", description = "Meio de pagamento excluído com sucesso")
    @ApiResponse(responseCode = "404", description = "Meio de pagamento não encontrado")
    @DeleteMapping("/payment-methods/{id}")
    public ResponseEntity<?> deletePaymentMethod(@Parameter(description = "Id do meio de pagamento") @PathVariable Long id) {
        var paymentMethod = repository.findById(id);
        if (paymentMethod.isEmpty())
            return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
