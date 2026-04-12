# GTI — CRUD de Usuários
Spring Boot + MariaDB + HTML/JS

---

## Pré-requisitos

- Java 17+ instalado
- Maven instalado (ou use o wrapper `./mvnw`)
- MariaDB rodando localmente

---

## Passo 1 — Configurar o banco

Abra o MariaDB e crie o banco (o Spring cria a tabela automaticamente):

```sql
CREATE DATABASE IF NOT EXISTS gti_usuarios;
```

---

## Passo 2 — Configurar usuário/senha do banco

Abra o arquivo:
```
src/main/resources/application.properties
```

Altere as linhas conforme o seu MariaDB:
```properties
spring.datasource.username=root
spring.datasource.password=root
```

---

## Passo 3 — Rodar o projeto

No terminal, dentro da pasta do projeto:

```bash
mvn spring-boot:run
```

Se não tiver Maven instalado globalmente:
```bash
./mvnw spring-boot:run   # Linux / Mac
mvnw.cmd spring-boot:run # Windows
```

---

## Passo 4 — Abrir o sistema

Abra o navegador em:
```
http://localhost:8080
```

A tabela `usuarios` é criada automaticamente no MariaDB.

---

## Endpoints da API

| Método | URL                              | O que faz             |
|--------|----------------------------------|-----------------------|
| GET    | /api/usuarios                    | Lista todos           |
| GET    | /api/usuarios/{id}               | Busca por ID          |
| POST   | /api/usuarios                    | Cria novo             |
| PUT    | /api/usuarios/{id}               | Atualiza              |
| PATCH  | /api/usuarios/{id}/bloqueio      | Bloqueia/Desbloqueia  |
| DELETE | /api/usuarios/{id}               | Exclui                |

---

## Estrutura do projeto

```
gti-usuarios/
├── pom.xml                          ← dependências Maven
└── src/main/
    ├── java/com/gti/usuarios/
    │   ├── GtiUsuariosApplication.java  ← main (ponto de entrada)
    │   ├── model/
    │   │   └── Usuario.java             ← MODEL / Entidade JPA
    │   ├── repository/
    │   │   └── UsuarioRepository.java   ← acesso ao banco
    │   ├── service/
    │   │   └── UsuarioService.java      ← regras de negócio
    │   └── controller/
    │       └── UsuarioController.java   ← CONTROLLER / endpoints REST
    └── resources/
        ├── application.properties       ← config do banco
        └── static/
            └── index.html               ← VIEW / frontend HTML
```

---

## Tabela criada no banco (automático)

```sql
CREATE TABLE usuarios (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome         VARCHAR(150) NOT NULL,
  descricao    VARCHAR(255),
  bloqueado    BOOLEAN NOT NULL DEFAULT FALSE,
  criado_em    DATETIME NOT NULL,
  atualizado_em DATETIME
);
```

O Hibernate gera essa tabela automaticamente com base na classe `Usuario.java`.
Você não precisa executar nenhum SQL manualmente.
