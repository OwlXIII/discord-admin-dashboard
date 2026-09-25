**Enhanced with Claude Code**

# Discord Admin Dashboard

A Spring Boot web application for administering the Discord servers you own or have
`ADMINISTRATOR` on. Log in with Discord (OAuth2), pick a server, and manage its
members and roles from a lightweight dashboard.

## Features

- **Discord OAuth2 login** with the session stored in an `httpOnly`, `SameSite` JWT cookie.
- **Server list** filtered to only the guilds you can administer (owner or `ADMINISTRATOR`).
- **Member management** via a Discord bot (JDA): list members, kick, add/remove roles.
- **Role management**: list roles, create new roles with optional permissions.
- **Modern UI**: Discord-styled dashboard with toasts, confirmations and loading states.

## Architecture

```
com.discordadmindashboard
├── config      DiscordProperties, AppProperties, SecurityConfig
├── security    JwtService, JwtAuthenticationFilter, UserContext, AuthenticatedUser
├── service     DiscordOAuthService (user API), DiscordBotService (JDA gateway)
├── controller  OAuthController, ViewController, ServerController, MemberController
├── dto         DiscordGuild, DiscordUser, MemberDto, RoleDto, CreateRoleRequest, ApiMessage
└── exception   GlobalExceptionHandler + typed exceptions
```

- **User-scoped** calls (login, server list) use the user's OAuth token via `DiscordOAuthService`.
- **Privileged** calls (members, kicks, roles) go through the bot in `DiscordBotService`.
- A request-scoped `UserContext` (populated by the JWT filter) carries the identity.

## Configuration

All secrets are read from environment variables — nothing sensitive is committed.
The easiest way to provide them is a **`.env` file** (loaded automatically via
[spring-dotenv](https://github.com/paulschwarz/spring-dotenv), like `python-dotenv`):

```bash
cp .env.example .env   # then edit .env and fill in your values
```

`.env` is gitignored. Real OS environment variables still take precedence over it,
and if `.env` is missing the app falls back to environment variables / defaults.

| Variable | Purpose | Default |
|---|---|---|
| `DISCORD_CLIENT_ID` | OAuth2 client id | _(required for login)_ |
| `DISCORD_CLIENT_SECRET` | OAuth2 client secret | _(required for login)_ |
| `DISCORD_BOT_TOKEN` | Bot token for member/role actions | _(optional; endpoints return 503 without it)_ |
| `DISCORD_REDIRECT_URI` | OAuth2 redirect URI | `http://localhost:8080/oauth/redirect` |
| `APP_DASHBOARD_URL` | Where to land after login | `http://localhost:8080/dashboard.html` |
| `APP_COOKIE_SECURE` | Mark the cookie `Secure` (HTTPS) | `false` |
| `APP_JWT_SECRET` | Stable JWT signing secret (>= 32 bytes) | _(ephemeral if unset)_ |
| `APP_JWT_TTL_MINUTES` | Session lifetime | `60` |

In the Discord Developer Portal, add the redirect URI and enable the
**Server Members Intent** for the bot.

## Running

With a `.env` file in place (recommended):

```bash
./mvnw spring-boot:run
```

Or provide the variables inline in PowerShell instead of using `.env`:

```powershell
$env:DISCORD_CLIENT_ID="your-client-id"; $env:DISCORD_CLIENT_SECRET="your-client-secret"; $env:DISCORD_BOT_TOKEN="your-bot-token"; ./mvnw spring-boot:run
```

Then open http://localhost:8080 and click **Login with Discord**.

## Testing

```bash
./mvnw test
```