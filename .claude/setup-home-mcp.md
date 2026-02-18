# Установка MCP серверов на домашнем компьютере

## GitHub MCP

```bash
# Установка
claude mcp add github

# Проверка
claude mcp list
```

## GitHub CLI авторизация

```bash
# Войти для 5000 req/hour
gh auth login

# Выбрать:
# - GitHub.com
# - HTTPS
# - Login with a web browser
```

## DeepWiki (если понадобится)

```bash
# DeepWiki для поиска по документации
claude mcp add -s user -t http deepwiki https://mcp.deepwiki.com/mcp
```

## Проверка всех MCP

```bash
claude mcp list
```

---

## Использование

```
> Создай PR в pavlig43/Nocombro
> Открой issue про баг
> Покажи последние 5 коммитов
> Как использовать Decompose?
```
