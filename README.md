# Parking Spot API

API REST para cadastro de **vagas de estacionamento** de um condomínio.
É o backend pronto usado nas atividades de CRUD com **Flutter**: os aplicativos só consomem a API, não é preciso alterar o código Java.

Cada vaga registra: número da vaga, placa, marca, modelo e cor do veículo, nome do responsável, apartamento e bloco.

## Tecnologias

- Java 11 (também testado com JDK 21)
- Spring Boot 2.6.4 (Web, Data JPA, Validation)
- PostgreSQL
- Lombok
- Maven (wrapper `mvnw` incluso, não é preciso instalar o Maven)

## Como executar

### 1. Banco de dados

Com o PostgreSQL instalado e em execução, crie o banco (uma única vez):

```sql
CREATE DATABASE "parking-control-db";
```

A tabela `tb_parking_spot` é criada automaticamente na primeira execução. Os dados existentes nunca são apagados.

### 2. Configuração da conexão

A conexão é configurada por variáveis de ambiente. Valores padrão:

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_HOST` | `localhost` | Endereço do PostgreSQL |
| `DB_PORT` | `5432` | Porta do PostgreSQL |
| `DB_NAME` | `parking-control-db` | Nome do banco |
| `DB_USERNAME` | `postgres` | Usuário |
| `DB_PASSWORD` | `postgres` | Senha |
| `SERVER_PORT` | `8080` | Porta da API |
| `CORS_ALLOWED_ORIGINS` | `*` | Origens liberadas para Flutter Web (separadas por vírgula) |

A forma mais simples é criar um arquivo **`.env`** na raiz do projeto, copiando o modelo `.env.example`:

```properties
DB_PASSWORD=minha_senha
```

O `.env` não é enviado ao Git. Variáveis de ambiente do sistema têm prioridade sobre ele.

### 3. Subir a API

Na raiz do projeto:

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Também é possível executar a classe `ParkingControlApplication` pela IDE (IntelliJ, VS Code etc.).

A API estará disponível quando aparecer no console: `Started ParkingControlApplication`.

## URL base

| Onde o app Flutter roda | URL base |
|---|---|
| Flutter Web / desktop / simulador iOS (mesmo computador) | `http://localhost:8080` |
| Emulador Android | `http://10.0.2.2:8080` |
| Celular físico (mesma rede Wi-Fi) | `http://IP_DO_COMPUTADOR:8080` (ex.: `http://192.168.0.15:8080`) |

Dicas:

- Descubra o IP do computador com `ipconfig` (Windows) ou `ip a` / `ifconfig` (Linux/macOS). Se o celular não conectar, libere a porta 8080 no firewall.
- **Android** bloqueia HTTP sem TLS por padrão. Em `android/app/src/main/AndroidManifest.xml`, na tag `<application>`, adicione `android:usesCleartextTraffic="true"`.
- **iOS** bloqueia HTTP por padrão. No `ios/Runner/Info.plist`, adicione a chave `NSAppTransportSecurity` com `NSAllowsArbitraryLoads` = `true` (somente em desenvolvimento).

## Endpoints

Todos os endpoints começam com `/parking-spot`.

| Método | Endpoint | Operação | Sucesso |
|---|---|---|---|
| GET | `/parking-spot` | Listar todas as vagas | 200 |
| GET | `/parking-spot/{id}` | Buscar vaga por id | 200 |
| POST | `/parking-spot` | Cadastrar vaga | 201 |
| PUT | `/parking-spot/{id}` | Atualizar vaga | 200 |
| DELETE | `/parking-spot/{id}` | Excluir vaga | 200 |
| GET | `/parking-spot/order` | Listagem paginada | 200 |

Endpoints alternativos (mantidos por compatibilidade; prefira os da tabela acima):

| Método | Endpoint | Equivale a |
|---|---|---|
| POST | `/parking-spot/edit-param/{id}` | PUT `/parking-spot/{id}` |
| POST | `/parking-spot/edit` (com `id` no corpo) | PUT `/parking-spot/{id}` |
| POST | `/parking-spot/delete/{id}` | DELETE `/parking-spot/{id}` |

Envie sempre o cabeçalho **`Content-Type: application/json`** no POST e no PUT.

### Campos da vaga

| Campo | Tipo | Obrigatório | Máx. caracteres | Observação |
|---|---|---|---|---|
| `id` | UUID (texto) | — | — | Gerado pela API |
| `registrationDate` | data/hora (texto) | — | — | Gerada pela API (UTC) |
| `parkingSpotNumber` | texto | sim | 10 | Único |
| `licensePlateCar` | texto | sim | 7 | Único. Ex.: `ABC1D23` (sem hífen) |
| `brandCar` | texto | sim | 70 | |
| `modelCar` | texto | sim | 70 | |
| `colorCar` | texto | sim | 70 | |
| `responsibleName` | texto | sim | 130 | |
| `apartment` | texto | sim | 30 | Par apartamento + bloco é único |
| `block` | texto | sim | 30 | |

`id` e `registrationDate` são controlados pela API: se forem enviados no POST ou no PUT, são ignorados. Por isso, no PUT pode ser enviado o mesmo objeto recebido no GET.

### Cadastrar (POST `/parking-spot`)

Requisição:

```json
{
  "parkingSpotNumber": "205B",
  "licensePlateCar": "RRS8562",
  "brandCar": "Audi",
  "modelCar": "Q5",
  "colorCar": "Preto",
  "responsibleName": "Maria Souza",
  "apartment": "205",
  "block": "B"
}
```

Resposta `201 Created`:

```json
{
  "id": "f1945877-a77b-4e83-86f7-98de66fd8a60",
  "parkingSpotNumber": "205B",
  "licensePlateCar": "RRS8562",
  "brandCar": "Audi",
  "modelCar": "Q5",
  "colorCar": "Preto",
  "registrationDate": "2026-10-19T15:00:00Z",
  "responsibleName": "Maria Souza",
  "apartment": "205",
  "block": "B"
}
```

### Listar (GET `/parking-spot`)

Retorna uma lista (`[]` quando não há vagas):

```json
[
  { "id": "f1945877-...", "parkingSpotNumber": "205B", "licensePlateCar": "RRS8562", "...": "..." }
]
```

### Atualizar (PUT `/parking-spot/{id}`)

Mesmo corpo do cadastro. Retorna `200` com a vaga atualizada. O `id` e a `registrationDate` originais são mantidos.

### Excluir (DELETE `/parking-spot/{id}`)

Retorna `200` com o **texto simples** (não é JSON):

```
Parking Spot deleted successfully.
```

### Listagem paginada (GET `/parking-spot/order`)

Parâmetros opcionais: `page` (começa em 0, padrão 0), `size` (padrão 10) e `sort` (padrão `id,asc`).

```
GET /parking-spot/order?page=0&size=5&sort=parkingSpotNumber,asc
```

Resposta (resumida):

```json
{
  "content": [ { "id": "...", "parkingSpotNumber": "101A", "...": "..." } ],
  "totalElements": 12,
  "totalPages": 3,
  "number": 0,
  "size": 5,
  "first": true,
  "last": false,
  "empty": false
}
```

### Datas

`registrationDate` é enviada no formato ISO 8601 em **UTC** (sufixo `Z`): `2026-10-19T15:00:00Z`.
No Flutter: `DateTime.parse(json['registrationDate']).toLocal()` converte para o horário local.

## Códigos HTTP

| Código | Quando acontece |
|---|---|
| 200 OK | Consulta, atualização ou exclusão realizada |
| 201 Created | Vaga cadastrada |
| 400 Bad Request | Campos inválidos, JSON malformado, id em formato inválido |
| 404 Not Found | Vaga (ou endpoint) não encontrada |
| 405 Method Not Allowed | Método HTTP errado para o endpoint |
| 409 Conflict | Placa, número de vaga ou apartamento/bloco já cadastrados |
| 415 Unsupported Media Type | Faltou o cabeçalho `Content-Type: application/json` |
| 500 Internal Server Error | Erro inesperado no servidor |

## Erros

Toda resposta de erro é um JSON com o mesmo formato:

```json
{
  "status": 409,
  "message": "Esta placa já está cadastrada.",
  "timestamp": "2026-10-19T15:00:00Z"
}
```

Em erros de validação (400), o campo `errors` informa o problema de cada campo:

```json
{
  "status": 400,
  "message": "Dados inválidos. Verifique os campos informados.",
  "timestamp": "2026-10-19T15:00:00Z",
  "errors": {
    "licensePlateCar": "A placa do veículo deve ter no máximo 7 caracteres.",
    "block": "O bloco é obrigatório."
  }
}
```

Mensagens possíveis:

| Status | Mensagem |
|---|---|
| 400 | `Dados inválidos. Verifique os campos informados.` (com `errors`) |
| 400 | `Corpo da requisição ausente ou com JSON inválido.` |
| 400 | `Valor inválido para o parâmetro 'id'.` |
| 400 | `O campo id é obrigatório para editar uma vaga.` (somente `POST /edit`) |
| 400 | `Campo de ordenação inválido: '...'.` |
| 404 | `Vaga de estacionamento não encontrada.` |
| 404 | `Endpoint não encontrado.` |
| 409 | `Esta placa já está cadastrada.` |
| 409 | `Este número de vaga já está em uso.` |
| 409 | `Já existe uma vaga cadastrada para este apartamento/bloco.` |
| 409 | `Não foi possível salvar: já existe uma vaga com esta placa ou este número de vaga.` |
| 415 | `Formato não suportado. Envie o cabeçalho 'Content-Type: application/json'.` |

Na atualização, a vaga pode manter a própria placa, o próprio número e o próprio apartamento/bloco. O conflito só ocorre se esses dados pertencerem a **outra** vaga.

## Exemplo em Flutter (pacote `http`)

```dart
import 'dart:convert';
import 'package:http/http.dart' as http;

const baseUrl = 'http://10.0.2.2:8080/parking-spot';

Future<void> cadastrar() async {
  final response = await http.post(
    Uri.parse(baseUrl),
    headers: {'Content-Type': 'application/json'},
    body: jsonEncode({
      'parkingSpotNumber': '205B',
      'licensePlateCar': 'RRS8562',
      'brandCar': 'Audi',
      'modelCar': 'Q5',
      'colorCar': 'Preto',
      'responsibleName': 'Maria Souza',
      'apartment': '205',
      'block': 'B',
    }),
  );

  final body = jsonDecode(utf8.decode(response.bodyBytes));
  if (response.statusCode == 201) {
    print('Cadastrado: ${body['id']}');
  } else {
    print('Erro ${body['status']}: ${body['message']}');
  }
}
```

Use `utf8.decode(response.bodyBytes)` para que os acentos apareçam corretamente.

## Testes

```bash
mvnw.cmd test        # Windows
./mvnw test          # Linux / macOS
```

- `ParkingSpotControllerTest`: testa endpoints, validações e erros **sem** banco de dados.
- `ParkingControlApplicationTests`: sobe a aplicação completa e **precisa** do PostgreSQL configurado.

Para gerar o `.jar` sem rodar os testes: `mvnw.cmd package -DskipTests`.
