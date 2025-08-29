# Tipos de Parâmetros em REST APIs

## 1. Path Parameters (Parâmetros de Caminho)

Os **Path Parameters** são valores que fazem parte da própria URL, representando recursos específicos na hierarquia da API.

### Características:
- São obrigatórios
- Fazem parte da estrutura da URL
- Identificam recursos específicos
- Não podem ser nulos

### Sintaxe:
```
/api/users/{userId}
/api/products/{productId}/reviews/{reviewId}
```

### Exemplos:
```http
GET /api/users/123
DELETE /api/products/456
PUT /api/orders/789/items/101
```

### Implementação Spring Boot:
```java
@GetMapping("/users/{id}")
public User getUserById(@PathVariable Long id) {
    return userService.findById(id);
}

@GetMapping("/products/{productId}/reviews/{reviewId}")
public Review getReview(
    @PathVariable Long productId, 
    @PathVariable Long reviewId
) {
    return reviewService.findByProductAndId(productId, reviewId);
}
```

## 2. Query Parameters (Parâmetros de Consulta)

Os **Query Parameters** são utilizados para filtrar, ordenar, paginar ou configurar a resposta da API.

### Características:
- São opcionais (geralmente)
- Aparecem após o `?` na URL
- Separados por `&`
- Úteis para filtros e configurações

### Sintaxe:
```
/api/users?page=1&size=10&sort=name
/api/products?category=electronics&minPrice=100&maxPrice=500
```

### Exemplos:
```http
GET /api/users?page=0&size=20&sort=name,asc
GET /api/products?category=books&author=tolkien
GET /api/orders?status=pending&date=2024-01-01
```

### Implementação Spring Boot:
```java
@GetMapping("/users")
public Page<User> getUsers(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(required = false) String name
) {
    return userService.findUsers(page, size, name);
}

@GetMapping("/products")
public List<Product> getProducts(
    @RequestParam(required = false) String category,
    @RequestParam(required = false) BigDecimal minPrice,
    @RequestParam(required = false) BigDecimal maxPrice
) {
    return productService.findProducts(category, minPrice, maxPrice);
}
```

## 3. Request Body (Corpo da Requisição)

O **Request Body** contém os dados que serão enviados para o servidor, geralmente em formato JSON.

### Características:
- Usado principalmente em POST, PUT e PATCH
- Contém dados estruturados (JSON, XML, etc.)
- Pode ser validado
- Suporta objetos complexos

### Exemplos:
```http
POST /api/users
Content-Type: application/json

{
    "name": "João Silva",
    "email": "joao@email.com",
    "age": 30
}
```

```http
PUT /api/products/123
Content-Type: application/json

{
    "name": "Notebook Gamer",
    "price": 2500.00,
    "category": "electronics"
}
```

### Implementação Spring Boot:
```java
@PostMapping("/users")
public User createUser(@RequestBody @Valid User user) {
    return userService.create(user);
}

@PutMapping("/products/{id}")
public Product updateProduct(
    @PathVariable Long id,
    @RequestBody @Valid Product product
) {
    return productService.update(id, product);
}
```

## 4. Headers (Cabeçalhos)

Os **Headers** carregam metadados sobre a requisição, como autenticação, tipo de conteúdo, etc.

### Características:
- Contêm metadados da requisição
- Usados para autenticação, configuração, etc.
- Não fazem parte do corpo da mensagem
- Case-insensitive

### Exemplos Comuns:
- `Authorization: Bearer token123`
- `Content-Type: application/json`
- `Accept: application/json`
- `X-API-Key: api-key-123`

### Implementação Spring Boot:
```java
@GetMapping("/users")
public List<User> getUsers(
    @RequestHeader("Authorization") String authorization,
    @RequestHeader(value = "X-API-Key", required = false) String apiKey
) {
    // Lógica de autenticação e processamento
    return userService.findAll();
}

@PostMapping("/users")
public User createUser(
    @RequestBody User user,
    @RequestHeader("Content-Type") String contentType
) {
    return userService.create(user);
}
```

## 5. Form Data (Dados de Formulário)

**Form Data** é usado para envio de dados de formulários, incluindo upload de arquivos.

### Características:
- Content-Type: `application/x-www-form-urlencoded` ou `multipart/form-data`
- Usado para formulários HTML tradicionais
- Suporta upload de arquivos
- Dados são codificados no corpo da requisição

### Exemplos:
```http
POST /api/users
Content-Type: application/x-www-form-urlencoded

name=João+Silva&email=joao@email.com&age=30
```

```http
POST /api/upload
Content-Type: multipart/form-data

------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="document.pdf"
Content-Type: application/pdf

[binary data]
------WebKitFormBoundary
```

### Implementação Spring Boot:
```java
@PostMapping("/upload")
public String uploadFile(
    @RequestParam("file") MultipartFile file,
    @RequestParam("description") String description
) {
    return fileService.upload(file, description);
}

@PostMapping(value = "/users", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
public User createUserFromForm(
    @RequestParam String name,
    @RequestParam String email,
    @RequestParam int age
) {
    User user = new User(name, email, age);
    return userService.create(user);
}
```

## 6. Matrix Parameters

**Matrix Parameters** são uma alternativa aos Query Parameters, usando ponto e vírgula como separador.

### Características:
- Usam `;` como separador
- Menos comuns que Query Parameters
- Úteis para parâmetros hierárquicos
- Suporte limitado em alguns frameworks

### Exemplo:
```http
GET /api/cars;color=red;year=2020;brand=toyota
```

### Implementação Spring Boot:
```java
@GetMapping("/cars/{path}")
public List<Car> getCars(
    @PathVariable String path,
    @MatrixVariable Map<String, String> matrixVars
) {
    return carService.findByMatrixParams(matrixVars);
}
```

## Boas Práticas

### 1. **Escolha do Tipo Correto**
- Use **Path Parameters** para identificar recursos específicos
- Use **Query Parameters** para filtros, paginação e ordenação
- Use **Request Body** para dados complexos (POST, PUT, PATCH)
- Use **Headers** para metadados e autenticação

### 2. **Validação**
```java
@PostMapping("/users")
public User createUser(@RequestBody @Valid User user) {
    return userService.create(user);
}

@GetMapping("/users/{id}")
public User getUser(@PathVariable @Min(1) Long id) {
    return userService.findById(id);
}
```

### 3. **Documentação**
```java
@GetMapping("/users")
@Operation(summary = "Buscar usuários")
@Parameter(name = "page", description = "Número da página")
@Parameter(name = "size", description = "Tamanho da página")
public Page<User> getUsers(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size
) {
    return userService.findUsers(page, size);
}
```

### 4. **Tratamento de Erros**
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidationErrors(
    MethodArgumentNotValidException ex
) {
    // Tratar erros de validação de parâmetros
    return ResponseEntity.badRequest().body(errorResponse);
}
```

## Resumo

| Tipo | Localização | Uso Principal | Obrigatório |
|------|-------------|---------------|-------------|
| Path Parameters | URL path | Identificar recursos | Sim |
| Query Parameters | URL query string | Filtros, paginação | Não |
| Request Body | Corpo da requisição | Dados complexos | Depende |
| Headers | Cabeçalhos HTTP | Metadados, auth | Depende |
| Form Data | Corpo da requisição | Formulários, upload | Depende |
| Matrix Parameters | URL path | Parâmetros hierárquicos | Não |

Cada tipo de parâmetro tem seu propósito específico e deve ser usado adequadamente para criar APIs REST bem estruturadas e intuitivas.
