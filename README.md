# App Cliente (Android, Kotlin + Jetpack Compose)

Aplicativo do cliente final, **um só APK para todas as lojas**. O cliente entra
com conta `CUSTOMER` e as lojas entram na conta quando ele abre o link do
estabelecimento (`deliveryhub://loja/{slug}` ou `https://.../loja/{slug}`). A
lista resultante é a tela inicial: com **uma** loja o cardápio abre direto; com
**duas ou mais**, o app mostra os estabelecimentos. As abas são **Cardápio**,
**Carrinho**, **Pedidos** e **Conta** (quando não há loja ativa, a primeira aba
se chama **Lojas**).

## Como rodar

1. Instale o Android Studio (Ladybug ou mais recente) e um emulador API 26+.
2. Abra a pasta `cliente-android`.
3. Suba o backend completo (`docker compose up -d`) ou os serviços necessários.
4. Rode o app. O build `debug` usa `http://10.0.2.2:8080/api/`, endereço do host visto pelo emulador.
5. Entre ou crie uma conta de cliente na tela inicial. Para testar sem cadastro,
   use `POST /auth/register` com `role = "CUSTOMER"`.

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
```

Em aparelho físico, informe o IP da máquina mantendo `/api/` no final:

```powershell
.\gradlew.bat :app:assembleDebug -PAPI_BASE_URL=http://192.168.1.100:8080/api/
```

## Estabelecimentos pelo link

Não existe mais `STORE_SLUG`: a loja **não** é escolhida no build. O fluxo é:

1. o cliente abre o link da loja (deep link, link HTTPS ou colando o link na aba **Lojas**);
2. sem sessão, o app carrega o tema público (`GET /tenants/{slug}/theme`) e usa a loja como hero do login;
3. depois do login, o app chama `POST /customers/me/stores`; o servidor valida o slug e grava o vínculo na conta;
4. o app recarrega `GET /customers/me/stores` e o tema ao vivo de cada loja.

Regras que o app aplica:

- abrir o mesmo link duas vezes não duplica a loja (o `POST` é idempotente);
- slug inexistente mostra erro no diálogo do link e não cria vínculo;
- com uma única loja, o cardápio abre direto; com duas ou mais, a lista inicial;
- trocar de loja (botão **Trocar** no cabeçalho do cardápio) limpa o carrinho, com aviso;
- o histórico de pedidos é global e cada pedido mostra a loja;
- na aba **Conta** é possível abrir ou remover um estabelecimento.

Teste rápido pelo `adb`:

```powershell
adb shell am start -a android.intent.action.VIEW `
  -d "deliveryhub://loja/vera-marmitex" com.deliveryplatform.cliente
```

O `MainActivity` usa `launchMode="singleTop"` e `onNewIntent`: um link novo com o
app aberto abre a loja sem reiniciar a sessão.

## Propriedades do build

| Propriedade | Default | Efeito |
|---|---|---|
| `APP_NAME` | `Delivery` | Nome exibido na gaveta de apps e marca da tela de login sem loja vinculada. |
| `GOOGLE_CLIENT_ID` | vazio | Client ID OAuth web; vazio esconde o login Google. |
| `API_BASE_URL` | `http://10.0.2.2:8080/api/` no debug | URL base do gateway. |

```powershell
.\gradlew.bat :app:assembleRelease `
  -PAPP_NAME="Delivery" `
  -PGOOGLE_CLIENT_ID=1234567890.apps.googleusercontent.com `
  -PAPI_BASE_URL=https://api.seudominio.com/api/
```

O build `debug` mostra, na aba **Lojas**, as lojas de `GET /tenants` para testar
sem um link em mãos. O build `release` não expõe esse atalho.

## O que funciona com o backend

O login é obrigatório: sem sessão o app abre a tela de entrada em tela cheia e não
carrega cardápio, pedidos nem dados da conta até o cliente autenticar.

| Tela | Chamadas |
|---|---|
| Abertura da loja pelo link | `GET /tenants/{slug}/theme` |
| Login, cadastro e Google | `POST /auth/login`, `POST /auth/register`, `POST /auth/google` |
| Sessão | JWT em `EncryptedSharedPreferences`; sem refresh token |
| Estabelecimentos | `GET/POST /customers/me/stores` e `DELETE /customers/me/stores/{slug}` |
| Cardápio e filtro alimentar | `GET /catalog/tenants/{tenantId}/dishes?availableOnly=true&tags=` |
| Carrinho e janelas de entrega | `POST /orders/availability` |
| Checkout | `POST /orders` |
| Histórico de pedidos | `GET /orders/customer/{customerId}` |
| Perfil | `GET/PUT /customers/me` |
| Endereços | `GET/POST/PUT/DELETE /customers/me/addresses` e `PATCH .../{id}/default` |
| Cartões | `GET/POST/DELETE /customers/me/cards` e `PATCH .../{id}/default` |

O carrinho lista só janelas com `available = true`, pré-seleciona a
`recommended` e mostra o encaixe e os minutos livres.

### Pedidos e comanda

A aba **Pedidos** lista o histórico do mais recente para o mais antigo, com
loja, número, data, quantidade de itens, total e status, e atualiza a cada 30 s
enquanto visível. Tocar num pedido abre a **comanda** (`OrderDetailScreen`):

- status atual com descrição e cor por etapa;
- linha do tempo `Recebido → Em preparo → Pronto → Saiu para entrega → Entregue`,
  com tratamento próprio para `CANCELLED`;
- itens com opções e valores, subtotal, entrega, total, forma de pagamento,
  endereço de entrega, janela agendada e observações;
- botão **Falar com a loja** quando o tema traz telefone.

O status é lido de `Order.status` (`CREATED`, `PAID`, `ACCEPTED`, `PREPARING`,
`READY`, `PICKED_UP`, `DELIVERED`, `CANCELLED`) e atualizado por polling; não há
push.

## Limitações conhecidas

- O app só aceita contas `CUSTOMER`; outros perfis são rejeitados no cliente.
- Pagamento é simulado: não há PSP, captura ou estorno.
- **Não há mapa do entregador em tempo real.** O backend não guarda coordenadas
  nem posição do entregador (`Order` só tem `courierId`) e não existe serviço de
  tracking; a comanda mostra apenas a linha do tempo do status. Um mapa exige
  endpoint de posição, coleta de GPS no app do entregador e geocodificação do
  endereço.
- O carrinho é de uma loja por vez: trocar de estabelecimento descarta os itens.
- A loja ainda não vê a lista de clientes que abriram o link: o vínculo é
  gravado com `tenant_id`, mas não há endpoint do vendedor para consultá-lo.
- Não há push, avaliação, cupom ou fidelidade.
- A agenda pública da cozinha (`GET /orders/tenant/{tenantId}/agenda`) **não** é
  consumida por este app: continua na web do cliente e no app vendedor.
- O release usa `https://api.seudominio.com/api/` como placeholder até a URL real ser definida.
- Injeção de dependências é manual (`AppContainer`); Hilt pode entrar quando houver mais telas.
