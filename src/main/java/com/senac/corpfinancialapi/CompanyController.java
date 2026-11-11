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
@Tag(name = "companies", description = "Cadastro das empresas clientes associadas à conta autenticada.")
public class CompanyController {

    private final CompanyRepository repository;
    private final IdempotentPosts posts;
    private final AccountScope scope;
    private final CompanyModelAssembler assembler;
    private final PagedResourcesAssembler<Company> pagedResourcesAssembler;

    public CompanyController(CompanyRepository repository, CompanyModelAssembler assembler, PagedResourcesAssembler<Company> pagedResourcesAssembler, IdempotentPosts posts, AccountScope scope) {
        this.repository = repository;
        this.posts = posts;
        this.scope = scope;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Listar empresas", description = "Retorna lista paginada de empresas com links de navegação. Use os parâmetros page, size e sort para navegar. Exemplo: /companies?page=0&size=10&sort=id,asc")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @ApiResponse(responseCode = "400", description = "Paginação ou campo de ordenação inválido")
    @GetMapping("/companies")
    public ResponseEntity<PagedModel<EntityModel<Company>>> getAllCompanies(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Company> companyPage = repository.findAllByAccount_Id(scope.accountId(), pageable);
        PagedModel<EntityModel<Company>> pagedModel = pagedResourcesAssembler.toModel(companyPage, assembler);
        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Criar empresa", description = "Cria uma empresa. Envie tradeName, legalName, docNumber, sector, email, phone e active. Os campos createdAt e updatedAt são preenchidos pelo servidor quando ausentes. O campo docNumber identifica o documento da empresa e deve ser único por conta.")
    @ApiResponse(responseCode = "201", description = "Empresa criada com sucesso")
    @ApiResponse(responseCode = "400", description = "Corpo da requisição inválido")
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
        return posts.execute(newCompany, () -> createCompany(newCompany));
    }

    private ResponseEntity<EntityModel<Company>> createCompany(Company newCompany) {
        newCompany.assignTo(scope.account());
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
    @ApiResponse(responseCode = "400", description = "Identificador inválido")
    @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    @GetMapping(path = "/companies/{id}", version = "1+")
    public EntityModel<Company> getCompanyById(@Parameter(description = "Id da empresa") @PathVariable @Positive Long id) {
        Company company = repository.findByIdAndAccount_Id(id, scope.accountId()).orElseThrow(() -> new CompanyNotFoundException(id));
        return assembler.toModel(company);
    }

    @Operation(summary = "Buscar perfil da empresa por id", description = "Com X-API-Version 2.0.0, os dados da empresa ficam no campo company.")
    @ApiResponse(responseCode = "200", description = "Perfil encontrado")
    @ApiResponse(responseCode = "400", description = "Identificador ou versão inválida")
    @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    @GetMapping(path = "/companies/{id}", version = "2")
    public EntityModel<CompanyProfile> getCompanyProfile(@PathVariable @Positive Long id) {
        EntityModel<Company> company = getCompanyById(id);
        return EntityModel.of(new CompanyProfile(company.getContent()), company.getLinks());
    }

    public record CompanyProfile(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Company company) {
    }

    @Operation(summary = "Atualizar ou criar empresa", description = "Atualiza o registro com o id informado. Se o ID não estiver associado à conta, cria um registro para essa conta com ID gerado pelo servidor. O campo createdAt original é mantido e o campo updatedAt é atualizado pelo servidor.")
    @ApiResponse(responseCode = "200", description = "Empresa atualizada com sucesso")
    @ApiResponse(responseCode = "400", description = "Identificador ou corpo da requisição inválido")
    @ApiResponse(responseCode = "201", description = "Registro criado com id gerado pelo servidor")
    @PutMapping("/companies/{id}")
    public ResponseEntity<EntityModel<Company>> updateCompany(@Parameter(description = "Id da empresa") @PathVariable @Positive Long id, @RequestBody @Valid Company newCompany) {
        Company company = repository.findByIdAndAccount_Id(id, scope.accountId()).orElse(null);
        if (company == null) {
            return createCompany(newCompany);
        }
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
    }

    @Operation(summary = "Excluir empresa", description = "Exclusivo do Admin. Informe a conta proprietária no cabeçalho X-Account-Id. A empresa precisa estar sem registros dependentes.")
    @ApiResponse(responseCode = "204", description = "Empresa excluída com sucesso", content = @Content)
    @ApiResponse(responseCode = "400", description = "Identificador inválido")
    @ApiResponse(responseCode = "404", description = "Empresa não encontrada", content = @Content)
    @DeleteMapping("/companies/{id}")
    public ResponseEntity<Void> deleteCompany(@Parameter(description = "Id da empresa") @PathVariable @Positive Long id) {
        var company = repository.findByIdAndAccount_Id(id, scope.accountId());
        if (company.isEmpty())
            return ResponseEntity.notFound().build();
        repository.delete(company.get());
        return ResponseEntity.noContent().build();
    }
}
