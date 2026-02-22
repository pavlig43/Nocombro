# Claude Code Notification Script for Windows
# Показывает toast notification с информацией о вкладке WT

param(
    [Parameter(Mandatory=$true)]
    [string]$MessageType,

    [string]$Title = "Claude Code",
    [string]$Message = "",
    [string]$ToolName = ""
)

# Получаем информацию о текущей сессии
$CurrentDir = Get-Location | Select-Object -ExpandProperty Path
$ProjectName = Split-Path $CurrentDir -Leaf
$WT_SESSION = $env:WT_SESSION

# Пытаемся получить название вкладки из заголовка окна Windows Terminal
$TabTitle = "?"
try {
    # Ищем процесс Windows Terminal
    $WTProcess = Get-Process -Name "WindowsTerminal" -ErrorAction SilentlyContinue

    if ($WTProcess -and $WTProcess.MainWindowTitle) {
        $Title = $WTProcess.MainWindowTitle

        # Убираем спецсимволы спиннера из заголовка
        $Title = $Title -replace '[\x2800\x2801\x2802\x2803\x2804\x2805\x2806\x2807⠁⠂⠃⠄⠅⠆⠇⠈⠉⠊⠋⠌⠍⠎⠏]', ''

        if ($Title -match '^\s*$') {
            # Если заголовок пустой, используем короткий хеш WT_SESSION
            if ($WT_SESSION) {
                $TabTitle = $WT_SESSION.Substring(0, 8)
            }
        } else {
            # Используем заголовок как название вкладки
            $TabTitle = $Title.Trim()
        }
    } else {
        # Если процесс не найден, используем короткий хеш WT_SESSION
        if ($WT_SESSION) {
            $TabTitle = $WT_SESSION.Substring(0, 8)
        }
    }
} catch {
    # При ошибке используем короткий хеш WT_SESSION
    if ($WT_SESSION) {
        $TabTitle = $WT_SESSION.Substring(0, 8)
    }
}

# Формируем информацию о вкладке
$TabInfo = $TabTitle

# Сопоставление типов событий с эмодзи
$Icons = @{
    "Stop" = "✅"
    "PostToolUse" = "⚡"
    "PreToolUse" = "▶️"
    "UserPromptSubmit" = "💬"
    "Error" = "❌"
    "AgentRun" = "🤖"
}

$Icon = if ($Icons.ContainsKey($MessageType)) { $Icons[$MessageType] } else { "📌" }

# Сопоставление типов событий с сообщениями
$Messages = @{
    "Stop" = "Работа завершена"
    "PostToolUse" = if ($ToolName) { "Готово: $ToolName" } else { "Действие выполнено" }
    "PreToolUse" = if ($ToolName) { "Выполняю: $ToolName" } else { "Выполняется..." }
    "UserPromptSubmit" = "Сообщение отправлено"
    "Error" = "Ошибка!"
    "AgentRun" = "Агент запущен"
}

$DefaultMessage = if ($Messages.ContainsKey($MessageType)) { $Messages[$MessageType] } else { $MessageType }
$DisplayMessage = if ($Message) { $Message } else { $DefaultMessage }

# Полный заголовок с иконкой и проектом
$FullTitle = "$Icon $Title"
$FullMessage = "$DisplayMessage`n$TabInfo | $ProjectName"

# Загружаем сборки для Windows Forms
Add-Type -AssemblyName System.Windows.Forms

# Создаём уведомление
$notification = New-Object System.Windows.Forms.NotifyIcon

# Выбираем иконку в зависимости от типа события
$TipIcon = switch ($MessageType) {
    "Error" { [System.Windows.Forms.ToolTipIcon]::Error }
    "Stop" { [System.Windows.Forms.ToolTipIcon]::Info }
    default { [System.Windows.Forms.ToolTipIcon]::Info }
}

$notification.Icon = [System.Drawing.SystemIcons]::Information
$notification.BalloonTipIcon = $TipIcon
$notification.BalloonTipTitle = $FullTitle
$notification.BalloonTipText = $FullMessage
$notification.Visible = $true

# Показываем уведомление на 7 секунд
$notification.ShowBalloonTip(7000)

# Ждём немного перед закрытием
Start-Sleep -Milliseconds 7500

# Освобождаем ресурсы
$notification.Dispose()
