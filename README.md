# GTI - Sistema de Gestão de TI

Plataforma interna de gestão de TI com três módulos: **Usuários**, **Ativos** e **Chamados**.

Spring Boot (API) + MariaDB + frontend Vue 3 (build estático servido pelo Spring Boot).

---

## Stack

- **Java 21** (Amazon Corretto)
- **Spring Boot 3.2.4** (Web, Data JPA, Security)
- **MariaDB** - banco `sistemagti`
- **Maven** (ou wrapper `./mvnw`)
- **Autenticação:** JWT (jjwt 0.11.5) + Spring Security, senhas com BCrypt
- **Frontend:** Vue 3 + Vite + Vue Router + Pinia + Tailwind CSS + Chart.js (build gerado em `src/main/resources/static`)
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

## Passo 3 - Frontend (build)

O frontend fica em `frontend/` (Vue 3 + Vite) e é buildado direto para `src/main/resources/static`, servido pelo Spring Boot.

```bash
cd frontend
npm install
npm run build
```

Para desenvolver com hot-reload (proxy para a API em `:8080`):

```bash
npm run dev    # http://localhost:5173
```

---

## Passo 4 - Rodar o backend

```bash
mvn spring-boot:run
```

Sem Maven global:

```bash
./mvnw spring-boot:run    # Linux / Mac
mvnw.cmd spring-boot:run  # Windows
```

---

## Passo 5 - Acessar o sistema

```
http://localhost:8080
```

A raiz redireciona para `/login`.

---

## Perfis de acesso

Dois tipos de usuário (`tipo_acesso`):

- **TI** - acesso completo (usuários, ativos, chamados, dashboard, relatórios).
- **COMUM** - abre e acompanha apenas os próprios chamados.

Após o login, TI é direcionado para `/home` e COMUM para `/meus-chamados`.

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

### Centros de custo
| Método | URL | Descrição |
|--------|-----|-----------|
| GET | /api/centros-custo | Lista todos |
| POST | /api/centros-custo | Cria |

### Integração (chave de API)

Consumo externo — autenticado pelo header `X-API-Key`, sem login. Cada chave só acessa os conjuntos de dados liberados na sua criação.

| Método | URL | Descrição |
|--------|-----|-----------|
| GET | /api/integracao/ativos | Exporta ativos (escopo ATIVOS) |
| GET | /api/integracao/usuarios | Exporta usuários (escopo USUARIOS) |
| GET | /api/integracao/chamados | Exporta chamados (escopo CHAMADOS) |
| GET | /api/integracao/dashboard | Exporta indicadores (escopo DASHBOARD) |

Administração das chaves — autenticado por JWT, restrito ao perfil TI.

| Método | URL | Descrição |
|--------|-----|-----------|
| GET | /api/integracao/chaves | Lista as chaves com situação e último uso |
| POST | /api/integracao/chaves | Cria uma chave e retorna o valor uma única vez |
| GET | /api/integracao/chaves/{id} | Detalhe com uso de 30 dias e últimas chamadas |
| POST | /api/integracao/chaves/{id}/girar | Gera a substituta e dá carência à atual |
| DELETE | /api/integracao/chaves/{id} | Desliga a chave (não apaga o histórico) |
| GET | /api/integracao/chaves/tentativas-negadas | Tentativas recusadas, paginadas |
| GET | /api/integracao/preview/{ativos\|usuarios\|chamados\|dashboard} | Amostra do payload para a tela de configurações |

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
├── src/main/
│   ├── java/com/gti/usuarios/
│   │   ├── GtiApplication.java          ← ponto de entrada
│   │   ├── model/                       ← entidades JPA
│   │   │   ├── Usuario.java
│   │   │   ├── TipoAtivo.java
│   │   │   ├── CampoDinamico.java
│   │   │   ├── Ativo.java
│   │   │   ├── ValorCampo.java
│   │   │   ├── AnexoAtivo.java
│   │   │   ├── CentroCusto.java
│   │   │   ├── HistoricoAtivo.java
│   │   │   ├── Manutencao.java
│   │   │   ├── Chamado.java
│   │   │   └── MensagemChamado.java
│   │   ├── repository/                  ← acesso ao banco (JPA)
│   │   ├── service/                     ← regras de negócio
│   │   ├── controller/                  ← endpoints REST
│   │   ├── security/                    ← JWT + Spring Security
│   │   │   ├── SecurityConfig.java
│   │   │   ├── JwtFilter.java
│   │   │   ├── JwtUtil.java
│   │   │   ├── IntegracaoApiKeyFilter.java
│   │   │   └── ChaveApiRateLimiter.java
│   │   ├── audit/                       ← listener de auditoria
│   │   ├── config/                      ← CORS, migrations
│   │   └── dto/
│   └── resources/
│       ├── application.properties
│       ├── logback-spring.xml
│       └── static/                      ← build do frontend (gerado pelo Vite, não editar direto)
└── frontend/                            ← código-fonte do frontend (Vue 3 + Vite)
    ├── package.json / vite.config.js / tailwind.config.js
    └── src/
        ├── main.js / App.vue
        ├── router/                      ← rotas (Vue Router)
        ├── stores/                      ← estado (Pinia) - auth
        ├── services/                    ← chamadas à API (axios)
        ├── layouts/
        ├── components/                  ← base/, ativos/, chamados/
        └── views/                       ← Login, Home, Consulta, ativos/, chamados/, usuarios/, configuracoes/
```

---

## Segurança

- Senhas armazenadas com **BCrypt** (`BCryptPasswordEncoder`); nunca em texto puro.
- Autenticação **stateless** via **JWT** (validade de 8h), enviado no header `Authorization: Bearer <token>`.
- O frontend usa uma instância axios (`services/http.js`) que anexa o token automaticamente e redireciona ao login em caso de `401`/`403`; estado de autenticação em `stores/auth.js` (Pinia).
- Integrações externas autenticam por chave de API (`IntegracaoApiKeyFilter`). As chaves são nomeadas por consumidor, armazenadas apenas como hash SHA-256 (o valor em texto puro é exibido uma única vez, na criação) e validadas quanto a situação, validade, endereço de origem, escopo do recurso e limite de chamadas por minuto.
- Toda chamada à API de integração é registrada, inclusive as recusadas, com endpoint, método, origem, status e motivo da recusa. O detalhe é mantido por `integracao.dias-retencao-uso` dias e consolidado em totais diários por chave.