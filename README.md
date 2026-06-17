# GTI - Sistema de Gestão de TI

Plataforma interna de gestão de TI com três módulos: **Usuários**, **Ativos** e **Chamados**.

Spring Boot + MariaDB + HTML/CSS/JS (servido como recurso estático do Spring Boot).

---

## Stack

- **Java 21** (Amazon Corretto)
- **Spring Boot 3.2.4** (Web, Data JPA, Security)
- **MariaDB** - banco `sistemagti`
- **Maven** (ou wrapper `./mvnw`)
- **Autenticação:** JWT (jjwt 0.11.5) + Spring Security, senhas com BCrypt
- **Frontend:** HTML/CSS/JS puro, sem framework
- **Lombok**

---

## Pré-requisitos

- Java 21 instalado
- Maven instalado (ou usar o wrapper)
- MariaDB rodando localmente

---

## Passo 1 - Banco de dados

O banco é criado automaticamente na primeira execução (`createDatabaseIfNotExist=true`). Se preferir criar manualmente:

```sql
CREATE DATABASE IF NOT EXISTS sistemagti;
```

As tabelas são geradas pelo Hibernate (`spring.jpa.hibernate.ddl-auto=update`) com base nas entidades. Não é necessário rodar SQL manualmente.

---

## Passo 2 - Configuração de acesso ao banco

Arquivo: `src/main/resources/application.properties`

```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/sistemagti?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=1234
```

Ajuste `username` e `password` conforme o seu MariaDB.

A chave de assinatura do JWT também fica nesse arquivo (`jwt.secret`).

---

## Passo 3 - Rodar o projeto

```bash
mvn spring-boot:run
```

Sem Maven global:

```bash
./mvnw spring-boot:run    # Linux / Mac
mvnw.cmd spring-boot:run  # Windows
```

---

## Passo 4 - Acessar o sistema

```
http://localhost:8080
```

A raiz redireciona para `login.html`.

---

## Perfis de acesso

Dois tipos de usuário (`tipo_acesso`):

- **TI** - acesso completo (usuários, ativos, chamados, dashboard, relatórios).
- **COMUM** - abre e acompanha apenas os próprios chamados.

Após o login, TI é direcionado para `home.html` e COMUM para `chamados/chamados-usuario.html`.

---

## Endpoints da API

### Autenticação
| Método | URL | Descrição |
|--------|-----|-----------|
| POST | /api/auth/login | Autentica e retorna o token JWT |

### Usuários
| Método | URL | Descrição |
|--------|-----|-----------|
| GET | /api/usuarios | Lista todos |
| GET | /api/usuarios/{id} | Busca por ID |
| POST | /api/usuarios | Cria |
| PUT | /api/usuarios/{id} | Atualiza |
| PATCH | /api/usuarios/{id}/bloqueio | Bloqueia/desbloqueia |
| DELETE | /api/usuarios/{id} | Exclui |

### Tipos de ativo e campos dinâmicos
| Método | URL | Descrição |
|--------|-----|-----------|
| GET | /api/tipos-ativo | Lista tipos |
| GET | /api/tipos-ativo/{id} | Busca tipo |
| POST | /api/tipos-ativo | Cria tipo |
| PUT | /api/tipos-ativo/{id} | Atualiza tipo |
| DELETE | /api/tipos-ativo/{id} | Exclui tipo |
| GET | /api/tipos-ativo/{id}/campos | Lista campos do tipo |
| POST | /api/tipos-ativo/{id}/campos | Adiciona campo |
| DELETE | /api/tipos-ativo/campos/{campoId} | Exclui campo |

### Ativos
| Método | URL | Descrição |
|--------|-----|-----------|
| GET | /api/ativos | Lista todos |
| GET | /api/ativos/{id} | Busca por ID |
| GET | /api/ativos/{id}/valores | Valores dos campos dinâmicos |
| POST | /api/ativos | Cria |
| PUT | /api/ativos/{id} | Atualiza |
| DELETE | /api/ativos/{id} | Exclui |
| GET | /api/ativos/{id}/historico | Histórico do ativo |
| GET | /api/ativos/{id}/manutencoes | Lista manutenções |
| POST | /api/ativos/{id}/manutencoes | Registra manutenção |

### Chamados
| Método | URL | Descrição |
|--------|-----|-----------|
| GET | /api/chamados | Lista (TI: todos; COMUM: próprios) |
| GET | /api/chamados/{id} | Busca por ID |
| POST | /api/chamados | Cria (título + descrição) |
| PUT | /api/chamados/{id} | Atualiza (apenas técnico que assumiu) |
| PATCH | /api/chamados/{id}/assumir | TI assume o chamado |
| PATCH | /api/chamados/{id}/status | Altera status |
| DELETE | /api/chamados/{id} | Exclui |
| GET | /api/chamados/{id}/mensagens | Lista mensagens do chat |
| POST | /api/chamados/{id}/mensagens | Envia mensagem |
| GET | /api/ativos/{ativoId}/chamados | Chamados vinculados a um ativo |

### Dashboard e relatórios
| Método | URL | Descrição |
|--------|-----|-----------|
| GET | /api/dashboard | Indicadores agregados (filtros: tipo, status, centroCusto) |
| GET | /api/relatorios/ativos | Tabela de ativos (filtros aplicáveis) |
| GET | /api/relatorios/ativos/export | Exportação CSV (UTF-8 BOM, separador `;`) |

---

## Estrutura do projeto

```
gti/
├── pom.xml
└── src/main/
    ├── java/com/gti/usuarios/
    │   ├── GtiApplication.java          ← ponto de entrada
    │   ├── model/                       ← entidades JPA
    │   │   ├── Usuario.java
    │   │   ├── TipoAtivo.java
    │   │   ├── CampoDinamico.java
    │   │   ├── Ativo.java
    │   │   ├── ValorCampo.java
    │   │   ├── HistoricoAtivo.java
    │   │   ├── Manutencao.java
    │   │   ├── Chamado.java
    │   │   └── MensagemChamado.java
    │   ├── repository/                  ← acesso ao banco (JPA)
    │   ├── service/                     ← regras de negócio
    │   │   ├── UsuarioService.java
    │   │   ├── TipoAtivoService.java
    │   │   ├── AtivoService.java
    │   │   ├── ChamadoService.java
    │   │   └── MensagemChamadoService.java
    │   ├── controller/                  ← endpoints REST
    │   └── security/                    ← JWT + Spring Security
    │       ├── SecurityConfig.java
    │       ├── JwtFilter.java
    │       └── JwtUtil.java
    └── resources/
        ├── application.properties
        ├── logback-spring.xml
        └── static/                      ← frontend
            ├── login.html
            ├── home.html
            ├── auth.js                  ← wrapper apiFetch (Bearer token)
            ├── navbar.js                ← sidebar dinâmica por módulo/perfil
            ├── style.css
            ├── ativos/
            │   ├── dashboard.html
            │   ├── ativos.html
            │   ├── tipos.html
            │   ├── relatorios.html
            │   └── detalhe-ativo.html
            ├── chamados/
            │   ├── chamados.html
            │   ├── chamados-usuario.html
            │   ├── detalhe-chamado.html
            │   └── detalhe-chamado-usuario.html
            └── usuarios/
                └── usuarios.html
```

---

## Segurança

- Senhas armazenadas com **BCrypt** (`BCryptPasswordEncoder`); nunca em texto puro.
- Autenticação **stateless** via **JWT** (validade de 8h), enviado no header `Authorization: Bearer <token>`.
- O frontend usa o wrapper `apiFetch()` (em `auth.js`), que anexa o token automaticamente e redireciona ao login em caso de `401`/`403`.