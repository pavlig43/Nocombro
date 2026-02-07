# MCP: GitHub и DeepWiki

## Что настроено

**`.claude/mcp.json`:**
```json
{
  "mcpServers": {
    "deepwiki": {
      "type": "sse",
      "url": "https://mcp.deepwiki.com/sse"
    },
    "github": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "env": {
        "GITHUB_TOKEN": "YOUR_GITHUB_TOKEN_HERE"
      }
    }
  }
}
```

---

## GitHub MCP

### Установка

```bash
npm install -g @modelcontextprotocol/server-github
```

### Получить токен

1. GitHub → Settings → Developer settings
2. Personal access tokens → Tokens (classic)
3. Generate new token → Выбери `repo`, `issues`, `pull_requests`
4. Скопируй токен и вставь в `mcp.json` вместо `YOUR_GITHUB_TOKEN_HERE`

### Использование

```
> Покажи открытые PR в репозитории
> Создай issue с заголовком "Bug in decimalColumn"
> Какой последний коммит в main?
> Открой issue на основе текущих изменений
```

---

## DeepWiki MCP

### Что это

Поиск по документации без интернета.

### Использование

```
> Как работать с DateTime в Compose?
> Объясни Kotlin Flow
> Как использовать Decompose для навигации?
```

---

## Проверка работы

```
> Какие MCP серверы доступны?
```

Должен показать:
- `deepwiki` — поиск по документации
- `github` — работа с GitHub

---

**Источник:** https://modelcontextprotocol.io/
