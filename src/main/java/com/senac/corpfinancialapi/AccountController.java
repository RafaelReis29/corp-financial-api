package com.senac.corpfinancialapi;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(path = "/accounts", version = "1+")
@Tag(name = "accounts", description = "Administração de contas. Crie uma conta antes de emitir suas chaves.")
class AccountController {

    private final AccountRepository repository;
    private final IdempotentPosts posts;

    AccountController(AccountRepository repository, IdempotentPosts posts) {
        this.repository = repository;
        this.posts = posts;
    }

    @Operation(summary = "Criar conta", description = "Cria uma conta sem registros associados. Use o id retornado ao emitir chaves Básica e Premium.")
    @ApiResponse(responseCode = "201", description = "Conta criada")
    @PostMapping
    ResponseEntity<AccountMetadata> create(@RequestBody @Valid CreateAccount request) {
        return posts.execute(request, () -> {
            AccountMetadata account = AccountMetadata.from(repository.save(new Account(request.name().strip(), false)));
            return ResponseEntity.status(HttpStatus.CREATED).body(account);
        });
    }

    @Operation(summary = "Listar contas", description = "Lista as contas cadastradas e identifica a conta de demonstração.")
    @ApiResponse(responseCode = "200", description = "Contas retornadas")
    @GetMapping
    List<AccountMetadata> list() {
        return repository.findAll(Sort.by("name", "id")).stream().map(AccountMetadata::from).toList();
    }

    @Operation(summary = "Consultar conta por ID")
    @ApiResponse(responseCode = "200", description = "Conta encontrada")
    @ApiResponse(responseCode = "404", description = "Conta não encontrada")
    @GetMapping("/{id}")
    AccountMetadata get(@PathVariable UUID id) {
        return repository.findById(id).map(AccountMetadata::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada."));
    }

    record CreateAccount(@NotBlank @Size(max = 80) String name) {
    }

    record AccountMetadata(UUID id, String name, boolean demo) {
        static AccountMetadata from(Account account) {
            return new AccountMetadata(account.id(), account.name(), account.demo());
        }
    }
}
