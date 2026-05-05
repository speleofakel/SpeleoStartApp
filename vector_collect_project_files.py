import os
import sys
from pathlib import Path
from datetime import datetime

# ============================================================================
# ИНСТРУКЦИЯ ПО ЗАПУСКУ:
# ============================================================================
# 1. Сохраните этот файл как vector_collect_project_files.py в папке:
#    D:\Android\Projects\SpeleoStartApp
#
# 2. Запустите скрипт:
#    cd D:\Android\Projects\SpeleoStartApp
#    python vector_collect_project_files.py
#
# 3. При запросе вставьте список файлов в одном из форматов:
#
#    Формат 1 (каждый файл с новой строки):
#      TeamRegisterScreen.kt
#      PersonNewScreen.kt
#      TitleCaseTextField.kt
#      ...
#
#    Формат 2 (через запятую):
#      TeamRegisterScreen.kt, PersonNewScreen.kt, MainActivity.kt
#
#    Формат 3 (через пробел):
#      TeamRegisterScreen.kt PersonNewScreen.kt MainActivity.kt
#
#    Для завершения ввода в формате 1 - нажмите Enter дважды
# ============================================================================

# ============================================================================
# НАСТРОЙКИ (измените под свой проект)
# ============================================================================
PROJECT_DIR = Path(r"D:\Android\Projects\SpeleoStartApp")
OUTPUT_PREFIX = "vector"
MAX_FILE_SIZE = 100 * 1024  # 100 КБ
# ============================================================================

def check_python_version():
    """Проверяет версию Python"""
    if sys.version_info < (3, 6):
        print("⚠️  ВНИМАНИЕ: Рекомендуется Python 3.6 или выше")
        print(f"   Ваша версия: {sys.version}")

def show_header():
    """Показывает заголовок"""
    print("=" * 70)
    print("📦 ВЕКТОРНЫЙ СБОРЩИК ФАЙЛОВ ПРОЕКТА")
    print("=" * 70)
    print(f"\n📂 Папка проекта: {PROJECT_DIR}")
    print(f"⚡ Лимит файла: {MAX_FILE_SIZE//1024} КБ")
    print(f"📄 Формат вывода: {OUTPUT_PREFIX}_ГГГГММДД_1_N.txt")
    print("=" * 70)
    print()

def input_file_list():
    """
    Запрашивает у пользователя список файлов.
    Поддерживает ввод:
    - каждой строки как отдельного файла (пустая строка - конец)
    - через запятую
    - через пробел
    """
    print("📝 Введите список файлов для сбора.")
    print("   (можно вставить список из сообщения)")
    print("-" * 70)
    print("Примеры ввода:")
    print("   1. Каждый файл с новой строки (Enter дважды для завершения):")
    print("      TeamRegisterScreen.kt")
    print("      PersonNewScreen.kt")
    print("      MainActivity.kt")
    print()
    print("   2. Через запятую:")
    print("      TeamRegisterScreen.kt, PersonNewScreen.kt, MainActivity.kt")
    print()
    print("   3. Через пробел:")
    print("      TeamRegisterScreen.kt PersonNewScreen.kt MainActivity.kt")
    print("-" * 70)
    print()

    lines = []
    print("➡️  Введите файлы (для завершения ввода - пустая строка):")

    while True:
        try:
            line = input().strip()
            if line == "":
                if lines:
                    break
                continue
            lines.append(line)
        except EOFError:
            break

    if not lines:
        print("❌ Список файлов не введён.")
        sys.exit(1)

    # Объединяем все строки и парсим
    full_text = " ".join(lines)

    # Пробуем разные разделители
    file_names = []

    # Если есть запятые - разбиваем по запятым
    if "," in full_text:
        parts = full_text.split(",")
        for part in parts:
            part = part.strip()
            if part:
                # Если внутри есть пробелы - могли быть несколько файлов
                for subpart in part.split():
                    if subpart:
                        file_names.append(subpart)
    else:
        # Разбиваем по пробелам и переводам строк
        for part in full_text.split():
            if part:
                file_names.append(part)

    # Удаляем дубликаты, сохраняя порядок
    seen = set()
    unique_files = []
    for f in file_names:
        if f not in seen:
            seen.add(f)
            unique_files.append(f)

    return unique_files

def find_files_by_names(project_root, target_names):
    """
    Находит файлы по имени или относительному пути.
    Возвращает список (относительный_путь, полный_путь)
    """
    found = []
    missing = []

    for target in target_names:
        target = target.strip()
        if not target:
            continue

        # Пытаемся найти точное совпадение пути
        exact_path = project_root / target
        if exact_path.exists() and exact_path.is_file():
            rel_path = exact_path.relative_to(project_root)
            found.append((rel_path, exact_path))
            continue

        # Ищем по имени файла (рекурсивно)
        matches = []
        for root, dirs, files in os.walk(project_root):
            # Исключаем системные папки
            dirs[:] = [d for d in dirs if d not in {".git", "build", ".idea", "gradle", "captures"}]
            if target in files:
                full_path = Path(root) / target
                rel_path = full_path.relative_to(project_root)
                matches.append((rel_path, full_path))

        if len(matches) == 1:
            found.append(matches[0])
        elif len(matches) > 1:
            print(f"⚠️  Найдено несколько файлов с именем '{target}':")
            for rel, _ in matches:
                print(f"     - {rel}")
            print(f"     ➜ Берётся первый: {matches[0][0]}")
            found.append(matches[0])
        else:
            missing.append(target)

    return found, missing

def format_file_entry(rel_path, content):
    """Форматирует одну запись для выходного файла."""
    path_str = str(rel_path).replace("\\", "/")
    return f"{path_str}>>>\n{content}\n<<<конец файла\n\n"

def get_current_date_str():
    """Возвращает текущую дату в формате ГГГГММДД"""
    return datetime.now().strftime("%Y%m%d")

def main():
    check_python_version()
    show_header()

    if not PROJECT_DIR.exists():
        print(f"❌ Ошибка: папка проекта не найдена: {PROJECT_DIR}")
        print(f"\n💡 Совет: проверьте путь в переменной PROJECT_DIR")
        sys.exit(1)

    # Запрашиваем список файлов
    target_files = input_file_list()

    print(f"\n✅ Получено файлов: {len(target_files)}")
    print("📋 Список:")
    for i, f in enumerate(target_files, 1):
        print(f"   {i:2d}. {f}")

    print("\n" + "=" * 70)
    answer = input("❓ Продолжить с этим списком? (y/n): ").strip().lower()
    if answer not in ("y", "yes", "да", "+", "1"):
        print("❌ Отменено пользователем.")
        return

    print("\n🔍 Поиск файлов в проекте...")
    files, missing = find_files_by_names(PROJECT_DIR, target_files)
    total_files = len(files)

    # Показываем ненайденные файлы
    if missing:
        print("\n⚠️  НЕ НАЙДЕНЫ следующие файлы:")
        for f in missing:
            print(f"   ❌ {f}")
        print()

    if total_files == 0:
        print("❌ Не найдено ни одного файла из введённого списка.")
        print("💡 Проверьте имена файлов и путь к проекту")
        sys.exit(1)

    # Суммарный размер исходных файлов
    total_raw_size = 0
    for _, full_path in files:
        try:
            total_raw_size += full_path.stat().st_size
        except:
            pass

    print(f"\n📊 СТАТИСТИКА:")
    print(f"   📁 Найдено файлов: {total_files} из {len(target_files)}")
    if missing:
        print(f"   ❌ Не найдено: {len(missing)}")
    print(f"   💾 Общий исходный размер: {total_raw_size / 1024:.2f} КБ")
    print(f"   📂 Папка сохранения: {PROJECT_DIR}")

    print("\n" + "=" * 70)
    answer = input("❓ Начать генерацию? (y/n): ").strip().lower()
    if answer not in ("y", "yes", "да", "+", "1"):
        print("❌ Отменено пользователем.")
        return

    print("\n🚀 Чтение и обработка файлов...")
    print("=" * 70)

    # Читаем все файлы и подготавливаем записи
    entries = []
    for idx, (rel_path, full_path) in enumerate(files, 1):
        print(f"   [{idx}/{total_files}] Чтение: {rel_path}")
        try:
            # Бинарные файлы определяем по расширению
            binary_extensions = {".png", ".jpg", ".jpeg", ".zip", ".pdf", ".svg", ".ico", ".webp", ".bin"}
            if full_path.suffix.lower() in binary_extensions:
                content = f"[BINARY FILE: {full_path.name} — содержимое не показано]"
            else:
                with open(full_path, "r", encoding="utf-8", errors="replace") as f:
                    content = f.read()
        except Exception as e:
            content = f"[ОШИБКА ЧТЕНИЯ: {e}]"

        entry = format_file_entry(rel_path, content)
        entries.append((rel_path, entry, len(entry.encode("utf-8"))))

    # Распределяем записи по файлам с учётом лимита
    parts = []
    current_part_entries = []
    current_size = 0

    for rel_path, entry, entry_size in entries:
        # Если одна запись больше лимита - всё равно добавляем (не разрываем)
        if current_size + entry_size > MAX_FILE_SIZE and current_size > 0:
            parts.append(current_part_entries)
            current_part_entries = []
            current_size = 0
        current_part_entries.append(entry)
        current_size += entry_size

    if current_part_entries:
        parts.append(current_part_entries)

    total_parts = len(parts)
    date_str = get_current_date_str()

    print(f"\n📦 Создание выходных файлов (всего {total_parts} частей)...")
    print("-" * 70)

    # Записываем части
    for part_idx, part_entries in enumerate(parts, 1):
        out_filename = PROJECT_DIR / f"{OUTPUT_PREFIX}_{date_str}_{part_idx}_{total_parts}.txt"

        total_part_size = 0
        with open(out_filename, "w", encoding="utf-8") as out_file:
            for entry in part_entries:
                out_file.write(entry)
                total_part_size += len(entry.encode("utf-8"))

        print(f"   ✅ {out_filename.name} — {total_part_size/1024:.2f} КБ")

    print("\n" + "=" * 70)
    print("🎉 ГЕНЕРАЦИЯ УСПЕШНО ЗАВЕРШЕНА!")
    print("=" * 70)
    print(f"   📊 Всего обработано файлов: {len(entries)}")
    print(f"   📁 Создано выходных файлов: {total_parts}")
    print(f"   📂 Файлы сохранены в: {PROJECT_DIR}")
    print(f"\n💡 Пример имени файла: {OUTPUT_PREFIX}_{date_str}_1_{total_parts}.txt")
    print("=" * 70)

if __name__ == "__main__":
    main()