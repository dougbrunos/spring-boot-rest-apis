# HTTP Status Codes

Este documento apresenta os principais códigos de status HTTP utilizados em APIs REST, suas aplicações e exemplos práticos de implementação.

## O que são HTTP Status Codes?

Os **HTTP Status Codes** são códigos numéricos de três dígitos que indicam o resultado de uma requisição HTTP. Eles são essenciais para comunicar o estado da operação entre cliente e servidor.

### Estrutura dos Códigos:
- **1xx**: Informacionais (Continue)
- **2xx**: Sucesso (Success)
- **3xx**: Redirecionamento (Redirection)
- **4xx**: Erro do Cliente (Client Error)
- **5xx**: Erro do Servidor (Server Error)

---

## 1. Códigos 2xx - Sucesso

### 200 OK
**Uso**: Requisição processada com sucesso.
**Aplicação**: GET, PUT, PATCH bem-sucedidos.

```java
@GetMapping("/users/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    User user = userService.findById(id);
    return ResponseEntity.ok(user); // 200 OK
}

@PutMapping("/users/{id}")
public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
    User updatedUser = userService.update(id, user);
    return ResponseEntity.ok(updatedUser); // 200 OK
}
```

### 201 Created
**Uso**: Recurso criado com sucesso.
**Aplicação**: POST para criar novos recursos.

```java
@PostMapping("/users")
public ResponseEntity<User> createUser(@RequestBody User user) {
    User newUser = userService.create(user);
    URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(newUser.getId())
        .toUri();
    
    return ResponseEntity.created(location).body(newUser); // 201 Created
}
```

### 202 Accepted
**Uso**: Requisição aceita para processamento assíncrono.
**Aplicação**: Operações que levam tempo para processar.

```java
@PostMapping("/reports/generate")
public ResponseEntity<String> generateReport(@RequestBody ReportRequest request) {
    String jobId = reportService.generateAsync(request);
    return ResponseEntity.accepted()
        .header("Location", "/reports/jobs/" + jobId)
        .body("Report generation started"); // 202 Accepted
}
```

### 204 No Content
**Uso**: Operação bem-sucedida sem conteúdo de retorno.
**Aplicação**: DELETE, PUT que não retornam dados.

```java
@DeleteMapping("/users/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.delete(id);
    return ResponseEntity.noContent().build(); // 204 No Content
}

@PutMapping("/users/{id}/status")
public ResponseEntity<Void> updateUserStatus(@PathVariable Long id, @RequestBody String status) {
    userService.updateStatus(id, status);
    return ResponseEntity.noContent().build(); // 204 No Content
}
```

---

## 2. Códigos 3xx - Redirecionamento

### 301 Moved Permanently
**Uso**: Recurso movido permanentemente.
**Aplicação**: Mudanças permanentes de URL.

```java
@GetMapping("/old-api/users/{id}")
public ResponseEntity<Void> redirectToNewApi(@PathVariable Long id) {
    return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
        .location(URI.create("/api/v2/users/" + id))
        .build(); // 301 Moved Permanently
}
```

### 304 Not Modified
**Uso**: Recurso não foi modificado desde a última requisição.
**Aplicação**: Cache condicional com ETags ou Last-Modified.

```java
@GetMapping("/users/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id, 
                                   @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
    User user = userService.findById(id);
    String etag = "\"" + user.getVersion() + "\"";
    
    if (etag.equals(ifNoneMatch)) {
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build(); // 304 Not Modified
    }
    
    return ResponseEntity.ok()
        .eTag(etag)
        .body(user);
}
```

---

## 3. Códigos 4xx - Erro do Cliente

### 400 Bad Request
**Uso**: Requisição malformada ou dados inválidos.
**Aplicação**: Validação de entrada falha.

```java
@PostMapping("/users")
public ResponseEntity<?> createUser(@Valid @RequestBody User user, BindingResult result) {
    if (result.hasErrors()) {
        List<String> errors = result.getAllErrors().stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .collect(Collectors.toList());
        
        return ResponseEntity.badRequest().body(errors); // 400 Bad Request
    }
    
    User newUser = userService.create(user);
    return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
}

@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
    ErrorResponse error = new ErrorResponse("Validation failed", ex.getBindingResult());
    return ResponseEntity.badRequest().body(error); // 400 Bad Request
}
```

### 401 Unauthorized
**Uso**: Autenticação necessária ou inválida.
**Aplicação**: Token ausente ou inválido.

```java
@ExceptionHandler(AuthenticationException.class)
public ResponseEntity<ErrorResponse> handleAuthenticationError(AuthenticationException ex) {
    ErrorResponse error = new ErrorResponse("Authentication required", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .header("WWW-Authenticate", "Bearer")
        .body(error); // 401 Unauthorized
}

@GetMapping("/protected")
public ResponseEntity<String> protectedEndpoint(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body("Authentication required"); // 401 Unauthorized
    }
    return ResponseEntity.ok("Access granted");
}
```

### 403 Forbidden
**Uso**: Acesso negado (usuário autenticado mas sem permissão).
**Aplicação**: Autorização insuficiente.

```java
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/users/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.delete(id);
    return ResponseEntity.noContent().build();
}

@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
    ErrorResponse error = new ErrorResponse("Access denied", "Insufficient permissions");
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error); // 403 Forbidden
}
```

### 404 Not Found
**Uso**: Recurso não encontrado.
**Aplicação**: ID inexistente, endpoint inválido.

```java
@GetMapping("/users/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    try {
        User user = userService.findById(id);
        return ResponseEntity.ok(user);
    } catch (UserNotFoundException ex) {
        return ResponseEntity.notFound().build(); // 404 Not Found
    }
}

@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
    ErrorResponse error = new ErrorResponse("Resource not found", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error); // 404 Not Found
}
```

### 409 Conflict
**Uso**: Conflito com o estado atual do recurso.
**Aplicação**: Tentativa de criar recurso duplicado.

```java
@PostMapping("/users")
public ResponseEntity<?> createUser(@RequestBody User user) {
    try {
        User newUser = userService.create(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
    } catch (EmailAlreadyExistsException ex) {
        ErrorResponse error = new ErrorResponse("Conflict", "Email already exists");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error); // 409 Conflict
    }
}
```

### 422 Unprocessable Entity
**Uso**: Dados semanticamente incorretos.
**Aplicação**: Validação de regras de negócio.

```java
@PostMapping("/orders")
public ResponseEntity<?> createOrder(@RequestBody Order order) {
    try {
        Order newOrder = orderService.create(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(newOrder);
    } catch (InsufficientStockException ex) {
        ErrorResponse error = new ErrorResponse("Unprocessable Entity", "Insufficient stock");
        return ResponseEntity.unprocessableEntity().body(error); // 422 Unprocessable Entity
    }
}
```

### 429 Too Many Requests
**Uso**: Limite de taxa excedido.
**Aplicação**: Rate limiting.

```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!rateLimitService.isAllowed(request.getRemoteAddr())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 429 Too Many Requests
            response.setHeader("Retry-After", "60");
            return false;
        }
        return true;
    }
}
```

---

## 4. Códigos 5xx - Erro do Servidor

### 500 Internal Server Error
**Uso**: Erro interno não tratado.
**Aplicação**: Exceções inesperadas.

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
    logger.error("Unexpected error occurred", ex);
    ErrorResponse error = new ErrorResponse("Internal Server Error", "An unexpected error occurred");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error); // 500 Internal Server Error
}
```

### 502 Bad Gateway
**Uso**: Erro em serviço downstream.
**Aplicação**: Falha em chamadas para APIs externas.

```java
@GetMapping("/external-data")
public ResponseEntity<?> getExternalData() {
    try {
        ExternalData data = externalService.fetchData();
        return ResponseEntity.ok(data);
    } catch (ExternalServiceException ex) {
        ErrorResponse error = new ErrorResponse("Bad Gateway", "External service unavailable");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error); // 502 Bad Gateway
    }
}
```

### 503 Service Unavailable
**Uso**: Serviço temporariamente indisponível.
**Aplicação**: Manutenção, sobrecarga.

```java
@GetMapping("/health")
public ResponseEntity<String> healthCheck() {
    if (!systemHealthService.isHealthy()) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header("Retry-After", "300")
            .body("Service temporarily unavailable"); // 503 Service Unavailable
    }
    return ResponseEntity.ok("Service is healthy");
}
```

---

## Boas Práticas para Status Codes

### 1. **Consistência**
```java
// Sempre use os mesmos códigos para situações similares
@RestController
public class UserController {
    
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userService.findById(id)
            .map(user -> ResponseEntity.ok(user))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/users/{id}/orders")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable Long id) {
        return userService.findById(id)
            .map(user -> ResponseEntity.ok(orderService.findByUser(user)))
            .orElse(ResponseEntity.notFound().build());
    }
}
```

### 2. **Headers Apropriados**
```java
@PostMapping("/users")
public ResponseEntity<User> createUser(@RequestBody User user) {
    User newUser = userService.create(user);
    
    URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(newUser.getId())
        .toUri();
    
    return ResponseEntity.created(location)
        .header("X-Created-By", "UserService")
        .body(newUser);
}
```

### 3. **Mensagens de Erro Padronizadas**
```java
@Data
@AllArgsConstructor
public class ErrorResponse {
    private String error;
    private String message;
    private LocalDateTime timestamp = LocalDateTime.now();
    private String path;
    
    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }
}

@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, 
                                                       HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
            "Not Found", 
            ex.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

### 4. **Documentação com OpenAPI**
```java
@GetMapping("/users/{id}")
@Operation(summary = "Buscar usuário por ID")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
    @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
})
public ResponseEntity<User> getUser(@PathVariable Long id) {
    // implementação
}
```

---

## Tabela de Referência Rápida

| Código | Nome | Quando Usar | Exemplo de Uso |
|--------|------|-------------|----------------|
| **2xx - Sucesso** |
| 200 | OK | Operação bem-sucedida | GET, PUT, PATCH |
| 201 | Created | Recurso criado | POST |
| 202 | Accepted | Processamento assíncrono | Jobs, uploads |
| 204 | No Content | Sucesso sem retorno | DELETE |
| **3xx - Redirecionamento** |
| 301 | Moved Permanently | URL mudou permanentemente | Migração de API |
| 304 | Not Modified | Cache válido | ETags |
| **4xx - Erro do Cliente** |
| 400 | Bad Request | Dados inválidos | Validação |
| 401 | Unauthorized | Sem autenticação | Token ausente |
| 403 | Forbidden | Sem autorização | Permissões |
| 404 | Not Found | Recurso inexistente | ID inválido |
| 409 | Conflict | Conflito de estado | Duplicação |
| 422 | Unprocessable Entity | Erro de negócio | Regras violadas |
| 429 | Too Many Requests | Rate limit | Muitas requisições |
| **5xx - Erro do Servidor** |
| 500 | Internal Server Error | Erro não tratado | Exceções |
| 502 | Bad Gateway | Serviço externo falhou | API externa |
| 503 | Service Unavailable | Serviço indisponível | Manutenção |

---

## Mapeamento por Operação REST

### CREATE (POST)
- ✅ **201 Created**: Recurso criado com sucesso
- ❌ **400 Bad Request**: Dados inválidos
- ❌ **409 Conflict**: Recurso já existe
- ❌ **422 Unprocessable Entity**: Regra de negócio violada

### READ (GET)
- ✅ **200 OK**: Recurso encontrado
- ✅ **304 Not Modified**: Cache válido
- ❌ **404 Not Found**: Recurso não existe

### UPDATE (PUT/PATCH)
- ✅ **200 OK**: Atualizado com retorno
- ✅ **204 No Content**: Atualizado sem retorno
- ❌ **400 Bad Request**: Dados inválidos
- ❌ **404 Not Found**: Recurso não existe
- ❌ **409 Conflict**: Conflito de versão

### DELETE
- ✅ **204 No Content**: Deletado com sucesso
- ❌ **404 Not Found**: Recurso não existe
- ❌ **409 Conflict**: Não pode ser deletado

O uso correto dos HTTP Status Codes é fundamental para criar APIs REST semânticas e intuitivas, facilitando a integração e o debugging por parte dos desenvolvedores que consomem a API.

