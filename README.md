# PresentYá — Loja virtual

Presentes e variedades, com destaque para **presentes personalizados**. São Luís – MA.

| Parte | Tecnologia | Pasta |
|---|---|---|
| API REST (backend) | Java 21 + Spring Boot 4 (Maven, abre no Eclipse) | `backend/` |
| Loja e painel (frontend) | React 19 + Vite | `frontend/` |
| Banco de dados | PostgreSQL — banco `BDPresentya` | `database/` |
| Pagamentos | Mercado Pago: cartão de crédito, débito e Pix | `backend/.../pagamento` |

O visual é o do `presentya-site.html` original (cópia em `docs/`), agora com produtos, categorias e dados da loja vindos do banco. As sugestões de `docs/sugestões.docx` já estão aplicadas.

## Perfis de acesso

| Perfil | Pode |
|---|---|
| **ADMIN** | Tudo: produtos, categorias, pedidos, usuários/perfis e dados da loja (contatos, horários, frete) |
| **TECNICO** | Cadastrar/editar produtos e fotos; acompanhar pedidos e mudar status (produção, entrega) |
| **USUARIO** | Cliente: comprar, acompanhar pedidos, Clube de pontos |

Na primeira execução é criado o administrador **admin@presentya.com.br / Admin@2026** (mude a senha em *Minha conta*, ou defina `ADMIN_EMAIL`/`ADMIN_PASSWORD` antes de rodar pela primeira vez). Para criar um técnico, a pessoa cria conta no site e o admin muda o perfil em **Painel > Usuários**.

## Rodar no computador

### 1. Banco de dados
```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -h localhost -f database\criar-banco.sql
```
As tabelas e os dados iniciais são criados pela API.

### 2. Backend (porta 8080)
```powershell
cd backend
$env:DB_PASSWORD = "sua-senha-do-postgres"
.\mvnw.cmd spring-boot:run
```
**Eclipse:** *File > Import > Maven > Existing Maven Projects* → pasta `backend`. Rode `PresentyaApiApplication` (Run As > Java Application) com a variável `DB_PASSWORD` em *Run Configurations > Environment*.

Sem PostgreSQL (banco em memória, para testes rápidos):
```powershell
.\mvnw.cmd spring-boot:test-run "-Dspring-boot.run.profiles=test" "-Dspring-boot.run.mainClass=br.com.presentya.TestPresentyaApiApplication"
```
Testes automatizados: `.\mvnw.cmd test`

### 3. Frontend (porta 5173)
```powershell
cd frontend
npm install
npm run dev
```
Abra http://localhost:5173 — painel em http://localhost:5173/admin

## Variáveis de ambiente do backend

| Variável | Padrão | Para quê |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/BDPresentya` | Conexão do banco |
| `DB_USER` / `DB_PASSWORD` | `postgres` / `postgres` | Usuário e senha do banco |
| `JWT_SECRET` | (valor de desenvolvimento) | **Obrigatório trocar em produção** (mín. 32 caracteres) |
| `FRONTEND_URL` | `http://localhost:5173` | Endereço da loja (CORS e retorno do pagamento) |
| `API_URL` | `http://localhost:8080` | Endereço público da API (aviso de pagamento do Mercado Pago) |
| `MP_ACCESS_TOKEN` | vazio | Token do Mercado Pago. **Vazio = modo simulado**, sem cobrança real |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | ver acima | Primeiro administrador |

## Pagamentos (Mercado Pago)

- **Pix:** QR Code e "copia e cola" na própria página do pedido; ela atualiza sozinha quando o pagamento cai.
- **Crédito (até 12x) e débito:** o cliente paga na página segura do Mercado Pago (Checkout Pro). A loja **não recebe nem guarda dados de cartão**.
- Preços, frete e total são sempre recalculados no servidor.
- Pagamento aprovado → pedido "Pago", pontos do Clube creditados (R$ 1 = 1 ponto).

Para ativar: crie a conta em https://www.mercadopago.com.br/developers, copie o **Access Token** (teste ou produção) para `MP_ACCESS_TOKEN` e, em produção, cadastre o webhook `https://SUA-API/api/pagamentos/webhook/mercadopago` (evento *Pagamentos*).

## Hospedagem gratuita (sugestão)

| O quê | Onde | Observação |
|---|---|---|
| Frontend | **Vercel** (ou Netlify) | Pasta `frontend`, variável `VITE_API_URL` = endereço da API. `vercel.json` já incluso |
| Backend | **Render** (Docker) | `render.yaml` e `backend/Dockerfile` inclusos. O plano grátis "dorme" após 15 min sem acesso (o 1º acesso demora ~1 min) |
| Banco | **Neon** (PostgreSQL) | Plano grátis permanente. Use a URL JDBC com `?sslmode=require` |

Passo a passo:
1. Suba a pasta `presentya` para um repositório no GitHub.
2. **Neon:** crie o projeto, crie o banco `BDPresentya` e copie host/usuário/senha.
3. **Render:** *New > Blueprint* → escolha o repositório → preencha as variáveis (`DB_URL=jdbc:postgresql://HOST/BDPresentya?sslmode=require`, `DB_USER`, `DB_PASSWORD`, `FRONTEND_URL`, `API_URL`, `MP_ACCESS_TOKEN`).
4. **Vercel:** *Add New Project* → repositório → *Root Directory* `frontend` → variável `VITE_API_URL` com o endereço do Render.
5. Volte ao Render e confira se `FRONTEND_URL` é exatamente o endereço da Vercel.

Quando a loja começar a vender de verdade, vale migrar o backend para um plano pago (~US$ 7/mês no Render) para não "dormir" e não perder clientes no primeiro acesso.

## Estrutura

```
backend/   API Spring Boot (domain, repository, service, pagamento, web, config)
frontend/  Loja (pages), painel (admin), componentes e estilos (styles/site.css)
database/  Script de criação do BDPresentya
docs/      Site original e documento de sugestões
```
