# Cardapio - Fundamentos Java

Projeto de estudos em Java com foco em fundamentos da linguagem e de backend:

- modelagem de dominio (`ItemCardapio`)
- serializacao/desserializacao com Gson (JSON e objeto Java serializado)
- JDBC com MySQL
- sockets (cliente e servidor)
- mini framework REST baseado em anotacoes customizadas
- organizacao em camadas (use cases, DAO, infraestrutura)

## Stack

- Java
- Gradle
- Gson
- MySQL Connector/J
- Reflections (scan de anotacoes/classes)
- JUnit 5

## Estrutura principal

```text
src/main/java/mx/florinda/cardapio
|- application/          # regras de negocio (use cases)
|- client/               # clientes e servidor socket "basico"
|- database/             # DAO + JDBC
|- infra/                # leitura de propriedades
|- pix/                  # exemplo de serializacao customizada
|- rest/                 # controller REST + anotacoes customizadas
|- socket/               # servidor HTTP/socket e cliente
|- Main.java             # exemplo direto de uso do DAO
```

Arquivos relevantes:

- `src/main/resources/cardapio.properties`: configuracao de conexao com banco
- `src/main/resources/db/script.sql`: criacao do schema e seed inicial
- `src/test/java/http/*.http`: exemplos de chamadas HTTP para testar endpoints

## Requisitos

- JDK 21+ (recomendado)
- Docker e Docker Compose (para subir o MySQL)

## Como subir o banco local

Na raiz do projeto:

```bash
docker compose up -d
```

Isso sobe um MySQL com:

- host: `localhost`
- porta: `3306`
- usuario: `root`
- senha: `senha123`
- banco: `cardapio` (criado automaticamente via `script.sql`)

## Build e testes

Windows:

```bash
.\gradlew.bat clean build
.\gradlew.bat test
```

Linux/macOS:

```bash
./gradlew clean build
./gradlew test
```

## Executando a aplicacao

O projeto possui várias classes com `main` para cenarios diferentes.

Principais:

- `mx.florinda.cardapio.socket.server.ServidorItensCardapioComSocket`
  - sobe servidor HTTP em socket na porta `8000`
- `mx.florinda.cardapio.Main`
  - exemplo simples de acesso ao banco via DAO

Forma recomendada: executar essas classes pela IDE (Run Configuration).

## Endpoints principais (servidor socket)

Base URL: `http://localhost:8000`

- `GET /itens-cardapio`
- `GET /itens-cardapio/{id}`
- `POST /itens-cardapio`
- `PATCH /itens-cardapio/{id}/price`
- `DELETE /itens-cardapio/{id}`
- `GET /itens-cardapio/total`
- `GET /itensCardapio.json`
- `GET /` e `GET /en`

Use os arquivos `.http` em `src/test/java/http/` para testes rápidos.

## Observacoes

- A conexão com banco vem de `cardapio.properties`.
- O servidor REST/socket faz descoberta de métodos anotados com `@Rest`, `@Get`, `@Post`, etc.
- Este repositório e voltado a aprendizado, concentrando múltiplos exemplos em um único projeto.
