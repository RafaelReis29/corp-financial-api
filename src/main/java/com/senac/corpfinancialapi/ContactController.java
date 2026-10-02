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
@Tag(name = "contacts", description = "Gestão de contatos das empresas. Cada contato pertence a uma empresa pelo campo companyId. A base inicial contém personagens da cultura pop.")
public class ContactController {

    private final ContactRepository repository;
    private final ContactModelAssembler assembler;
    private final PagedResourcesAssembler<Contact> pagedResourcesAssembler;

    public ContactController(ContactRepository repository, ContactModelAssembler assembler, PagedResourcesAssembler<Contact> pagedResourcesAssembler) {
        this.repository = repository;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Listar contatos", description = "Retorna lista paginada de contatos com links de navegação. Use page, size e sort para navegar.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping("/contacts")
    public ResponseEntity<PagedModel<EntityModel<Contact>>> getAllContacts(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Contact> contactPage = repository.findAll(pageable);
        PagedModel<EntityModel<Contact>> pagedModel = pagedResourcesAssembler.toModel(contactPage, assembler);
        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Criar contato", description = "Cria um contato vinculado a uma empresa. Envie companyId com o id de uma empresa existente, além de name, role, email, phone, isPrimary e active. Os campos createdAt e updatedAt são preenchidos pelo servidor quando ausentes.")
    @ApiResponse(responseCode = "201", description = "Contato criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos. Confira os campos obrigatórios")
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
        if (newContact.getCreatedAt() == null) {
            newContact.setCreatedAt(LocalDateTime.now());
        }
        newContact.setUpdatedAt(LocalDateTime.now());
        Contact saved = repository.save(newContact);
        EntityModel<Contact> entityModel = assembler.toModel(saved);
        return ResponseEntity.created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(entityModel);
    }

    @Operation(summary = "Buscar contato por id", description = "Retorna um contato com links de navegação. Use o id retornado na listagem ou na criação.")
    @ApiResponse(responseCode = "200", description = "Contato encontrado")
    @ApiResponse(responseCode = "404", description = "Contato não encontrado")
    @GetMapping("/contacts/{id}")
    public EntityModel<Contact> getContactById(@Parameter(description = "Id do contato") @PathVariable Long id) {
        Contact contact = repository.findById(id).orElseThrow(() -> new ContactNotFoundException(id));
        return assembler.toModel(contact);
    }

    @Operation(summary = "Atualizar ou criar contato", description = "Atualiza todos os campos do contato com o id informado. Se o id não existir, cria um contato novo com id gerado pelo servidor. O campo createdAt original é mantido e o campo updatedAt é atualizado pelo servidor.")
    @ApiResponse(responseCode = "200", description = "Contato atualizado com sucesso")
    @ApiResponse(responseCode = "201", description = "Contato criado com id gerado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos. Confira os campos obrigatórios")
    @PutMapping("/contacts/{id}")
    public ResponseEntity<EntityModel<Contact>> updateOrCreateContact(@Parameter(description = "Id do contato") @PathVariable Long id, @RequestBody @Valid Contact newContact) {
        return repository.findById(id)
                .map(contact -> {
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
                })
                .orElseGet(() -> {
                    if (newContact.getCreatedAt() == null) {
                        newContact.setCreatedAt(LocalDateTime.now());
                    }
                    newContact.setUpdatedAt(LocalDateTime.now());
                    Contact saved = repository.save(newContact);
                    EntityModel<Contact> entityModel = assembler.toModel(saved);
                    return ResponseEntity.created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(entityModel);
                });
    }

    @Operation(summary = "Excluir contato", description = "Exclui o contato com o id informado. A resposta não possui conteúdo no corpo.")
    @ApiResponse(responseCode = "204", description = "Contato excluído com sucesso")
    @ApiResponse(responseCode = "404", description = "Contato não encontrado")
    @DeleteMapping("/contacts/{id}")
    public ResponseEntity<?> deleteContact(@Parameter(description = "Id do contato") @PathVariable Long id) {
        var contact = repository.findById(id);
        if (contact.isEmpty())
            return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
