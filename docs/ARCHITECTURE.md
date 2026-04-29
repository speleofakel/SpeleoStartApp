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
- Автокап первой буквы (Title Case) применяется в util/StringExt перед сохранением
- Работает в регистрации, инлайн-создании, быстром менторе

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