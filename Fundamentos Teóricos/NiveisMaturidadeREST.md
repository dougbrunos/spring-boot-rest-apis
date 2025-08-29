# Níveis de Maturidade REST - Modelo Richardson

Este documento apresenta o **Modelo de Maturidade Richardson**, que define quatro níveis (0-3) para classificar o quão "RESTful" uma API realmente é. Desenvolvido por Leonard Richardson, este modelo ajuda a entender a evolução de uma API em direção aos princípios REST verdadeiros.

## Visão Geral dos Níveis

```
Nível 0: The Swamp of POX (Plain Old XML/JSON)
    ↓
Nível 1: Recursos
    ↓
Nível 2: Verbos HTTP
    ↓
Nível 3: Controles Hipermídia (HATEOAS)
```

---

## Nível 0 - The Swamp of POX

### Características:
- 🔄 **HTTP como transporte**: Usa HTTP apenas como túnel
- 📍 **Um único endpoint**: Geralmente uma única URL
- 📝 **POST para tudo**: Usa apenas POST (raramente GET)
- 🏷️ **Ação no payload**: A operação está no corpo da mensagem
- ❌ **Não RESTful**: Mais parecido com RPC

### Exemplo Nível 0:
```http
POST /api/service
Content-Type: application/json

{
    "action": "getUser",
    "userId": 123
}
```

```http
POST /api/service
Content-Type: application/json

{
    "action": "createUser",
    "userData": {
        "name": "João Silva",
        "email": "joao@email.com"
    }
}
```

```http
POST /api/service
Content-Type: application/json

{
    "action": "deleteUser",
    "userId": 123
}
```

### Implementação Spring Boot (Nível 0):
```java
@RestController
@RequestMapping("/api/service")
public class Level0Controller {
    
    @PostMapping
    public ResponseEntity<?> handleRequest(@RequestBody Map<String, Object> request) {
        String action = (String) request.get("action");
        
        switch (action) {
            case "getUser":
                Long userId = Long.valueOf(request.get("userId").toString());
                User user = userService.findById(userId);
                return ResponseEntity.ok(user);
                
            case "createUser":
                Map<String, Object> userData = (Map<String, Object>) request.get("userData");
                User newUser = userService.create(userData);
                return ResponseEntity.ok(newUser);
                
            case "updateUser":
                Long updateId = Long.valueOf(request.get("userId").toString());
                Map<String, Object> updateData = (Map<String, Object>) request.get("userData");
                User updatedUser = userService.update(updateId, updateData);
                return ResponseEntity.ok(updatedUser);
                
            case "deleteUser":
                Long deleteId = Long.valueOf(request.get("userId").toString());
                userService.delete(deleteId);
                return ResponseEntity.ok("User deleted");
                
            default:
                return ResponseEntity.badRequest().body("Unknown action");
        }
    }
}
```

### Problemas do Nível 0:
- ❌ Não aproveita os recursos do HTTP
- ❌ Difícil de cachear
- ❌ Sem semântica clara das operações
- ❌ Debugging complexo
- ❌ Não padronizado

---

## Nível 1 - Recursos

### Características:
- 🎯 **Múltiplos endpoints**: Diferentes URLs para diferentes recursos
- 📦 **Recursos identificados**: Cada recurso tem sua própria URL
- 🔄 **Ainda usa POST**: Mas agora com endpoints específicos
- 📝 **Estrutura melhor**: Organização mais clara

### Exemplo Nível 1:
```http
POST /api/users/get
Content-Type: application/json

{
    "userId": 123
}
```

```http
POST /api/users/create
Content-Type: application/json

{
    "name": "João Silva",
    "email": "joao@email.com"
}
```

```http
POST /api/users/update
Content-Type: application/json

{
    "userId": 123,
    "name": "João Silva Santos"
}
```

```http
POST /api/users/delete
Content-Type: application/json

{
    "userId": 123
}
```

### Implementação Spring Boot (Nível 1):
```java
@RestController
@RequestMapping("/api/users")
public class Level1Controller {
    
    @PostMapping("/get")
    public ResponseEntity<User> getUser(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        User user = userService.findById(userId);
        return ResponseEntity.ok(user);
    }
    
    @PostMapping("/create")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User newUser = userService.create(user);
        return ResponseEntity.ok(newUser);
    }
    
    @PostMapping("/update")
    public ResponseEntity<User> updateUser(@RequestBody UpdateUserRequest request) {
        User updatedUser = userService.update(request.getUserId(), request);
        return ResponseEntity.ok(updatedUser);
    }
    
    @PostMapping("/delete")
    public ResponseEntity<String> deleteUser(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        userService.delete(userId);
        return ResponseEntity.ok("User deleted successfully");
    }
    
    @PostMapping("/list")
    public ResponseEntity<List<User>> listUsers(@RequestBody(required = false) Map<String, Object> filters) {
        List<User> users = userService.findAll(filters);
        return ResponseEntity.ok(users);
    }
}
```

### Melhorias do Nível 1:
- ✅ Recursos claramente identificados
- ✅ Melhor organização do código
- ✅ Endpoints mais específicos
- ✅ Facilita manutenção

### Ainda falta:
- ❌ Não usa verbos HTTP apropriados
- ❌ Não aproveita códigos de status HTTP
- ❌ Difícil de cachear efetivamente

---

## Nível 2 - Verbos HTTP

### Características:
- 🔧 **Verbos HTTP corretos**: GET, POST, PUT, DELETE, etc.
- 📊 **Status codes apropriados**: 200, 201, 404, 500, etc.
- 🎯 **Semântica clara**: Cada verbo tem significado específico
- 💾 **Cacheable**: GET pode ser cacheado
- 🔒 **Idempotência**: PUT e DELETE são idempotentes

### Exemplo Nível 2:
```http
GET /api/users/123
Accept: application/json
```

```http
POST /api/users
Content-Type: application/json

{
    "name": "João Silva",
    "email": "joao@email.com"
}
```

```http
PUT /api/users/123
Content-Type: application/json

{
    "name": "João Silva Santos",
    "email": "joao.santos@email.com"
}
```

```http
DELETE /api/users/123
```

### Implementação Spring Boot (Nível 2):
```java
@RestController
@RequestMapping("/api/users")
public class Level2Controller {
    
    // GET - Buscar usuário específico
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok()
                        .eTag("\"" + user.getVersion() + "\"")
                        .body(user))
                .orElse(ResponseEntity.notFound().build());
    }
    
    // GET - Listar usuários com paginação
    @GetMapping
    public ResponseEntity<Page<User>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = userService.findAll(pageable, name);
        
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(users.getTotalElements()))
                .body(users);
    }
    
    // POST - Criar novo usuário
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        try {
            User newUser = userService.create(user);
            
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(newUser.getId())
                    .toUri();
            
            return ResponseEntity.created(location).body(newUser);
        } catch (EmailAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
    
    // PUT - Atualizar usuário completamente
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody User user,
            @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        
        try {
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
                    
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // PATCH - Atualizar usuário parcialmente
    @PatchMapping("/{id}")
    public ResponseEntity<User> patchUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        
        try {
            User updatedUser = userService.patch(id, updates);
            return ResponseEntity.ok(updatedUser);
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // DELETE - Remover usuário
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
    
    // HEAD - Verificar se usuário existe
    @RequestMapping(value = "/{id}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkUserExists(@PathVariable Long id) {
        if (userService.existsById(id)) {
            User user = userService.findById(id).get();
            return ResponseEntity.ok()
                    .eTag("\"" + user.getVersion() + "\"")
                    .build();
        }
        return ResponseEntity.notFound().build();
    }
}
```

### Recursos Aninhados (Nível 2):
```java
@RestController
@RequestMapping("/api/users/{userId}/orders")
public class UserOrdersController {
    
    @GetMapping
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable Long userId) {
        if (!userService.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }
        
        List<Order> orders = orderService.findByUserId(userId);
        return ResponseEntity.ok(orders);
    }
    
    @PostMapping
    public ResponseEntity<Order> createOrder(
            @PathVariable Long userId,
            @Valid @RequestBody Order order) {
        
        if (!userService.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }
        
        order.setUserId(userId);
        Order newOrder = orderService.create(order);
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newOrder.getId())
                .toUri();
        
        return ResponseEntity.created(location).body(newOrder);
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(
            @PathVariable Long userId,
            @PathVariable Long orderId) {
        
        return orderService.findByIdAndUserId(orderId, userId)
                .map(order -> ResponseEntity.ok(order))
                .orElse(ResponseEntity.notFound().build());
    }
}
```

### Benefícios do Nível 2:
- ✅ Semântica HTTP clara
- ✅ Códigos de status apropriados
- ✅ Cacheabilidade com GET
- ✅ Idempotência com PUT/DELETE
- ✅ Melhor experiência para desenvolvedores
- ✅ Facilita debugging e monitoramento

---

## Nível 3 - Controles Hipermídia (HATEOAS)

### Características:
- 🔗 **Hypermedia as the Engine of Application State**: HATEOAS
- 🧭 **Self-descriptive**: API fornece navegação
- 🔄 **Estado da aplicação**: Guiado por hipermídia
- 📱 **Descoberta dinâmica**: Cliente descobre ações disponíveis
- 🎯 **Baixo acoplamento**: Cliente não precisa conhecer URLs

### Exemplo Nível 3:
```json
GET /api/users/123

{
    "id": 123,
    "name": "João Silva",
    "email": "joao@email.com",
    "status": "active",
    "_links": {
        "self": {
            "href": "/api/users/123"
        },
        "edit": {
            "href": "/api/users/123",
            "method": "PUT"
        },
        "delete": {
            "href": "/api/users/123",
            "method": "DELETE"
        },
        "orders": {
            "href": "/api/users/123/orders"
        },
        "deactivate": {
            "href": "/api/users/123/deactivate",
            "method": "POST"
        }
    }
}
```

### Implementação Spring Boot (Nível 3) com Spring HATEOAS:
```java
// Dependência necessária
// implementation 'org.springframework.boot:spring-boot-starter-hateoas'

@RestController
@RequestMapping("/api/users")
public class Level3Controller {
    
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<User>> getUser(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> {
                    EntityModel<User> userModel = EntityModel.of(user);
                    
                    // Link para si mesmo
                    userModel.add(linkTo(methodOn(Level3Controller.class).getUser(id)).withSelfRel());
                    
                    // Link para editar
                    userModel.add(linkTo(methodOn(Level3Controller.class).updateUser(id, null, null))
                            .withRel("edit"));
                    
                    // Link para deletar
                    userModel.add(linkTo(methodOn(Level3Controller.class).deleteUser(id))
                            .withRel("delete"));
                    
                    // Link para pedidos
                    userModel.add(linkTo(methodOn(UserOrdersController.class).getUserOrders(id))
                            .withRel("orders"));
                    
                    // Links condicionais baseados no estado
                    if (user.getStatus() == UserStatus.ACTIVE) {
                        userModel.add(linkTo(methodOn(Level3Controller.class).deactivateUser(id))
                                .withRel("deactivate"));
                    } else {
                        userModel.add(linkTo(methodOn(Level3Controller.class).activateUser(id))
                                .withRel("activate"));
                    }
                    
                    return ResponseEntity.ok(userModel);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<User>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<User> usersPage = userService.findAll(pageable);
        
        List<EntityModel<User>> userModels = usersPage.getContent().stream()
                .map(user -> {
                    EntityModel<User> userModel = EntityModel.of(user);
                    userModel.add(linkTo(methodOn(Level3Controller.class).getUser(user.getId()))
                            .withSelfRel());
                    return userModel;
                })
                .collect(Collectors.toList());
        
        CollectionModel<EntityModel<User>> collectionModel = CollectionModel.of(userModels);
        
        // Link para si mesmo
        collectionModel.add(linkTo(methodOn(Level3Controller.class).getUsers(page, size))
                .withSelfRel());
        
        // Link para criar novo usuário
        collectionModel.add(linkTo(methodOn(Level3Controller.class).createUser(null))
                .withRel("create"));
        
        // Links de navegação
        if (page > 0) {
            collectionModel.add(linkTo(methodOn(Level3Controller.class).getUsers(page - 1, size))
                    .withRel("prev"));
        }
        
        if (page < usersPage.getTotalPages() - 1) {
            collectionModel.add(linkTo(methodOn(Level3Controller.class).getUsers(page + 1, size))
                    .withRel("next"));
        }
        
        collectionModel.add(linkTo(methodOn(Level3Controller.class).getUsers(0, size))
                .withRel("first"));
        
        collectionModel.add(linkTo(methodOn(Level3Controller.class)
                .getUsers(usersPage.getTotalPages() - 1, size))
                .withRel("last"));
        
        return ResponseEntity.ok(collectionModel);
    }
    
    @PostMapping
    public ResponseEntity<EntityModel<User>> createUser(@Valid @RequestBody User user) {
        try {
            User newUser = userService.create(user);
            
            EntityModel<User> userModel = EntityModel.of(newUser);
            userModel.add(linkTo(methodOn(Level3Controller.class).getUser(newUser.getId()))
                    .withSelfRel());
            
            URI location = linkTo(methodOn(Level3Controller.class).getUser(newUser.getId()))
                    .toUri();
            
            return ResponseEntity.created(location).body(userModel);
        } catch (EmailAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<User>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody User user,
            @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        
        try {
            User updatedUser = userService.update(id, user);
            
            EntityModel<User> userModel = EntityModel.of(updatedUser);
            userModel.add(linkTo(methodOn(Level3Controller.class).getUser(id)).withSelfRel());
            
            return ResponseEntity.ok()
                    .eTag("\"" + updatedUser.getVersion() + "\"")
                    .body(userModel);
                    
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Ações específicas com HATEOAS
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<EntityModel<User>> deactivateUser(@PathVariable Long id) {
        try {
            User user = userService.deactivate(id);
            
            EntityModel<User> userModel = EntityModel.of(user);
            userModel.add(linkTo(methodOn(Level3Controller.class).getUser(id)).withSelfRel());
            userModel.add(linkTo(methodOn(Level3Controller.class).activateUser(id))
                    .withRel("activate"));
            
            return ResponseEntity.ok(userModel);
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{id}/activate")
    public ResponseEntity<EntityModel<User>> activateUser(@PathVariable Long id) {
        try {
            User user = userService.activate(id);
            
            EntityModel<User> userModel = EntityModel.of(user);
            userModel.add(linkTo(methodOn(Level3Controller.class).getUser(id)).withSelfRel());
            userModel.add(linkTo(methodOn(Level3Controller.class).deactivateUser(id))
                    .withRel("deactivate"));
            
            return ResponseEntity.ok(userModel);
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

### Representador Customizado:
```java
@Component
public class UserRepresentationModelAssembler 
        implements RepresentationModelAssembler<User, EntityModel<User>> {
    
    @Override
    public EntityModel<User> toModel(User user) {
        EntityModel<User> userModel = EntityModel.of(user);
        
        // Link básicos
        userModel.add(linkTo(methodOn(Level3Controller.class).getUser(user.getId()))
                .withSelfRel());
        userModel.add(linkTo(Level3Controller.class).withRel("users"));
        
        // Links condicionais baseados no estado e permissões
        if (user.getStatus() == UserStatus.ACTIVE) {
            userModel.add(linkTo(methodOn(Level3Controller.class).deactivateUser(user.getId()))
                    .withRel("deactivate"));
        } else {
            userModel.add(linkTo(methodOn(Level3Controller.class).activateUser(user.getId()))
                    .withRel("activate"));
        }
        
        // Links para recursos relacionados
        userModel.add(linkTo(methodOn(UserOrdersController.class).getUserOrders(user.getId()))
                .withRel("orders"));
        
        return userModel;
    }
}
```

### Response de Erro com HATEOAS:
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<EntityModel<ErrorResponse>> handleUserNotFound(
            UserNotFoundException ex, HttpServletRequest request) {
        
        ErrorResponse error = new ErrorResponse(
                "User not found",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value()
        );
        
        EntityModel<ErrorResponse> errorModel = EntityModel.of(error);
        errorModel.add(linkTo(Level3Controller.class).withRel("users"));
        errorModel.add(Link.of(request.getRequestURL().toString()).withSelfRel());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorModel);
    }
}
```

### Benefícios do Nível 3:
- ✅ **Descoberta dinâmica**: Cliente descobre funcionalidades
- ✅ **Baixo acoplamento**: URLs podem mudar sem quebrar clientes
- ✅ **Estado da aplicação**: Guiado por hipermídia
- ✅ **Self-descriptive**: API documenta-se sozinha
- ✅ **Flexibilidade**: Facilita evolução da API
- ✅ **Experiência rica**: Cliente pode reagir ao estado

---

## Comparação dos Níveis

| Aspecto | Nível 0 | Nível 1 | Nível 2 | Nível 3 |
|---------|---------|---------|---------|---------|
| **Recursos** | ❌ Um endpoint | ✅ Múltiplos endpoints | ✅ Múltiplos endpoints | ✅ Múltiplos endpoints |
| **Verbos HTTP** | ❌ Apenas POST | ❌ Apenas POST | ✅ GET, POST, PUT, DELETE | ✅ Todos os verbos |
| **Status Codes** | ❌ Sempre 200 | ❌ Sempre 200 | ✅ Status apropriados | ✅ Status apropriados |
| **Cache** | ❌ Não funciona | ❌ Não funciona | ✅ GET cacheável | ✅ GET cacheável |
| **Idempotência** | ❌ Não garante | ❌ Não garante | ✅ PUT/DELETE idempotentes | ✅ PUT/DELETE idempotentes |
| **Hipermídia** | ❌ Sem links | ❌ Sem links | ❌ Sem links | ✅ HATEOAS completo |
| **Descoberta** | ❌ Manual | ❌ Manual | ❌ Manual | ✅ Dinâmica |
| **Acoplamento** | ❌ Alto | ❌ Alto | ⚠️ Médio | ✅ Baixo |

---

## Evolução Gradual

### Estratégia de Migração:

#### 1. **Do Nível 0 para Nível 1**:
```java
// Antes (Nível 0)
@PostMapping("/api/service")
public ResponseEntity<?> handleAll(@RequestBody Map<String, Object> request) {
    // Tudo em um método
}

// Depois (Nível 1)
@PostMapping("/api/users/create")
public ResponseEntity<User> createUser(@RequestBody User user) { }

@PostMapping("/api/users/get")
public ResponseEntity<User> getUser(@RequestBody Map<String, Long> request) { }
```

#### 2. **Do Nível 1 para Nível 2**:
```java
// Antes (Nível 1)
@PostMapping("/api/users/get")
public ResponseEntity<User> getUser(@RequestBody Map<String, Long> request) { }

// Depois (Nível 2)
@GetMapping("/api/users/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) { }
```

#### 3. **Do Nível 2 para Nível 3**:
```java
// Antes (Nível 2)
@GetMapping("/api/users/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    User user = userService.findById(id);
    return ResponseEntity.ok(user);
}

// Depois (Nível 3)
@GetMapping("/api/users/{id}")
public ResponseEntity<EntityModel<User>> getUser(@PathVariable Long id) {
    User user = userService.findById(id);
    EntityModel<User> userModel = userRepresentationAssembler.toModel(user);
    return ResponseEntity.ok(userModel);
}
```

---

## Ferramentas e Bibliotecas

### Spring HATEOAS:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-hateoas</artifactId>
</dependency>
```

### HAL Browser para desenvolvimento:
```xml
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-rest-hal-browser</artifactId>
</dependency>
```

### Configuração HATEOAS:
```java
@Configuration
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
public class HateoasConfig {
    
    @Bean
    public CurieProvider curieProvider() {
        return new DefaultCurieProvider("ex", UriTemplate.of("/docs/reference.html#resources-{rel}"));
    }
}
```

---

## Boas Práticas por Nível

### **Nível 1** - Organize por Recursos:
```java
/api/users/*          // Operações de usuários
/api/orders/*         // Operações de pedidos
/api/products/*       // Operações de produtos
```

### **Nível 2** - Use Verbos e Status Corretos:
```java
GET    /api/users     → 200 OK
POST   /api/users     → 201 Created
PUT    /api/users/123 → 200 OK ou 204 No Content
DELETE /api/users/123 → 204 No Content
```

### **Nível 3** - Implemente HATEOAS Gradualmente:
1. Comece com links básicos (self, edit, delete)
2. Adicione navegação (next, prev, first, last)
3. Inclua ações condicionais baseadas no estado
4. Documente o formato de hipermídia

---

## Quando Usar Cada Nível

### **Nível 0/1**: 
- ❌ **Evitar**: Não é realmente REST
- 🔧 **Legacy**: Sistemas legados em migração

### **Nível 2**: 
- ✅ **Recomendado**: Para a maioria das APIs
- 🎯 **Prático**: Bom equilíbrio entre funcionalidade e complexidade
- 💼 **Empresarial**: Amplamente aceito na indústria

### **Nível 3**: 
- ⭐ **Ideal**: Para APIs públicas complexas
- 🚀 **Avançado**: Quando baixo acoplamento é crucial
- 🎓 **Acadêmico**: Implementação completa dos princípios REST

---

## Conclusão

O **Modelo de Maturidade Richardson** fornece um roteiro claro para evoluir APIs em direção aos princípios REST verdadeiros:

- **Nível 0**: HTTP como túnel (não REST)
- **Nível 1**: Múltiplos recursos (melhor organização)
- **Nível 2**: Verbos HTTP + Status codes (REST prático)
- **Nível 3**: HATEOAS (REST completo)

A maioria das APIs **Nível 2** já são consideradas RESTful e atendem bem às necessidades empresariais. O **Nível 3** oferece benefícios adicionais de descoberta e baixo acoplamento, mas adiciona complexidade que nem sempre é necessária.

A escolha do nível adequado deve considerar:
- 🎯 **Público-alvo** da API
- 🔧 **Complexidade** aceitável
- 📈 **Necessidades futuras** de evolução
- 🧩 **Acoplamento** desejado entre cliente e servidor
