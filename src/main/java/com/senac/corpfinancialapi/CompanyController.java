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
@Tag(name = "companies", description = "Gestão de empresas clientes da operação B2B. A base inicial contém empresas da cultura pop para facilitar os testes.")
public class CompanyController {

    private final CompanyRepository repository;
    private final CompanyModelAssembler assembler;
    private final PagedResourcesAssembler<Company> pagedResourcesAssembler;

    public CompanyController(CompanyRepository repository, CompanyModelAssembler assembler, PagedResourcesAssembler<Company> pagedResourcesAssembler) {
        this.repository = repository;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Listar empresas", description = "Retorna lista paginada de empresas com links de navegação. Use os parâmetros page, size e sort para navegar. Exemplo: /companies?page=0&size=10&sort=id,asc")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping("/companies")
    public ResponseEntity<PagedModel<EntityModel<Company>>> getAllCompanies(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Company> companyPage = repository.findAll(pageable);
        PagedModel<EntityModel<Company>> pagedModel = pagedResourcesAssembler.toModel(companyPage, assembler);
        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Criar empresa", description = "Cria uma empresa. Envie tradeName, legalName, docNumber, sector, email, phone e active. Os campos createdAt e updatedAt são preenchidos pelo servidor quando ausentes. O campo docNumber identifica o documento da empresa e deve ser único por empresa.")
    @ApiResponse(responseCode = "201", description = "Empresa criada com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos. Confira os campos obrigatórios")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true)
    @PostMapping("/companies")
    public ResponseEntity<EntityModel<Company>> newCompany(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados da empresa",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Company.class),
                            examples = @ExampleObject(value = "{ \"tradeName\": \"Stark\", \"legalName\": \"Stark Industries SA\", \"docNumber\": \"12345678000190\", \"sector\": \"Industria\", \"email\": \"contato@stark.com\", \"phone\": \"11999990000\", \"active\": true }"))
            )
            @RequestBody @Valid Company newCompany) {
        if (newCompany.getCreatedAt() == null) {
            newCompany.setCreatedAt(LocalDateTime.now());
        }
        newCompany.setUpdatedAt(LocalDateTime.now());
        Company saved = repository.save(newCompany);
        EntityModel<Company> entityModel = assembler.toModel(saved);
        return ResponseEntity.created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(entityModel);
    }

    @Operation(summary = "Buscar empresa por id", description = "Retorna uma empresa com links de navegação. Use o id retornado na listagem ou na criação.")
    @ApiResponse(responseCode = "200", description = "Empresa encontrada")
    @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    @GetMapping("/companies/{id}")
    public EntityModel<Company> getCompanyById(@Parameter(description = "Id da empresa") @PathVariable Long id) {
        Company company = repository.findById(id).orElseThrow(() -> new CompanyNotFoundException(id));
        return assembler.toModel(company);
    }

    @Operation(summary = "Atualizar ou criar empresa", description = "Atualiza todos os campos da empresa com o id informado. Se o id não existir, cria uma empresa nova com id gerado pelo servidor. O campo createdAt original é mantido e o campo updatedAt é atualizado pelo servidor.")
    @ApiResponse(responseCode = "200", description = "Empresa atualizada com sucesso")
    @ApiResponse(responseCode = "201", description = "Empresa criada com id gerado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos. Confira os campos obrigatórios")
    @PutMapping("/companies/{id}")
    public ResponseEntity<EntityModel<Company>> updateOrCreateCompany(@Parameter(description = "Id da empresa") @PathVariable Long id, @RequestBody @Valid Company newCompany) {
        return repository.findById(id)
                .map(company -> {
                    company.setTradeName(newCompany.getTradeName());
                    company.setLegalName(newCompany.getLegalName());
                    company.setDocNumber(newCompany.getDocNumber());
                    company.setSector(newCompany.getSector());
                    company.setEmail(newCompany.getEmail());
                    company.setPhone(newCompany.getPhone());
                    company.setActive(newCompany.getActive());
                    company.setUpdatedAt(LocalDateTime.now());
                    Company saved = repository.save(company);
                    return ResponseEntity.ok(assembler.toModel(saved));
                })
                .orElseGet(() -> {
                    if (newCompany.getCreatedAt() == null) {
                        newCompany.setCreatedAt(LocalDateTime.now());
                    }
                    newCompany.setUpdatedAt(LocalDateTime.now());
                    Company saved = repository.save(newCompany);
                    EntityModel<Company> entityModel = assembler.toModel(saved);
                    return ResponseEntity.created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(entityModel);
                });
    }

    @Operation(summary = "Excluir empresa", description = "Exclui a empresa com o id informado. A resposta não possui conteúdo no corpo.")
    @ApiResponse(responseCode = "204", description = "Empresa excluída com sucesso")
    @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    @DeleteMapping("/companies/{id}")
    public ResponseEntity<?> deleteCompany(@Parameter(description = "Id da empresa") @PathVariable Long id) {
        var company = repository.findById(id);
        if (company.isEmpty())
            return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
