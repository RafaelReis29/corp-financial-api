package com.senac.corpfinancialapi;

import java.time.LocalDateTime;

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
@Tag(name = "contracts", description = "Gestão de contratos entre empresas. Cada contrato pertence a uma empresa pelo campo companyId. O campo status indica a fase do contrato. Valores usados: ATIVO, RASCUNHO, SUSPENSO, ENCERRADO e CANCELADO.")
public class ContractController {

    private final ContractRepository repository;
    private final ContractModelAssembler assembler;
    private final PagedResourcesAssembler<Contract> pagedResourcesAssembler;

    public ContractController(ContractRepository repository, ContractModelAssembler assembler, PagedResourcesAssembler<Contract> pagedResourcesAssembler) {
        this.repository = repository;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Listar contratos", description = "Retorna lista paginada de contratos com links de navegação. Use page, size e sort para navegar.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping("/contracts")
    public ResponseEntity<PagedModel<EntityModel<Contract>>> getAllContracts(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Contract> contractPage = repository.findAll(pageable);
        PagedModel<EntityModel<Contract>> pagedModel = pagedResourcesAssembler.toModel(contractPage, assembler);
        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Criar contrato", description = "Cria um contrato vinculado a uma empresa. Envie companyId com o id de uma empresa existente, title, description, totalValue, currency, startDate, endDate e status. Use currency com o código da moeda, por exemplo BRL. Os campos createdAt e updatedAt são preenchidos pelo servidor quando ausentes.")
    @ApiResponse(responseCode = "201", description = "Contrato criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos. Confira os campos obrigatórios")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true)
    @PostMapping("/contracts")
    public ResponseEntity<EntityModel<Contract>> newContract(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados do contrato",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Contract.class),
                            examples = @ExampleObject(value = "{ \"companyId\": 1, \"title\": \"Fornecimento 2026\", \"description\": \"Fornecimento anual\", \"totalValue\": 150000.00, \"currency\": \"BRL\", \"startDate\": \"2026-01-01\", \"endDate\": \"2026-12-31\", \"status\": \"ATIVO\" }"))
            )
            @RequestBody @Valid Contract newContract) {
        if (newContract.getCreatedAt() == null) {
            newContract.setCreatedAt(LocalDateTime.now());
        }
        newContract.setUpdatedAt(LocalDateTime.now());
        Contract saved = repository.save(newContract);
        EntityModel<Contract> entityModel = assembler.toModel(saved);
        return ResponseEntity.created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(entityModel);
    }

    @Operation(summary = "Buscar contrato por id", description = "Retorna um contrato com links de navegação. Use o id retornado na listagem ou na criação.")
    @ApiResponse(responseCode = "200", description = "Contrato encontrado")
    @ApiResponse(responseCode = "404", description = "Contrato não encontrado")
    @GetMapping("/contracts/{id}")
    public EntityModel<Contract> getContractById(@Parameter(description = "Id do contrato") @PathVariable Long id) {
        Contract contract = repository.findById(id).orElseThrow(() -> new ContractNotFoundException(id));
        return assembler.toModel(contract);
    }

    @Operation(summary = "Atualizar ou criar contrato", description = "Atualiza todos os campos do contrato com o id informado. Se o id não existir, cria um contrato novo com id gerado pelo servidor. O campo createdAt original é mantido e o campo updatedAt é atualizado pelo servidor.")
    @ApiResponse(responseCode = "200", description = "Contrato atualizado com sucesso")
    @ApiResponse(responseCode = "201", description = "Contrato criado com id gerado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos. Confira os campos obrigatórios")
    @PutMapping("/contracts/{id}")
    public ResponseEntity<EntityModel<Contract>> updateOrCreateContract(@Parameter(description = "Id do contrato") @PathVariable Long id, @RequestBody @Valid Contract newContract) {
        return repository.findById(id)
                .map(contract -> {
                    contract.setCompanyId(newContract.getCompanyId());
                    contract.setTitle(newContract.getTitle());
                    contract.setDescription(newContract.getDescription());
                    contract.setTotalValue(newContract.getTotalValue());
                    contract.setCurrency(newContract.getCurrency());
                    contract.setStartDate(newContract.getStartDate());
                    contract.setEndDate(newContract.getEndDate());
                    contract.setStatus(newContract.getStatus());
                    contract.setUpdatedAt(LocalDateTime.now());
                    Contract saved = repository.save(contract);
                    return ResponseEntity.ok(assembler.toModel(saved));
                })
                .orElseGet(() -> {
                    if (newContract.getCreatedAt() == null) {
                        newContract.setCreatedAt(LocalDateTime.now());
                    }
                    newContract.setUpdatedAt(LocalDateTime.now());
                    Contract saved = repository.save(newContract);
                    EntityModel<Contract> entityModel = assembler.toModel(saved);
                    return ResponseEntity.created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(entityModel);
                });
    }

    @Operation(summary = "Excluir contrato", description = "Exclui o contrato com o id informado. A resposta não possui conteúdo no corpo.")
    @ApiResponse(responseCode = "204", description = "Contrato excluído com sucesso")
    @ApiResponse(responseCode = "404", description = "Contrato não encontrado")
    @DeleteMapping("/contracts/{id}")
    public ResponseEntity<?> deleteContract(@Parameter(description = "Id do contrato") @PathVariable Long id) {
        var contract = repository.findById(id);
        if (contract.isEmpty())
            return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
