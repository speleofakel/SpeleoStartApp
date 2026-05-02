# 🏗️ ARCHITECTURE.md · SpeleoStart
**Платформа:** Android (Kotlin, Compose) | **Архитектура:** MVVM + Clean

## 1. СТРУКТУРА ПАКЕТОВ

```
com.speleo.start/
├── data/
│   ├── local/          # Room: Entity, DAO, Database, Converters
│   └── repository/     # Абстракция над DAO (Flow/Suspend)
├── domain/
│   └── usecase/        # Бизнес-правила (валидация, расчёты, ОБГ)
├── presentation/
│   ├── SharedState.kt  # Глобальный StateFlow (activeCompId, selectedTeamId)
│   ├── TimerManager.kt # Singleton, Flow-таймер, восстановление из БД
│   └── screen/         # Compose UI + ViewModel (StateFlow/LaunchedEffect)
└── util/               # DateValidator, PhoneFormatter, StringExt (TitleCase)
```

## 2. ПОТОКИ ДАННЫХ

- Экран → ViewModel: UserEvent (клик, ввод, подтверждение)
- ViewModel → UseCase/Repository: Валидация → suspend fun → Room
- Repository → DB: Flow<List> → реактивное обновление UI
- Global State: SharedState хранит выбранные ID. Хардкод 1L/2L запрещён.

## 3. КЛЮЧЕВЫЕ РЕШЕНИЯ

### 3.1 Таймер и восстановление

- TimerManager считает: elapsed = now - startTimestamp
- startTimestamp сохраняется в app_settings при старте/паузе/краше
- При перезапуске: restoreFromSavedState() вычисляет разницу, таймер не сбрасывается

### 3.2 Динамические правила (из настроек)

- Параметры: min_team_size, confirm_judge, confirm_secretary, class_queue
- Хранятся в competition.settings_json
- Парсятся при загрузке соревнования, валидируются перед стартом

### 3.3 ОБГ и сортировка результатов

- Формируется в ResultsVM: isOBG = memberCount > settings.min_team_size
- Сортировка: Status.ordinal → score DESC → netTime ASC → isOBG ASC

### 3.4 Подтверждение ПЛ

- RouteCardVM.save() читает флаги из settings_json
- Если флаги сняты → пропускает ввод паролей, ставит checkpointsEntered = true, показывает предупреждение
- Все изменения логируются в HistoryDao

### 3.5 Форматирование ФИО

- Поля всегда раздельные: Фамилия | Имя | Отчество
- Автокап первой буквы (Title Case) применяется в `util/StringExt` перед сохранением
- Работает в регистрации, инлайн-создании, быстром менторе

**Реализация Title Case для русского языка:**

```kotlin
private val RUSSIAN_LOCALE = Locale("ru", "RU")

fun String.toTitleCase(): String {
    if (isEmpty()) return this
    
    val result = StringBuilder()
    var capitalizeNext = true
    
    for (i in indices) {
        val ch = this[i]
        
        when {
            ch.isLetter() -> {
                if (capitalizeNext) {
                    result.append(ch.uppercase(RUSSIAN_LOCALE))
                    capitalizeNext = false
                } else {
                    result.append(ch.lowercase(RUSSIAN_LOCALE))
                }
            }
            ch == '-' -> {  // Дефисные фамилии (Салтыков-Щедрин)
                result.append(ch)
                capitalizeNext = true
            }
            ch == ' ' || ch == '.' -> {
                result.append(ch)
                capitalizeNext = true
            }
            else -> result.append(ch)
        }
    }
    
    return result.toString()
}
```
**Ключевые особенности:**
- Явный `Locale("ru", "RU")` вместо `Locale.getDefault()` — корректная обработка русского языка на любом устройстве
- Поддержка дефисных фамилий (каждая часть с заглавной)
- Инициалы (Иванов И.И.) не ломаются
- `capitalizeNext` сбрасывается после пробелов, точек и дефисов

### 3.6 Стабильность курсора в полях автоформатирования

**Проблема:** При вводе текста с авто-вставкой символов (точки в дате `ДД.ММ.ГГГГ`, скобки в телефоне `+7 (XXX) XXX-XX-XX`) курсор «прыгает» — позиционируется перед вставленным символом вместо конца ввода.

**Корневая причина:** `TextFieldValue.selection.start` указывает на позицию в **исходном** тексте (до форматирования). После вставки авто-символа длина `formatted` строки отличается от `newVal.text`, и прямое копирование `selection.start` даёт неверную позицию.

**Запрещённый паттерн (приводит к багу):**

```
val newCursor = newVal.selection.start.coerceIn(0, formatted.length)
```

**Обязательный паттерн (пересчёт через цифры):**

```
val digitsBeforeCursor = newVal.text.take(oldCursor).count { it.isDigit() }
val dotsBeforeCursor = (digitsBeforeCursor / 2).coerceAtMost(2)
val newCursor = (digitsBeforeCursor + dotsBeforeCursor).coerceIn(0, formatted.length)
```

**Правило:** При автоформатировании, где количество вставляемых символов зависит от позиции (N цифр → N + K разделителей), курсор всегда пересчитывается через **количество значащих символов до старой позиции**, а не через абсолютный индекс.

**Синхронизация состояния:**

```
var state by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }

LaunchedEffect(value) {
if (state.text != value) {
state = TextFieldValue(value, TextRange(value.length))
}
}
```

`LaunchedEffect` предотвращает рассинхрон при обновлении `value` извне (например, при очистке поля).

### 3.7 ageGroup при быстром создании персоны

- При `createQuickPerson()` обязательно вычислять `ageGroup` через `calculateAgeGroup(birthDate)` и сохранять в `MemberDraft`
- `AgeGroup.UNKNOWN` после сохранения персоны недопустим — блокирует `allRequirementsMet` и скрывает UI ментора
- В `MemberCard` использовать `effectiveAgeGroup`: резервный расчёт на лету если `ageGroup == UNKNOWN` (защита от legacy-данных)

### 3.8 Поиск персоны с кнопками "+" и "✖"

**Проблема:** При поиске персоны нельзя создать новую, если в выдаче есть однофамильцы. Кнопка создания была скрыта за результатами поиска.

**Решение:** В поле поиска добавлены две кнопки в `trailingIcon`:
- **Кнопка "+"** — зелёная (`#4CAF50`), создаёт новую персону с введённой фамилией
- **Кнопка "✖"** — красная (`#F44336`), очищает поле поиска (появляется только при наличии текста)

**Техническая реализация:**
```kotlin
trailingIcon = {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        IconButton(
            onClick = { /* создание персоны */ },
            enabled = searchQuery.isNotBlank(),
            modifier = Modifier.size(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_action_add),
                    contentDescription = "Добавить",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        if (searchQuery.isNotBlank()) {
            IconButton(
                onClick = { onSearchQueryChange("") },
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFFF44336).copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_clear),
                        contentDescription = "Очистить",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
```

**Ключевые параметры:**
- Прозрачность `alpha = 0.4f` — приглушает яркие цвета
- Размер кнопок `28.dp` — визуально меньше высоты поля ввода
- SVG иконки `ic_action_add.xml` и `ic_action_clear.xml` — единый стиль
- Отступ `padding(end = 8.dp)` — предотвращает наезжание на рамку поля




## 4. БАЗА ДАННЫХ (Room v5)

- Триггеры: check_mentor_age, prevent_edit_confirmed_route_card, check_person_active_in_one_team
- Миграции: version++ строго при изменении @Entity. fallbackToDestructiveMigration только в Debug
- Очистка: строгий порядок дети → родители (участники → менторы → путевые → команды → персоны → соревнования)
- Безопасность: Пароли SHA-256 + salt. EncryptedSharedPreferences для ключей сессии

## 5. ПРАВИЛА РАЗРАБОТКИ

1. Один файл → одна полная правка за итерацию
2. Никогда не удалять импорты до финализации экрана
3. Всегда проверять insert() != -1L
4. Не хардкодить ID. Использовать SharedState или navArgument
5. Тёмная тема: не использовать захардкоженные Color(). Только MaterialTheme или проверка обеих тем
6. **Курсор автоформатирования:** Запрещено использовать `newVal.selection.start` как новую позицию курсора после вставки разделителей. Обязателен пересчёт через количество значащих символов.
7. **ageGroup при quick-create:** Обязательно вычислять и сохранять в `MemberDraft` при `createQuickPerson()`. `UNKNOWN` после сохранения — баг.