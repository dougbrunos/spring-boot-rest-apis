# Métodos HTTP

Este documento apresenta os principais métodos HTTP utilizados em APIs REST, suas características, propriedades e exemplos práticos de implementação.

## O que são Métodos HTTP?

Os **Métodos HTTP** (também chamados de verbos HTTP) definem a ação que deve ser realizada em um recurso específico. Cada método tem características próprias que determinam como ele deve ser usado em APIs REST.

### Propriedades dos Métodos HTTP:

- **Safe (Seguro)**: Não modifica o estado do servidor
- **Idempotent (Idempotente)**: Múltiplas execuções produzem o mesmo resultado
- **Cacheable (Cacheável)**: Resposta pode ser armazenada em cache

---

## 1. GET - Recuperar Dados

### Características:
- ✅ **Safe**: Não modifica dados
- ✅ **Idempotent**: Sempre retorna o mesmo resultado
- ✅ **Cacheable**: Responses podem ser cacheadas
- 📝 **Uso**: Buscar/listar recursos

### Sintaxe:
```http
GET /api/users
GET /api/users/123
GET /api/users/123/orders
```

### Implementação Spring Boot:
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // Listar todos os usuários
    @GetMapping
    public ResponseEntity<Page<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = userService.findAll(pageable, name);
        return ResponseEntity.ok(users);
    }
    
    // Buscar usuário por ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Buscar pedidos de um usuário
    @GetMapping("/{id}/orders")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable Long id) {
        if (!userService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        List<Order> orders = orderService.findByUserId(id);
        return ResponseEntity.ok(orders);
    }
}
```

### Exemplos de Cache:
```java
@GetMapping("/{id}")
@Cacheable("users")
public ResponseEntity<User> getUserById(@PathVariable Long id) {
    User user = userService.findById(id);
    
    return ResponseEntity.ok()
            .eTag("\"" + user.getVersion() + "\"")
            .lastModified(user.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant())
            .body(user);
}
```

### Quando Usar GET:
- ✅ Listar recursos: `GET /api/products`
- ✅ Buscar por ID: `GET /api/products/123`
- ✅ Filtrar/pesquisar: `GET /api/products?category=electronics`
- ✅ Recursos aninhados: `GET /api/users/123/orders`

---

## 2. POST - Criar Recursos

### Características:
- ❌ **Not Safe**: Modifica o estado do servidor
- ❌ **Not Idempotent**: Múltiplas execuções criam recursos diferentes
- ❌ **Not Cacheable**: Não deve ser cacheado
- 📝 **Uso**: Criar novos recursos

### Sintaxe:
```http
POST /api/users
Content-Type: application/json

{
    "name": "João Silva",
    "email": "joao@email.com"
}
```

### Implementação Spring Boot:
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // Criar novo usuário
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        User newUser = userService.create(user);
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newUser.getId())
                .toUri();
        
        return ResponseEntity.created(location).body(newUser);
    }
    
    // Upload de arquivo
    @PostMapping("/{id}/avatar")
    public ResponseEntity<String> uploadAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        
        if (!userService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        String avatarUrl = fileService.upload(file, "avatars");
        userService.updateAvatar(id, avatarUrl);
        
        return ResponseEntity.created(URI.create(avatarUrl))
                .body("Avatar uploaded successfully");
    }
    
    // Operação customizada
    @PostMapping("/{id}/send-email")
    public ResponseEntity<String> sendEmail(
            @PathVariable Long id,
            @RequestBody EmailRequest emailRequest) {
        
        User user = userService.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        emailService.sendEmail(user.getEmail(), emailRequest);
        
        return ResponseEntity.accepted()
                .body("Email queued for sending");
    }
}
```

### Validação e Tratamento de Erros:
```java
@PostMapping
public ResponseEntity<?> createUser(@Valid @RequestBody User user, BindingResult result) {
    if (result.hasErrors()) {
        List<String> errors = result.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());
        
        return ResponseEntity.badRequest().body(new ErrorResponse("Validation failed", errors));
    }
    
    try {
        User newUser = userService.create(user);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newUser.getId())
                .toUri();
        
        return ResponseEntity.created(location).body(newUser);
    } catch (EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("Conflict", ex.getMessage()));
    }
}
```

### Quando Usar POST:
- ✅ Criar recursos: `POST /api/users`
- ✅ Upload de arquivos: `POST /api/users/123/avatar`
- ✅ Operações customizadas: `POST /api/users/123/send-email`
- ✅ Processamento assíncrono: `POST /api/reports/generate`

---

## 3. PUT - Substituir/Atualizar Completamente

### Características:
- ❌ **Not Safe**: Modifica o estado do servidor
- ✅ **Idempotent**: Múltiplas execuções produzem o mesmo resultado
- ❌ **Not Cacheable**: Não deve ser cacheado
- 📝 **Uso**: Substituir recurso completamente

### Sintaxe:
```http
PUT /api/users/123
Content-Type: application/json

{
    "name": "João Silva Santos",
    "email": "joao.santos@email.com",
    "age": 30
}
```

### Implementação Spring Boot:
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // Atualizar usuário completamente
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody User user) {
        
        return userService.findById(id)
                .map(existingUser -> {
                    // Substitui todos os campos
                    existingUser.setName(user.getName());
                    existingUser.setEmail(user.getEmail());
                    existingUser.setAge(user.getAge());
                    existingUser.setPhone(user.getPhone());
                    
                    User updatedUser = userService.save(existingUser);
                    return ResponseEntity.ok(updatedUser);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // PUT também pode criar se não existir (upsert)
    @PutMapping("/{id}")
    public ResponseEntity<User> upsertUser(
            @PathVariable Long id,
            @Valid @RequestBody User user) {
        
        Optional<User> existingUser = userService.findById(id);
        
        if (existingUser.isPresent()) {
            // Atualizar usuário existente
            User updated = userService.update(id, user);
            return ResponseEntity.ok(updated);
        } else {
            // Criar novo usuário com ID específico
            user.setId(id);
            User created = userService.create(user);
            
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .build()
                    .toUri();
            
            return ResponseEntity.created(location).body(created);
        }
    }
}
```

### Controle de Concorrência:
```java
@PutMapping("/{id}")
public ResponseEntity<User> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody User user,
        @RequestHeader(value = "If-Match", required = false) String ifMatch) {
    
    User existingUser = userService.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
    
    // Verificar ETag para controle de concorrência
    if (ifMatch != null && !ifMatch.equals("\"" + existingUser.getVersion() + "\"")) {
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
    }
    
    User updatedUser = userService.update(id, user);
    
    return ResponseEntity.ok()
            .eTag("\"" + updatedUser.getVersion() + "\"")
            .body(updatedUser);
}
```

### Quando Usar PUT:
- ✅ Substituição completa: `PUT /api/users/123`
- ✅ Upsert (criar ou atualizar): `PUT /api/settings/theme`
- ✅ Recursos com ID conhecido: `PUT /api/profiles/my-profile`

---

## 4. PATCH - Atualizar Parcialmente

### Características:
- ❌ **Not Safe**: Modifica o estado do servidor
- ⚠️ **Not Idempotent**: Pode não ser idempotente (depende da implementação)
- ❌ **Not Cacheable**: Não deve ser cacheado
- 📝 **Uso**: Atualizar campos específicos

### Sintaxe:
```http
PATCH /api/users/123
Content-Type: application/json

{
    "email": "novo.email@example.com"
}
```

### Implementação Spring Boot:
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // Atualização parcial simples
    @PatchMapping("/{id}")
    public ResponseEntity<User> patchUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        
        return userService.findById(id)
                .map(user -> {
                    // Aplicar apenas os campos fornecidos
                    updates.forEach((key, value) -> {
                        switch (key) {
                            case "name":
                                user.setName((String) value);
                                break;
                            case "email":
                                user.setEmail((String) value);
                                break;
                            case "age":
                                user.setAge((Integer) value);
                                break;
                            // Ignorar campos desconhecidos
                        }
                    });
                    
                    User updatedUser = userService.save(user);
                    return ResponseEntity.ok(updatedUser);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Usando JSON Patch (RFC 6902)
    @PatchMapping(value = "/{id}", consumes = "application/json-patch+json")
    public ResponseEntity<User> patchUserWithJsonPatch(
            @PathVariable Long id,
            @RequestBody JsonPatch patch) {
        
        try {
            User user = userService.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
            
            // Converter para JsonNode
            JsonNode userNode = objectMapper.valueToTree(user);
            
            // Aplicar patch
            JsonNode patchedNode = patch.apply(userNode);
            
            // Converter de volta
            User patchedUser = objectMapper.treeToValue(patchedNode, User.class);
            
            // Salvar
            User updatedUser = userService.save(patchedUser);
            
            return ResponseEntity.ok(updatedUser);
        } catch (JsonPatchException | JsonProcessingException ex) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Operações específicas
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateUserStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {
        
        if (!userService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        userService.updateStatus(id, request.getStatus());
        return ResponseEntity.noContent().build();
    }
}
```

### DTO para Atualizações Parciais:
```java
public class UserPatchDTO {
    @Email
    private String email;
    
    @Size(min = 2, max = 100)
    private String name;
    
    @Min(18)
    @Max(120)
    private Integer age;
    
    // getters e setters
}

@PatchMapping("/{id}")
public ResponseEntity<User> patchUser(
        @PathVariable Long id,
        @Valid @RequestBody UserPatchDTO patchDTO) {
    
    return userService.findById(id)
            .map(user -> {
                if (patchDTO.getEmail() != null) {
                    user.setEmail(patchDTO.getEmail());
                }
                if (patchDTO.getName() != null) {
                    user.setName(patchDTO.getName());
                }
                if (patchDTO.getAge() != null) {
                    user.setAge(patchDTO.getAge());
                }
                
                User updatedUser = userService.save(user);
                return ResponseEntity.ok(updatedUser);
            })
            .orElse(ResponseEntity.notFound().build());
}
```

### Quando Usar PATCH:
- ✅ Atualizar campos específicos: `PATCH /api/users/123`
- ✅ Mudanças de status: `PATCH /api/orders/123/status`
- ✅ Operações incrementais: `PATCH /api/counters/123/increment`

---

## 5. DELETE - Remover Recursos

### Características:
- ❌ **Not Safe**: Modifica o estado do servidor
- ✅ **Idempotent**: Múltiplas execuções têm o mesmo efeito
- ❌ **Not Cacheable**: Não deve ser cacheado
- 📝 **Uso**: Remover recursos

### Sintaxe:
```http
DELETE /api/users/123
DELETE /api/users/123/orders/456
```

### Implementação Spring Boot:
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // Deletar usuário
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            userService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
    
    // Soft delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDeleteUser(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> {
                    userService.softDelete(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Deletar recurso aninhado
    @DeleteMapping("/{userId}/orders/{orderId}")
    public ResponseEntity<Void> deleteUserOrder(
            @PathVariable Long userId,
            @PathVariable Long orderId) {
        
        if (!userService.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }
        
        if (!orderService.existsByIdAndUserId(orderId, userId)) {
            return ResponseEntity.notFound().build();
        }
        
        orderService.delete(orderId);
        return ResponseEntity.noContent().build();
    }
    
    // Deletar múltiplos recursos
    @DeleteMapping
    public ResponseEntity<Void> deleteUsers(@RequestParam List<Long> ids) {
        List<Long> existingIds = userService.findExistingIds(ids);
        
        if (existingIds.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        userService.deleteAll(existingIds);
        return ResponseEntity.noContent().build();
    }
}
```

### Validações e Segurança:
```java
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN') or @userService.isOwner(#id, authentication.name)")
public ResponseEntity<Void> deleteUser(
        @PathVariable Long id,
        Authentication authentication) {
    
    User user = userService.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
    
    // Verificar se pode ser deletado
    if (userService.hasActiveOrders(id)) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .header("X-Error-Reason", "User has active orders")
                .build();
    }
    
    userService.delete(id);
    return ResponseEntity.noContent().build();
}
```

### Quando Usar DELETE:
- ✅ Remover recursos: `DELETE /api/users/123`
- ✅ Limpar dados: `DELETE /api/cache/users`
- ✅ Recursos aninhados: `DELETE /api/users/123/sessions/456`
- ✅ Remoção em lote: `DELETE /api/users?ids=1,2,3`

---

## 6. HEAD - Verificar Metadados

### Características:
- ✅ **Safe**: Não modifica dados
- ✅ **Idempotent**: Sempre retorna o mesmo resultado
- ✅ **Cacheable**: Responses podem ser cacheadas
- 📝 **Uso**: Obter metadados sem o corpo da resposta

### Implementação Spring Boot:
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @RequestMapping(value = "/{id}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkUserExists(@PathVariable Long id) {
        if (userService.existsById(id)) {
            User user = userService.findById(id).get();
            return ResponseEntity.ok()
                    .eTag("\"" + user.getVersion() + "\"")
                    .lastModified(user.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant())
                    .build();
        }
        return ResponseEntity.notFound().build();
    }
    
    @RequestMapping(method = RequestMethod.HEAD)
    public ResponseEntity<Void> getUsersCount() {
        long count = userService.count();
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(count))
                .build();
    }
}
```

---

## 7. OPTIONS - Descobrir Métodos Permitidos

### Características:
- ✅ **Safe**: Não modifica dados
- ✅ **Idempotent**: Sempre retorna o mesmo resultado
- ✅ **Cacheable**: Responses podem ser cacheadas
- 📝 **Uso**: Descobrir métodos HTTP permitidos

### Implementação Spring Boot:
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> options() {
        return ResponseEntity.ok()
                .allow(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, 
                       HttpMethod.PATCH, HttpMethod.DELETE, HttpMethod.HEAD, 
                       HttpMethod.OPTIONS)
                .build();
    }
    
    @RequestMapping(value = "/{id}", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> optionsById(@PathVariable Long id) {
        if (!userService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok()
                .allow(HttpMethod.GET, HttpMethod.PUT, HttpMethod.PATCH, 
                       HttpMethod.DELETE, HttpMethod.HEAD, HttpMethod.OPTIONS)
                .build();
    }
}
```

---

## Comparação dos Métodos HTTP

| Método | Safe | Idempotent | Cacheable | Uso Principal |
|--------|------|------------|-----------|---------------|
| **GET** | ✅ | ✅ | ✅ | Buscar dados |
| **POST** | ❌ | ❌ | ❌ | Criar recursos |
| **PUT** | ❌ | ✅ | ❌ | Substituir completamente |
| **PATCH** | ❌ | ⚠️ | ❌ | Atualizar parcialmente |
| **DELETE** | ❌ | ✅ | ❌ | Remover recursos |
| **HEAD** | ✅ | ✅ | ✅ | Verificar metadados |
| **OPTIONS** | ✅ | ✅ | ✅ | Descobrir métodos |

---

## Mapeamento CRUD para HTTP

| Operação CRUD | Método HTTP | Endpoint | Resposta |
|---------------|-------------|----------|----------|
| **Create** | POST | `POST /api/users` | 201 Created |
| **Read** | GET | `GET /api/users/123` | 200 OK |
| **Update** | PUT/PATCH | `PUT /api/users/123` | 200 OK |
| **Delete** | DELETE | `DELETE /api/users/123` | 204 No Content |
| **List** | GET | `GET /api/users` | 200 OK |

---

## Boas Práticas

### 1. **Escolha do Método Correto**
```java
// ✅ Correto - GET para buscar
@GetMapping("/users/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) { }

// ✅ Correto - POST para criar
@PostMapping("/users")
public ResponseEntity<User> createUser(@RequestBody User user) { }

// ❌ Incorreto - GET para modificar dados
@GetMapping("/users/{id}/delete")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) { }
```

### 2. **Idempotência**
```java
// PUT é idempotente - múltiplas chamadas têm o mesmo efeito
@PutMapping("/users/{id}")
public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
    // Sempre resulta no mesmo estado final
}

// POST não é idempotente - múltiplas chamadas criam recursos diferentes
@PostMapping("/users")
public ResponseEntity<User> createUser(@RequestBody User user) {
    // Cada chamada cria um novo usuário
}
```

### 3. **Responses Apropriadas**
```java
@PostMapping("/users")
public ResponseEntity<User> createUser(@RequestBody User user) {
    User newUser = userService.create(user);
    URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(newUser.getId())
            .toUri();
    
    return ResponseEntity.created(location).body(newUser);
}

@DeleteMapping("/users/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.delete(id);
    return ResponseEntity.noContent().build();
}
```

### 4. **Tratamento de Erros Consistente**
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.notFound().build();
    }
    
    @ExceptionHandler(MethodNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(MethodNotAllowedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .allow(ex.getSupportedMethods())
                .build();
    }
}
```

O uso correto dos métodos HTTP é fundamental para criar APIs REST semânticas, previsíveis e que seguem os padrões da web. Cada método tem seu propósito específico e deve ser usado adequadamente para garantir uma API bem estruturada.