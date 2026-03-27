# Как переключить модель в Claude Code на GLM-5-turbo

## Глобальная настройка (для всех проектов)

Файл: `~/.claude/settings.json` (Windows: `C:\Users\<user>\.claude\settings.json`)

```json
{
  "model": "glm-5-turbo"
}
```

## Через флаг при запуске (одноразово)

```bash
claude --model glm-5-turbo
```

## Переключение обратно на Claude

В том же файле `~/.claude/settings.json`:

```json
{
  "model": "claude-sonnet-4-20250514"
}
```

Доступные модели Claude:
- `claude-sonnet-4-20250514` — Sonnet 4 (по умолчанию)
- `claude-opus-4-20250514` — Opus 4
- `claude-haiku-3-5-20241022` — Haiku 3.5

## Приоритет настроек (от высшего к низшему)

1. Флаг `--model` в командной строке
2. Файл `~/.claude/settings.json` (глобальный)
3. Значение по умолчанию (Claude Sonnet)
