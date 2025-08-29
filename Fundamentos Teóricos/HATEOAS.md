# HATEOAS - Hypermedia as the Engine of Application State

Este documento apresenta o conceito de **HATEOAS** (Hypermedia as the Engine of Application State), um dos principais componentes do REST que permite criar APIs verdadeiramente auto-descritivas e desacopladas.

## O que é HATEOAS?

**HATEOAS** é um princípio da arquitetura REST que determina que a aplicação deve fornecer informações dinamicamente sobre quais ações estão disponíveis para o cliente através de links hipermídia incorporados nas respostas.

### Conceitos Fundamentais:
- 🔗 **Hipermídia**: Links que conectam recursos relacionados
- 🧭 **Navegação dinâmica**: Cliente descobre funcionalidades em tempo de execução
- 🔄 **Estado da aplicação**: Controlado através de links hipermídia
- 📱 **Auto-descoberta**: API se documenta sozinha
- ⚡ **Baixo acoplamento**: Cliente não precisa conhecer URLs hardcoded

---

## Por que usar HATEOAS?

### Problemas Sem HATEOAS:
```java
// Cliente hardcoded (problemático)
public class UserClient {
    private static final String BASE_URL = "https://api.example.com";
    
    public void updateUser(Long userId, User user) {
        // URL hardcoded - alto acoplamento
        String url = BASE_URL + "/users/" + userId;
        restTemplate.put(url, user);
        
        // Cliente precisa "saber" que pode fazer isso
        if (user.isActive()) {
            String deactivateUrl = BASE_URL + "/users/" + userId + "/deactivate";
            // Mais acoplamento...
        }
    }
}
```

### Benefícios Com HATEOAS:
```java
// Cliente dinâmico (flexível)
public class HateoasUserClient {
    
    public void updateUser(String userUrl, User user) {
        // URL vem da própria API
        UserResponse response = restTemplate.getForObject(userUrl, UserResponse.class);
        
        // Cliente descobre ações disponíveis
        if (response.hasLink("edit")) {
            restTemplate.put(response.getLink("edit").getHref(), user);
        }
        
        // Ações condicionais descobertas dinamicamente
        if (response.hasLink("deactivate")) {
            // Sabe que pode desativar
        }
    }
}
```

---

## Formatos de Hipermídia

### 1. HAL (Hypertext Application Language)

**HAL** é o formato mais popular para HATEOAS, usando `_links` para representar links.

```json
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
            "method": "POST",
            "title": "Deactivate User"
        }
    },
    "_embedded": {
        "orders": [
            {
                "id": 456,
                "total": 150.00,
                "_links": {
                    "self": { "href": "/api/orders/456" }
                }
            }
        ]
    }
}
```

### 2. JSON-LD (JSON for Linked Data)

```json
{
    "@context": "https://api.example.com/contexts/user",
    "@id": "/api/users/123",
    "@type": "User",
    "id": 123,
    "name": "João Silva",
    "email": "joao@email.com",
    "orders": {
        "@id": "/api/users/123/orders",
        "@type": "OrderCollection"
    }
}
```

### 3. Collection+JSON

```json
{
    "collection": {
        "version": "1.0",
        "href": "/api/users",
        "items": [
            {
                "href": "/api/users/123",
                "data": [
                    {"name": "id", "value": 123},
                    {"name": "name", "value": "João Silva"}
                ],
                "links": [
                    {"rel": "edit", "href": "/api/users/123"}
                ]
            }
        ],
        "links": [
            {"rel": "create", "href": "/api/users", "prompt": "Create new user"}
        ]
    }
}
```

---

## Implementação com Spring HATEOAS

### Configuração Inicial:

#### Dependência Maven:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-hateoas</artifactId>
</dependency>
```

#### Dependência Gradle:
```gradle
implementation 'org.springframework.boot:spring-boot-starter-hateoas'
```

#### Configuração:
```java
@Configuration
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
public class HateoasConfig {
    
    @Bean
    public CurieProvider curieProvider() {
        return new DefaultCurieProvider("doc", 
            UriTemplate.of("/docs/reference.html#resources-{rel}"));
    }
}
```

---

## Implementação Básica

### Controller com HATEOAS:
```java
@RestController
@RequestMapping("/api/users")
public class UserHateoasController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepresentationModelAssembler userAssembler;
    
    // Buscar usuário individual
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<User>> getUser(@PathVariable Long id) {
        return userService.findById(id)
                .map(userAssembler::toModel)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Listar usuários
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<User>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<User> usersPage = userService.findAll(pageable);
        
        CollectionModel<EntityModel<User>> collectionModel = 
                userAssembler.toCollectionModel(usersPage.getContent());
        
        // Adicionar links de navegação
        addNavigationLinks(collectionModel, usersPage, page, size);
        
        return ResponseEntity.ok(collectionModel);
    }
    
    // Criar usuário
    @PostMapping
    public ResponseEntity<EntityModel<User>> createUser(@Valid @RequestBody User user) {
        User newUser = userService.create(user);
        EntityModel<User> userModel = userAssembler.toModel(newUser);
        
        return ResponseEntity
                .created(userModel.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(userModel);
    }
    
    // Atualizar usuário
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<User>> updateUser(
            @PathVariable Long id, 
            @Valid @RequestBody User user) {
        
        try {
            User updatedUser = userService.update(id, user);
            EntityModel<User> userModel = userAssembler.toModel(updatedUser);
            
            return ResponseEntity.ok(userModel);
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Deletar usuário
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Ações específicas
    @PostMapping("/{id}/activate")
    public ResponseEntity<EntityModel<User>> activateUser(@PathVariable Long id) {
        try {
            User user = userService.activate(id);
            EntityModel<User> userModel = userAssembler.toModel(user);
            return ResponseEntity.ok(userModel);
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<EntityModel<User>> deactivateUser(@PathVariable Long id) {
        try {
            User user = userService.deactivate(id);
            EntityModel<User> userModel = userAssembler.toModel(user);
            return ResponseEntity.ok(userModel);
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    private void addNavigationLinks(CollectionModel<EntityModel<User>> collectionModel, 
                                   Page<User> page, int currentPage, int size) {
        
        // Link para si mesmo
        collectionModel.add(linkTo(methodOn(UserHateoasController.class)
                .getUsers(currentPage, size)).withSelfRel());
        
        // Link para criar
        collectionModel.add(linkTo(methodOn(UserHateoasController.class)
                .createUser(null)).withRel("create"));
        
        // Links de navegação
        if (currentPage > 0) {
            collectionModel.add(linkTo(methodOn(UserHateoasController.class)
                    .getUsers(currentPage - 1, size)).withRel(IanaLinkRelations.PREV));
        }
        
        if (currentPage < page.getTotalPages() - 1) {
            collectionModel.add(linkTo(methodOn(UserHateoasController.class)
                    .getUsers(currentPage + 1, size)).withRel(IanaLinkRelations.NEXT));
        }
        
        collectionModel.add(linkTo(methodOn(UserHateoasController.class)
                .getUsers(0, size)).withRel(IanaLinkRelations.FIRST));
        
        collectionModel.add(linkTo(methodOn(UserHateoasController.class)
                .getUsers(page.getTotalPages() - 1, size)).withRel(IanaLinkRelations.LAST));
    }
}
```

---

## RepresentationModelAssembler

### Assembler Básico:
```java
@Component
public class UserRepresentationModelAssembler 
        implements RepresentationModelAssembler<User, EntityModel<User>> {
    
    @Override
    public EntityModel<User> toModel(User user) {
        EntityModel<User> userModel = EntityModel.of(user);
        
        // Links básicos
        userModel.add(linkTo(methodOn(UserHateoasController.class)
                .getUser(user.getId())).withSelfRel());
        
        userModel.add(linkTo(UserHateoasController.class).withRel("users"));
        
        // Links condicionais baseados no estado
        addConditionalLinks(userModel, user);
        
        // Links para recursos relacionados
        addRelatedResourceLinks(userModel, user);
        
        return userModel;
    }
    
    private void addConditionalLinks(EntityModel<User> userModel, User user) {
        // Links baseados no status do usuário
        if (user.getStatus() == UserStatus.ACTIVE) {
            userModel.add(linkTo(methodOn(UserHateoasController.class)
                    .deactivateUser(user.getId())).withRel("deactivate"));
        } else if (user.getStatus() == UserStatus.INACTIVE) {
            userModel.add(linkTo(methodOn(UserHateoasController.class)
                    .activateUser(user.getId())).withRel("activate"));
        }
        
        // Links baseados em permissões
        if (hasPermissionToEdit(user)) {
            userModel.add(linkTo(methodOn(UserHateoasController.class)
                    .updateUser(user.getId(), null)).withRel("edit"));
        }
        
        if (hasPermissionToDelete(user)) {
            userModel.add(linkTo(methodOn(UserHateoasController.class)
                    .deleteUser(user.getId())).withRel("delete"));
        }
    }
    
    private void addRelatedResourceLinks(EntityModel<User> userModel, User user) {
        // Links para recursos relacionados
        userModel.add(linkTo(methodOn(OrderController.class)
                .getUserOrders(user.getId())).withRel("orders"));
        
        userModel.add(linkTo(methodOn(ProfileController.class)
                .getUserProfile(user.getId())).withRel("profile"));
        
        // Links condicionais para recursos relacionados
        if (user.hasActiveSubscription()) {
            userModel.add(linkTo(methodOn(SubscriptionController.class)
                    .getUserSubscription(user.getId())).withRel("subscription"));
        }
    }
    
    private boolean hasPermissionToEdit(User user) {
        // Lógica de autorização
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && (
            auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")) ||
            auth.getName().equals(user.getEmail())
        );
    }
    
    private boolean hasPermissionToDelete(User user) {
        // Apenas admins podem deletar
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && 
               auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
```

### Assembler Avançado com Templates:
```java
@Component
public class AdvancedUserRepresentationModelAssembler 
        implements RepresentationModelAssembler<User, EntityModel<User>> {
    
    @Override
    public EntityModel<User> toModel(User user) {
        EntityModel<User> userModel = EntityModel.of(user);
        
        // Link self com template
        userModel.add(linkTo(methodOn(UserHateoasController.class)
                .getUser(user.getId())).withSelfRel());
        
        // Link com template para busca
        Link searchLink = linkTo(UserHateoasController.class)
                .slash("search{?name,email,status}")
                .withRel("search");
        userModel.add(searchLink);
        
        // Links com affordances (formulários)
        addAffordances(userModel, user);
        
        // Links com metadados
        addLinksWithMetadata(userModel, user);
        
        return userModel;
    }
    
    private void addAffordances(EntityModel<User> userModel, User user) {
        // Affordance para edição (inclui esquema do formulário)
        userModel.add(linkTo(methodOn(UserHateoasController.class)
                .updateUser(user.getId(), null))
                .withRel("edit")
                .andAffordance(afford(methodOn(UserHateoasController.class)
                        .updateUser(user.getId(), null))));
        
        // Affordance para criação de pedido
        userModel.add(linkTo(methodOn(OrderController.class)
                .createOrder(user.getId(), null))
                .withRel("create-order")
                .andAffordance(afford(methodOn(OrderController.class)
                        .createOrder(user.getId(), null))));
    }
    
    private void addLinksWithMetadata(EntityModel<User> userModel, User user) {
        // Link com título e tipo
        userModel.add(linkTo(methodOn(UserHateoasController.class)
                .getUser(user.getId()))
                .withRel("profile")
                .withTitle("User Profile")
                .withType("application/hal+json"));
        
        // Link com hreflang para i18n
        userModel.add(linkTo(methodOn(UserHateoasController.class)
                .getUser(user.getId()))
                .withRel("alternate")
                .withHreflang("pt-BR"));
    }
}
```

---

## Recursos Relacionados e Embarcados

### Controller para Recursos Aninhados:
```java
@RestController
@RequestMapping("/api/users/{userId}/orders")
public class UserOrdersHateoasController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private OrderRepresentationModelAssembler orderAssembler;
    
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<Order>>> getUserOrders(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (!userService.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> ordersPage = orderService.findByUserId(userId, pageable);
        
        CollectionModel<EntityModel<Order>> collectionModel = 
                orderAssembler.toCollectionModel(ordersPage.getContent());
        
        // Link para o usuário pai
        collectionModel.add(linkTo(methodOn(UserHateoasController.class)
                .getUser(userId)).withRel("user"));
        
        // Link para si mesmo
        collectionModel.add(linkTo(methodOn(UserOrdersHateoasController.class)
                .getUserOrders(userId, page, size)).withSelfRel());
        
        // Link para criar novo pedido
        collectionModel.add(linkTo(methodOn(UserOrdersHateoasController.class)
                .createOrder(userId, null)).withRel("create"));
        
        return ResponseEntity.ok(collectionModel);
    }
    
    @PostMapping
    public ResponseEntity<EntityModel<Order>> createOrder(
            @PathVariable Long userId,
            @Valid @RequestBody Order order) {
        
        if (!userService.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }
        
        order.setUserId(userId);
        Order newOrder = orderService.create(order);
        EntityModel<Order> orderModel = orderAssembler.toModel(newOrder);
        
        return ResponseEntity
                .created(orderModel.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(orderModel);
    }
}
```

### Recursos Embarcados:
```java
@Component
public class UserWithOrdersAssembler 
        implements RepresentationModelAssembler<User, EntityModel<User>> {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private OrderRepresentationModelAssembler orderAssembler;
    
    @Override
    public EntityModel<User> toModel(User user) {
        EntityModel<User> userModel = EntityModel.of(user);
        
        // Links básicos
        userModel.add(linkTo(methodOn(UserHateoasController.class)
                .getUser(user.getId())).withSelfRel());
        
        // Embarcar pedidos recentes
        List<Order> recentOrders = orderService.findRecentByUserId(user.getId(), 5);
        if (!recentOrders.isEmpty()) {
            CollectionModel<EntityModel<Order>> ordersModel = 
                    orderAssembler.toCollectionModel(recentOrders);
            
            // Adicionar como embedded
            userModel.add(linkTo(methodOn(UserOrdersHateoasController.class)
                    .getUserOrders(user.getId(), 0, 10)).withRel("orders"));
        }
        
        return userModel;
    }
}
```

---

## Paginação com HATEOAS

### Controller com Paginação:
```java
@GetMapping
public ResponseEntity<PagedModel<EntityModel<User>>> getUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction) {
    
    Sort.Direction sortDirection = Sort.Direction.fromString(direction);
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
    
    Page<User> usersPage = userService.findAll(pageable);
    
    PagedModel<EntityModel<User>> pagedModel = pagedResourcesAssembler
            .toModel(usersPage, userAssembler);
    
    // Adicionar links customizados
    pagedModel.add(linkTo(methodOn(UserHateoasController.class)
            .createUser(null)).withRel("create"));
    
    return ResponseEntity.ok(pagedModel);
}
```

### PagedResourcesAssembler:
```java
@Configuration
public class PaginationConfig {
    
    @Bean
    public PagedResourcesAssembler<User> pagedResourcesAssembler() {
        return new PagedResourcesAssembler<>(null, null);
    }
}
```

---

## Tratamento de Erros com HATEOAS

### Error Response com Links:
```java
@Data
@AllArgsConstructor
public class ErrorResponse extends RepresentationModel<ErrorResponse> {
    private String error;
    private String message;
    private int status;
    private LocalDateTime timestamp;
    private String path;
}

@ControllerAdvice
public class HateoasExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex, 
            HttpServletRequest request) {
        
        ErrorResponse error = new ErrorResponse(
                "User Not Found",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
        
        // Adicionar links úteis para recuperação
        error.add(linkTo(UserHateoasController.class).withRel("users"));
        error.add(linkTo(methodOn(UserHateoasController.class)
                .createUser(null)).withRel("create-user"));
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            ValidationException ex,
            HttpServletRequest request) {
        
        ErrorResponse error = new ErrorResponse(
                "Validation Error",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
        
        // Links para documentação
        error.add(Link.of("/docs/validation").withRel("documentation"));
        error.add(Link.of("/docs/user-schema").withRel("schema"));
        
        return ResponseEntity.badRequest().body(error);
    }
}
```

---

## Cliente HATEOAS

### Cliente Java:
```java
@Service
public class HateoasUserClient {
    
    private final RestTemplate restTemplate;
    
    public HateoasUserClient() {
        this.restTemplate = new RestTemplate();
        // Configurar para HAL
        restTemplate.getMessageConverters()
                .add(new MappingJackson2HttpMessageConverter());
    }
    
    public void navigateAndUpdateUser(String entryPointUrl) {
        // 1. Descobrir API a partir do entry point
        ResponseEntity<EntityModel<ApiRoot>> rootResponse = 
                restTemplate.exchange(entryPointUrl, HttpMethod.GET, 
                        null, new ParameterizedTypeReference<EntityModel<ApiRoot>>() {});
        
        EntityModel<ApiRoot> root = rootResponse.getBody();
        
        // 2. Navegar para usuários
        if (root.hasLink("users")) {
            String usersUrl = root.getRequiredLink("users").getHref();
            
            ResponseEntity<CollectionModel<EntityModel<User>>> usersResponse = 
                    restTemplate.exchange(usersUrl, HttpMethod.GET, 
                            null, new ParameterizedTypeReference<CollectionModel<EntityModel<User>>>() {});
            
            CollectionModel<EntityModel<User>> users = usersResponse.getBody();
            
            // 3. Trabalhar com primeiro usuário
            if (!users.getContent().isEmpty()) {
                EntityModel<User> firstUser = users.getContent().iterator().next();
                
                // 4. Verificar se pode editar
                if (firstUser.hasLink("edit")) {
                    User userData = firstUser.getContent();
                    userData.setName("Nome Atualizado");
                    
                    String editUrl = firstUser.getRequiredLink("edit").getHref();
                    restTemplate.put(editUrl, userData);
                }
                
                // 5. Verificar ações disponíveis
                if (firstUser.hasLink("deactivate")) {
                    String deactivateUrl = firstUser.getRequiredLink("deactivate").getHref();
                    restTemplate.postForEntity(deactivateUrl, null, Void.class);
                }
            }
        }
    }
}
```

### Cliente JavaScript:
```javascript
class HateoasClient {
    constructor(baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    async navigateAndUpdate() {
        try {
            // 1. Descobrir API
            const rootResponse = await fetch(`${this.baseUrl}/api`);
            const root = await rootResponse.json();
            
            // 2. Navegar para usuários
            if (root._links && root._links.users) {
                const usersResponse = await fetch(root._links.users.href);
                const users = await usersResponse.json();
                
                // 3. Trabalhar com primeiro usuário
                if (users._embedded && users._embedded.users.length > 0) {
                    const firstUser = users._embedded.users[0];
                    
                    // 4. Verificar se pode editar
                    if (firstUser._links && firstUser._links.edit) {
                        const updatedUser = {
                            ...firstUser,
                            name: "Nome Atualizado"
                        };
                        
                        await fetch(firstUser._links.edit.href, {
                            method: 'PUT',
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify(updatedUser)
                        });
                    }
                    
                    // 5. Verificar ações disponíveis
                    if (firstUser._links && firstUser._links.deactivate) {
                        await fetch(firstUser._links.deactivate.href, {
                            method: 'POST'
                        });
                    }
                }
            }
        } catch (error) {
            console.error('Erro na navegação HATEOAS:', error);
        }
    }
}
```

---

## Ferramentas de Desenvolvimento

### HAL Browser:
```xml
<!-- Adicionar dependência para desenvolvimento -->
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-rest-hal-browser</artifactId>
    <scope>runtime</scope>
</dependency>
```

Acesse: `http://localhost:8080/browser/index.html`

### HAL Explorer:
```xml
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-rest-hal-explorer</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Configuração de Media Types:
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .favorParameter(false)
                .ignoreAcceptHeader(false)
                .defaultContentType(MediaTypes.HAL_JSON)
                .mediaType("hal+json", MediaTypes.HAL_JSON)
                .mediaType("json", MediaType.APPLICATION_JSON);
    }
}
```

---

## Documentação HATEOAS

### OpenAPI com HATEOAS:
```java
@RestController
@Tag(name = "Users", description = "User management API")
public class UserHateoasController {
    
    @GetMapping("/{id}")
    @Operation(
            summary = "Get user by ID",
            description = "Returns a user with hypermedia links"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User found",
                    content = @Content(
                            mediaType = "application/hal+json",
                            schema = @Schema(implementation = EntityModel.class),
                            examples = @ExampleObject(
                                    name = "User with HATEOAS",
                                    value = """
                                    {
                                        "id": 123,
                                        "name": "João Silva",
                                        "_links": {
                                            "self": {"href": "/api/users/123"},
                                            "edit": {"href": "/api/users/123"},
                                            "orders": {"href": "/api/users/123/orders"}
                                        }
                                    }
                                    """
                            )
                    )
            )
    })
    public ResponseEntity<EntityModel<User>> getUser(@PathVariable Long id) {
        // implementação
    }
}
```

### Profile Links:
```java
@RestController
@RequestMapping("/api")
public class ProfileController {
    
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile() {
        Map<String, Object> profile = new HashMap<>();
        
        profile.put("title", "User Management API");
        profile.put("version", "1.0");
        
        Map<String, Object> links = new HashMap<>();
        links.put("users", Map.of(
                "href", "/api/users",
                "title", "User Collection",
                "doc", "/docs/users"
        ));
        
        profile.put("_links", links);
        
        return ResponseEntity.ok(profile);
    }
}
```

---

## Testes para HATEOAS

### Testes de Integração:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserHateoasControllerTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldReturnUserWithHateoasLinks() {
        // Given
        User user = createTestUser();
        
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/users/" + user.getId(), String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        JsonNode jsonNode = JsonPath.parse(response.getBody()).read("$", JsonNode.class);
        
        // Verificar dados do usuário
        assertThat(jsonNode.get("id").asLong()).isEqualTo(user.getId());
        assertThat(jsonNode.get("name").asText()).isEqualTo(user.getName());
        
        // Verificar links HATEOAS
        JsonNode links = jsonNode.get("_links");
        assertThat(links).isNotNull();
        assertThat(links.has("self")).isTrue();
        assertThat(links.has("edit")).isTrue();
        assertThat(links.has("orders")).isTrue();
        
        // Verificar URLs dos links
        assertThat(links.get("self").get("href").asText())
                .endsWith("/api/users/" + user.getId());
    }
    
    @Test
    void shouldIncludeConditionalLinks() {
        // Given
        User activeUser = createActiveUser();
        User inactiveUser = createInactiveUser();
        
        // When
        ResponseEntity<String> activeResponse = restTemplate.getForEntity(
                "/api/users/" + activeUser.getId(), String.class);
        ResponseEntity<String> inactiveResponse = restTemplate.getForEntity(
                "/api/users/" + inactiveUser.getId(), String.class);
        
        // Then
        JsonNode activeLinks = JsonPath.parse(activeResponse.getBody())
                .read("$._links", JsonNode.class);
        JsonNode inactiveLinks = JsonPath.parse(inactiveResponse.getBody())
                .read("$._links", JsonNode.class);
        
        // Usuário ativo deve ter link para desativar
        assertThat(activeLinks.has("deactivate")).isTrue();
        assertThat(activeLinks.has("activate")).isFalse();
        
        // Usuário inativo deve ter link para ativar
        assertThat(inactiveLinks.has("activate")).isTrue();
        assertThat(inactiveLinks.has("deactivate")).isFalse();
    }
    
    @Test
    void shouldNavigateUsingHateoasLinks() {
        // Given
        User user = createTestUser();
        
        // When - Buscar usuário
        ResponseEntity<String> userResponse = restTemplate.getForEntity(
                "/api/users/" + user.getId(), String.class);
        
        // Extrair link para pedidos
        String ordersLink = JsonPath.parse(userResponse.getBody())
                .read("$._links.orders.href", String.class);
        
        // Navegar para pedidos usando o link
        ResponseEntity<String> ordersResponse = restTemplate.getForEntity(
                ordersLink, String.class);
        
        // Then
        assertThat(ordersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        JsonNode ordersJson = JsonPath.parse(ordersResponse.getBody()).read("$", JsonNode.class);
        assertThat(ordersJson.has("_links")).isTrue();
        assertThat(ordersJson.has("_embedded")).isTrue();
    }
}
```

### Testes Unitários para Assemblers:
```java
class UserRepresentationModelAssemblerTest {
    
    private UserRepresentationModelAssembler assembler;
    
    @BeforeEach
    void setUp() {
        assembler = new UserRepresentationModelAssembler();
    }
    
    @Test
    void shouldAddBasicLinksToUser() {
        // Given
        User user = User.builder()
                .id(123L)
                .name("João Silva")
                .status(UserStatus.ACTIVE)
                .build();
        
        // When
        EntityModel<User> userModel = assembler.toModel(user);
        
        // Then
        assertThat(userModel.hasLink(IanaLinkRelations.SELF)).isTrue();
        assertThat(userModel.getRequiredLink(IanaLinkRelations.SELF).getHref())
                .endsWith("/api/users/123");
        
        assertThat(userModel.hasLink("users")).isTrue();
        assertThat(userModel.hasLink("orders")).isTrue();
    }
    
    @Test
    void shouldAddConditionalLinksBasedOnUserStatus() {
        // Given
        User activeUser = User.builder()
                .id(123L)
                .status(UserStatus.ACTIVE)
                .build();
        
        User inactiveUser = User.builder()
                .id(456L)
                .status(UserStatus.INACTIVE)
                .build();
        
        // When
        EntityModel<User> activeModel = assembler.toModel(activeUser);
        EntityModel<User> inactiveModel = assembler.toModel(inactiveUser);
        
        // Then
        assertThat(activeModel.hasLink("deactivate")).isTrue();
        assertThat(activeModel.hasLink("activate")).isFalse();
        
        assertThat(inactiveModel.hasLink("activate")).isTrue();
        assertThat(inactiveModel.hasLink("deactivate")).isFalse();
    }
}
```

---

## Boas Práticas HATEOAS

### 1. **Consistência nos Nomes de Relações**:
```java
public class LinkRelations {
    public static final IanaLinkRelation USERS = IanaLinkRelation.of("users");
    public static final IanaLinkRelation ORDERS = IanaLinkRelation.of("orders");
    public static final IanaLinkRelation ACTIVATE = IanaLinkRelation.of("activate");
    public static final IanaLinkRelation DEACTIVATE = IanaLinkRelation.of("deactivate");
}
```

### 2. **Links Condicionais Inteligentes**:
```java
private void addActionLinks(EntityModel<User> userModel, User user) {
    // Baseado no estado do objeto
    if (user.canBeDeleted()) {
        userModel.add(linkTo(methodOn(UserController.class)
                .deleteUser(user.getId())).withRel("delete"));
    }
    
    // Baseado em permissões do usuário atual
    if (securityService.canEdit(user)) {
        userModel.add(linkTo(methodOn(UserController.class)
                .updateUser(user.getId(), null)).withRel("edit"));
    }
    
    // Baseado em regras de negócio
    if (businessRuleService.canCreateOrder(user)) {
        userModel.add(linkTo(methodOn(OrderController.class)
                .createOrder(user.getId(), null)).withRel("create-order"));
    }
}
```

### 3. **Navegação Rica**:
```java
private void addRichNavigation(CollectionModel<EntityModel<User>> collection, 
                              Page<User> page) {
    // Links básicos de paginação
    addPaginationLinks(collection, page);
    
    // Links de ordenação
    collection.add(linkTo(methodOn(UserController.class)
            .getUsers(page.getNumber(), page.getSize(), "name", "asc"))
            .withRel("sort-by-name"));
    
    // Links de filtro
    collection.add(linkTo(methodOn(UserController.class)
            .getUsersByStatus("ACTIVE"))
            .withRel("filter-active"));
    
    // Links de ação em lote
    collection.add(linkTo(methodOn(UserController.class)
            .bulkDeactivate(null))
            .withRel("bulk-deactivate"));
}
```

### 4. **Templates e Affordances**:
```java
private void addAffordances(EntityModel<User> userModel, User user) {
    // Link com template para busca
    Link searchTemplate = linkTo(UserController.class)
            .slash("search{?name,email,status,page,size}")
            .withRel("search");
    userModel.add(searchTemplate);
    
    // Affordance com schema do formulário
    userModel.add(linkTo(methodOn(UserController.class)
            .updateUser(user.getId(), null))
            .withRel("edit")
            .andAffordance(afford(methodOn(UserController.class)
                    .updateUser(user.getId(), null)))
            .withTitle("Update User")
            .withType("application/json"));
}
```

### 5. **Versionamento com HATEOAS**:
```java
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller {
    
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<User>> getUser(@PathVariable Long id) {
        EntityModel<User> userModel = userAssembler.toModel(user);
        
        // Link para versão mais recente
        userModel.add(linkTo(methodOn(UserV2Controller.class)
                .getUser(id)).withRel("latest"));
        
        // Link para versão anterior (se existir)
        userModel.add(linkTo(methodOn(UserV0Controller.class)
                .getUser(id)).withRel("previous"));
        
        return ResponseEntity.ok(userModel);
    }
}
```

---

## Conclusão

**HATEOAS** é o nível mais avançado do REST que permite criar APIs verdadeiramente auto-descritivas e desacopladas. Principais benefícios:

### ✅ **Benefícios**:
- **Descoberta dinâmica**: Cliente descobre funcionalidades
- **Baixo acoplamento**: URLs podem mudar sem quebrar clientes
- **Self-describing**: API documenta-se sozinha
- **Flexibilidade**: Facilita evolução da API
- **Experiência rica**: Navegação intuitiva

### ⚠️ **Considerações**:
- **Complexidade**: Aumenta a complexidade da implementação
- **Performance**: Mais dados trafegados (links)
- **Curva de aprendizado**: Conceito avançado
- **Tooling**: Nem todas as ferramentas suportam bem

### 🎯 **Quando Usar**:
- **APIs públicas complexas**: Quando evolução é importante
- **Sistemas distribuídos**: Para reduzir acoplamento
- **Interfaces ricas**: Quando cliente precisa descobrir funcionalidades
- **Longo prazo**: APIs que precisam evoluir sem quebrar clientes

HATEOAS representa o estado da arte em design de APIs REST, mas deve ser usado considerando o contexto e as necessidades específicas do projeto.
