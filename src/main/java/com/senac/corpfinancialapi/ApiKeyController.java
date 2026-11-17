package com.senac.corpfinancialapi;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(path = "/api-keys", version = "1+")
@Tag(name = "api-keys", description = "Emissão e revogação de chaves de cliente. Exige a chave de administração.")
class ApiKeyController {

    private final ApiKeys keys;
    private final ApiCredentialRepository repository;
    private final IdempotentPosts posts;
    private final AccountRepository accounts;

    ApiKeyController(ApiKeys keys, ApiCredentialRepository repository, IdempotentPosts posts, AccountRepository accounts) {
        this.keys = keys;
        this.repository = repository;
        this.posts = posts;
        this.accounts = accounts;
    }

    @Operation(summary = "Emitir chave de API", description = "Informe a conta, o tipo BASIC ou PREMIUM e um rótulo para identificar a integração. Guarde a chave retornada na criação. A credencial pública de demonstração já vem configurada.")
    @ApiResponse(responseCode = "201", description = "Chave emitida")
    @ApiResponse(responseCode = "404", description = "Conta não encontrada")
    @PostMapping
    ResponseEntity<KeyCreated> create(@RequestBody @Valid CreateKey request) {
        return posts.execute(request, () -> {
            Account account = accounts.findById(request.accountId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada."));
            if (account.demo()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Escolha BASIC ou PREMIUM e informe uma conta de cliente.");
            }
            ApiKeys.IssuedKey issued = keys.issue(request.label().strip(), account, ApiKeyType.valueOf(request.type()));
            KeyMetadata metadata = KeyMetadata.from(issued.credential());
            var location = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(metadata.id()).toUri();
            return ResponseEntity.created(location).cacheControl(CacheControl.noStore()).body(new KeyCreated(metadata, issued.secret()));
        });
    }

    @Operation(summary = "Listar chaves de API", description = "Retorna identificadores, rótulos, contas, tipos e datas de criação.")
    @ApiResponse(responseCode = "200", description = "Chaves retornadas")
    @GetMapping
    List<KeyMetadata> list() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream().map(KeyMetadata::from).toList();
    }

    @Operation(summary = "Consultar metadados de uma chave de API")
    @ApiResponse(responseCode = "200", description = "Chave encontrada")
    @ApiResponse(responseCode = "404", description = "Chave não encontrada")
    @GetMapping("/{id}")
    KeyMetadata get(@PathVariable UUID id) {
        return repository.findById(id).map(KeyMetadata::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found."));
    }

    @Operation(summary = "Revogar chave de API", description = "A chave deixa de autenticar novas requisições.")
    @ApiResponse(responseCode = "204", description = "Chave revogada")
    @ApiResponse(responseCode = "404", description = "Chave não encontrada")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> revoke(@PathVariable UUID id) {
        if (repository.revoke(id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found.");
        }
        return ResponseEntity.noContent().build();
    }

    record CreateKey(@NotBlank @Size(max = 80) String label, @NotNull UUID accountId,
                     @NotBlank @Pattern(regexp = "BASIC|PREMIUM", message = "Informe BASIC ou PREMIUM.")
                     @Schema(allowableValues = {"BASIC", "PREMIUM"}, example = "BASIC") String type) {
    }

    record KeyMetadata(UUID id, String label, UUID accountId, ApiKeyType type, Instant createdAt) {
        static KeyMetadata from(ApiCredential credential) {
            return new KeyMetadata(credential.id(), credential.label(), credential.account().id(), credential.type(), credential.createdAt());
        }
    }

    record KeyCreated(KeyMetadata metadata, String apiKey) {
        @Override
        public String toString() {
            return "KeyCreated[" + metadata + "]";
        }
    }
}
