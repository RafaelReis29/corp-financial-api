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
@Tag(name = "contacts", description = "Gestão de contatos das empresas. Cada contato pertence a uma empresa pelo campo companyId. A base inicial contém personagens da cultura pop.")
public class ContactController {

    private final ContactRepository repository;
    private final IdempotentPosts posts;
    private final AccountScope scope;
    private final ContactModelAssembler assembler;
    private final PagedResourcesAssembler<Contact> pagedResourcesAssembler;

    public ContactController(ContactRepository repository, ContactModelAssembler assembler, PagedResourcesAssembler<Contact> pagedResourcesAssembler, IdempotentPosts posts, AccountScope scope) {
        this.repository = repository;
        this.posts = posts;
        this.scope = scope;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Listar contatos", description = "Retorna lista paginada de contatos com links de navegação. Use page, size e sort para navegar.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @ApiResponse(responseCode = "400", description = "Paginação ou campo de ordenação inválido")
    @GetMapping("/contacts")
    public ResponseEntity<PagedModel<EntityModel<Contact>>> getAllContacts(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Contact> contactPage = repository.findAllByCompany_Account_Id(scope.accountId(), scope.validateSort(pageable, Contact.class));
        PagedModel<EntityModel<Contact>> pagedModel = pagedResourcesAssembler.toModel(contactPage, assembler);
        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Criar contato", description = "Cria um contato vinculado a uma empresa. Envie companyId com o id de uma empresa associada à sua conta, além de name, role, email, phone, isPrimary e active. Os campos createdAt e updatedAt são preenchidos pelo servidor quando ausentes.")
    @ApiResponse(responseCode = "201", description = "Contato criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Corpo da requisição inválido")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true)
    @PostMapping("/contacts")
    public ResponseEntity<EntityModel<Contact>> newContact(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados do contato",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Contact.class),
                            examples = @ExampleObject(value = "{ \"companyId\": 1, \"name\": \"Tony Stark\", \"role\": \"Financeiro\", \"email\": \"tony@stark.com\", \"phone\": \"11999990001\", \"isPrimary\": true, \"active\": true }"))
            )
            @RequestBody @Valid Contact newContact) {
        return posts.execute(newContact, () -> createContact(newContact));
    }

    private ResponseEntity<EntityModel<Contact>> createContact(Contact newContact) {
        scope.requireCompany(newContact.getCompanyId());
        if (newContact.getCreatedAt() == null) {
            newContact.setCreatedAt(LocalDateTime.now());
        }
        newContact.setUpdatedAt(LocalDateTime.now());
        Contact saved = repository.save(newContact);
        EntityModel<Contact> entityModel = assembler.toModel(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(entityModel);
    }

    @Operation(summary = "Buscar contato por id", description = "Retorna um contato com links de navegação. Use o id retornado na listagem ou na criação.")
    @ApiResponse(responseCode = "200", description = "Contato encontrado")
    @ApiResponse(responseCode = "400", description = "Identificador inválido")
    @ApiResponse(responseCode = "404", description = "Contato não encontrado")
    @GetMapping("/contacts/{id}")
    public EntityModel<Contact> getContactById(@Parameter(description = "Id do contato") @PathVariable @Positive Long id) {
        Contact contact = repository.findByIdAndCompany_Account_Id(id, scope.accountId()).orElseThrow(() -> new ContactNotFoundException(id));
        return assembler.toModel(contact);
    }

    @Operation(summary = "Atualizar ou criar contato", description = "Atualiza o registro com o id informado. Se o ID não estiver associado à conta, cria um registro para essa conta com ID gerado pelo servidor. O campo createdAt original é mantido e o campo updatedAt é atualizado pelo servidor.")
    @ApiResponse(responseCode = "200", description = "Contato atualizado com sucesso")
    @ApiResponse(responseCode = "400", description = "Identificador ou corpo da requisição inválido")
    @ApiResponse(responseCode = "201", description = "Registro criado com id gerado pelo servidor")
    @PutMapping("/contacts/{id}")
    public ResponseEntity<EntityModel<Contact>> updateContact(@Parameter(description = "Id do contato") @PathVariable @Positive Long id, @RequestBody @Valid Contact newContact) {
        Contact contact = repository.findByIdAndCompany_Account_Id(id, scope.accountId()).orElse(null);
        if (contact == null) {
            return createContact(newContact);
        }
        scope.requireCompany(newContact.getCompanyId());
        contact.setCompanyId(newContact.getCompanyId());
        contact.setName(newContact.getName());
        contact.setRole(newContact.getRole());
        contact.setEmail(newContact.getEmail());
        contact.setPhone(newContact.getPhone());
        contact.setIsPrimary(newContact.getIsPrimary());
        contact.setActive(newContact.getActive());
        contact.setUpdatedAt(LocalDateTime.now());
        Contact saved = repository.save(contact);
        return ResponseEntity.ok(assembler.toModel(saved));
    }

    @Operation(summary = "Excluir contato", description = "Exclui o contato com o id informado. A resposta não possui conteúdo no corpo.")
    @ApiResponse(responseCode = "204", description = "Contato excluído com sucesso", content = @Content)
    @ApiResponse(responseCode = "400", description = "Identificador inválido")
    @ApiResponse(responseCode = "404", description = "Contato não encontrado", content = @Content)
    @DeleteMapping("/contacts/{id}")
    public ResponseEntity<Void> deleteContact(@Parameter(description = "Id do contato") @PathVariable @Positive Long id) {
        var contact = repository.findByIdAndCompany_Account_Id(id, scope.accountId());
        if (contact.isEmpty())
            return ResponseEntity.notFound().build();
        repository.delete(contact.get());
        return ResponseEntity.noContent().build();
    }
}
