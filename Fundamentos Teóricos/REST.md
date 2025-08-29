# REST

Representational State Transfer(REST) é um estilo de arquitetura de software para sistemas distribuídos de hipermídia, como a World Wide Web.

## REST é baseado em um conjunto de constraints

- **Cliente-servidor**
Clientes e servidores separados.

- **Stateless server**
O servidor não deve guardar o estado do cliente. Cada request de um cliente contém todas as informações para atendê-la.

- **Cacheable**
O cliente deve ser informado sobre as propriedades de cache de um recurso para que possa decidir quando deve ou não utilizar cache.

- **Interface uniforme**
    - Identificação de recursos (URI)
    - Manipulação de recursos a partir de suas representações
    - Mensagens auto descritivas
    - Hypermedia as the engine of applications state - HATEOAS

- **Sistema em camadas**
Deve suporte conceitos como balanceamento de carga, proxies e firewalls.

- **Código sob Demanda (opcional)**
O cliente pode solicitar o código do servidor e executar.

## Formatos suportados pelo REST
- XML
- JSON
- CSV
- Texto
- Imagens
- HTML
- etc.

## Vantagens dos Web Services RESTful
- Padrão leve
- Desenvolvimento fácil e rápido
- Aplicativos Mobile utilizam cada vez mais