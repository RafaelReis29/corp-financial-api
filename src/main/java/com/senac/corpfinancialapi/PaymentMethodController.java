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
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
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
@Tag(name = "payment-methods", description = "Gestão de meios de pagamento das empresas. Cada registro pertence a uma empresa pelo campo companyId. O campo details guarda a referência operacional, por exemplo agência e conta. Valores de type usados: PIX, BOLETO, TRANSFERENCIA e CARTAO.")
public class PaymentMethodController {

    private final PaymentMethodRepository repository;
    private final IdempotentPosts posts;
    private final AccountScope scope;
    private final PaymentMethodModelAssembler assembler;
    private final PagedResourcesAssembler<PaymentMethod> pagedResourcesAssembler;

    public PaymentMethodController(PaymentMethodRepository repository, PaymentMethodModelAssembler assembler, PagedResourcesAssembler<PaymentMethod> pagedResourcesAssembler, IdempotentPosts posts, AccountScope scope) {
        this.repository = repository;
        this.posts = posts;
        this.scope = scope;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Listar meios de pagamento", description = "Retorna lista paginada de meios de pagamento com links de navegação. Use page, size e sort para navegar.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @ApiResponse(responseCode = "400", description = "Paginação ou campo de ordenação inválido")
    @GetMapping("/payment-methods")
    public ResponseEntity<PagedModel<EntityModel<PaymentMethod>>> getAllPaymentMethods(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<PaymentMethod> paymentMethodPage = repository.findAllByCompany_Account_Id(scope.accountId(), scope.validateSort(pageable, PaymentMethod.class));
        PagedModel<EntityModel<PaymentMethod>> pagedModel = pagedResourcesAssembler.toModel(paymentMethodPage, assembler);
        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Criar meio de pagamento", description = "Cria um meio de pagamento vinculado a uma empresa. Envie companyId com o id de uma empresa associada à sua conta, type, provider, label, details, isDefault e active. O campo label é o apelido interno da conta. O campo details guarda apenas referência operacional, sem dados sensíveis completos.")
    @ApiResponse(responseCode = "201", description = "Meio de pagamento criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Corpo da requisição inválido")
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
                                    @ExampleObject(name = "Boleto", value = "{ \"companyId\": 1, \"type\": \"BOLETO\", \"provider\": \"Banco Wayne\", \"label\": \"Cobranca\", \"details\": \"Código de cobrança 09\", \"isDefault\": false, \"active\": true }")
                            }))
            @RequestBody @Valid PaymentMethod newPaymentMethod) {
        return posts.execute(newPaymentMethod, () -> createPaymentMethod(newPaymentMethod));
    }

    private ResponseEntity<EntityModel<PaymentMethod>> createPaymentMethod(PaymentMethod newPaymentMethod) {
        scope.requireCompany(newPaymentMethod.getCompanyId());
        PaymentMethod saved = repository.save(newPaymentMethod);
        EntityModel<PaymentMethod> entityModel = assembler.toModel(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(entityModel);
    }

    @Operation(summary = "Buscar meio de pagamento por id", description = "Retorna um meio de pagamento com links de navegação. Use o id retornado na listagem ou na criação.")
    @ApiResponse(responseCode = "200", description = "Meio de pagamento encontrado")
    @ApiResponse(responseCode = "400", description = "Identificador inválido")
    @ApiResponse(responseCode = "404", description = "Meio de pagamento não encontrado")
    @GetMapping("/payment-methods/{id}")
    public EntityModel<PaymentMethod> getPaymentMethodById(@Parameter(description = "Id do meio de pagamento") @PathVariable @Positive Long id) {
        PaymentMethod paymentMethod = repository.findByIdAndCompany_Account_Id(id, scope.accountId()).orElseThrow(() -> new PaymentMethodNotFoundException(id));
        return assembler.toModel(paymentMethod);
    }

    @Operation(summary = "Atualizar ou criar meio de pagamento", description = "Atualiza o registro com o id informado. Se o ID não estiver associado à conta, cria um registro para essa conta com ID gerado pelo servidor.")
    @ApiResponse(responseCode = "200", description = "Meio de pagamento atualizado com sucesso")
    @ApiResponse(responseCode = "400", description = "Identificador ou corpo da requisição inválido")
    @ApiResponse(responseCode = "201", description = "Registro criado com id gerado pelo servidor")
    @PutMapping("/payment-methods/{id}")
    public ResponseEntity<EntityModel<PaymentMethod>> updatePaymentMethod(@Parameter(description = "Id do meio de pagamento") @PathVariable @Positive Long id, @RequestBody @Valid PaymentMethod newPaymentMethod) {
        PaymentMethod paymentMethod = repository.findByIdAndCompany_Account_Id(id, scope.accountId()).orElse(null);
        if (paymentMethod == null) {
            return createPaymentMethod(newPaymentMethod);
        }
        scope.requireCompany(newPaymentMethod.getCompanyId());
        paymentMethod.setCompanyId(newPaymentMethod.getCompanyId());
        paymentMethod.setType(newPaymentMethod.getType());
        paymentMethod.setProvider(newPaymentMethod.getProvider());
        paymentMethod.setLabel(newPaymentMethod.getLabel());
        paymentMethod.setDetails(newPaymentMethod.getDetails());
        paymentMethod.setIsDefault(newPaymentMethod.getIsDefault());
        paymentMethod.setActive(newPaymentMethod.getActive());
        PaymentMethod saved = repository.save(paymentMethod);
        return ResponseEntity.ok(assembler.toModel(saved));
    }

    @Operation(summary = "Excluir meio de pagamento", description = "Exclui o meio de pagamento com o id informado. A resposta não possui conteúdo no corpo.")
    @ApiResponse(responseCode = "204", description = "Meio de pagamento excluído com sucesso", content = @Content)
    @ApiResponse(responseCode = "400", description = "Identificador inválido")
    @ApiResponse(responseCode = "404", description = "Meio de pagamento não encontrado", content = @Content)
    @DeleteMapping("/payment-methods/{id}")
    public ResponseEntity<Void> deletePaymentMethod(@Parameter(description = "Id do meio de pagamento") @PathVariable @Positive Long id) {
        var paymentMethod = repository.findByIdAndCompany_Account_Id(id, scope.accountId());
        if (paymentMethod.isEmpty())
            return ResponseEntity.notFound().build();
        repository.delete(paymentMethod.get());
        return ResponseEntity.noContent().build();
    }
}
