# План: Исправление NoNameShadowing в launchCatching

## Проблема

Detekt выдаёт предупреждение `NoNameShadowing` в функции `launchCatching` — параметр `context` затеняется параметром с тем же именем в `CoroutineExceptionHandler`.

## Решение

Переименовать параметр функции `launchCatching` из `context` в `coroutineContext`.

## Файл для изменения

- `core/src/desktopMain/kotlin/ru/pavlig43/core/CoroutineUtils.kt:24-35`

## Изменения

```diff
 public fun CoroutineScope.launchCatching(
-    context: CoroutineContext = EmptyCoroutineContext,
+    coroutineContext: CoroutineContext = EmptyCoroutineContext,
     start: CoroutineStart = CoroutineStart.DEFAULT,
     onError: CoroutineContext.(Throwable) -> Unit,
     block: suspend CoroutineScope.() -> Unit,
 ): Job {
     val handler = CoroutineExceptionHandler { context, exception ->
         onError(context, exception)
     }
     val scope = this + handler + SupervisorJob()
-    return scope.launch(context, start, block)
+    return scope.launch(coroutineContext, start, block)
 }
```

## Проверка

После изменения:
- Detekt не должен выдавать предупреждение `NoNameShadowing`
- Код должен компилироваться без ошибок
