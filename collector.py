import os
import sys
from pathlib import Path

# ============================================================================
# ЧТО ДЕЛАЕТ ЭТОТ СКРИПТ:
# ============================================================================
# Скрипт собирает все файлы проекта Android Studio с расширениями:
#   *.kt, *.kts, *.xml, *.toml, *.properties, *.pro, *.gitignore, *.md,
#   *.svg, *.png, *.json, *.csv, *.pdf, *.zip, *.db, *.html
#
# И сохраняет их в один или несколько TXT-файлов в формате:
#   путь/к/файлу.kt>>>
#   содержимое файла
#   <<<конец файла
#
# ОСОБЕННОСТИ:
#   ✔ Каждый выходной файл НЕ превышает 100 КБ
#   ✔ Исходные файлы НЕ разрываются между частями
#   ✔ Бинарные файлы (png, zip, pdf и др.) заменяются на заглушку
#   ✔ Исключаются системные папки (.git, build, .idea, gradle, captures)
#   ✔ Перед началом показывает статистику и запрашивает подтверждение
#   ✔ В процессе показывает, какой файл обрабатывается
#
# СОЗДАЁТ ФАЙЛЫ:
#   collected_project_1.txt, collected_project_2.txt и т.д. в той же папке
# ============================================================================

# Конфигурация
PROJECT_DIR = Path(r"C:\Users\spele\AndroidStudioProjects\SpeleoStart")
EXTENSIONS = {".kt", ".kts", ".xml", ".toml", ".properties", ".pro",
              ".gitignore", ".md", ".svg", ".png", ".json", ".csv",
              ".pdf", ".zip", ".db", ".html"}
OUTPUT_BASE_NAME = "collected_project"
MAX_FILE_SIZE = 100 * 1024  # 100 КБ

# -------------------------------
def show_header():
    """Показывает заголовок с описанием функционала"""
    print("=" * 70)
    print("📦 СБОРЩИК ФАЙЛОВ ПРОЕКТА Android Studio")
    print("=" * 70)
    print("\n📌 Скрипт соберёт все файлы с расширениями:")
    print("   .kt, .kts, .xml, .toml, .properties, .pro, .gitignore")
    print("   .md, .svg, .png, .json, .csv, .pdf, .zip, .db, .html")
    print("\n📄 Формат записи каждого файла:")
    print("   путь/файла.kt>>>")
    print("   содержимое")
    print("   <<<конец файла")
    print(f"\n⚡ Лимит одного выходного файла: {MAX_FILE_SIZE//1024} КБ")
    print("   ➜ При превышении создаётся следующий файл")
    print("   ➜ Исходные файлы не разбиваются на части")
    print("\n🚫 Исключаются папки: .git, build, .idea, gradle, captures")
    print(f"\n💾 Выходные файлы: {OUTPUT_BASE_NAME}_1.txt, {OUTPUT_BASE_NAME}_2.txt, ...")
    print(f"   Расположение: {PROJECT_DIR}")
    print("=" * 70)
    print()

def get_all_files(project_root, extensions):
    """Собирает все файлы с нужными расширениями."""
    found = []
    for root, dirs, files in os.walk(project_root):
        # Исключаем системные папки
        dirs[:] = [d for d in dirs if d not in {".git", "build", ".idea", "gradle", "captures"}]
        for file in files:
            file_path = Path(root) / file
            if file_path.suffix in extensions or file in extensions:
                rel_path = file_path.relative_to(project_root)
                found.append((rel_path, file_path))
    return found

def format_file_entry(rel_path, content):
    """Форматирует одну запись для выходного файла."""
    path_str = str(rel_path).replace("\\", "/")
    return f"{path_str}>>>\n{content}\n<<<конец файла\n\n"

def estimate_output_files(total_raw_size, num_files):
    """Примерная оценка количества выходных файлов."""
    # Прикидка: на каждый файл добавляется ~50 байт служебной информации (путь и разделители)
    estimated_total_with_overhead = total_raw_size + num_files * 60
    estimated_parts = max(1, (estimated_total_with_overhead + MAX_FILE_SIZE - 1) // MAX_FILE_SIZE)
    return estimated_parts

def main():
    show_header()
    
    print("🔍 Поиск файлов в проекте...")
    if not PROJECT_DIR.exists():
        print(f"❌ Ошибка: папка проекта не найдена: {PROJECT_DIR}")
        sys.exit(1)

    files = get_all_files(PROJECT_DIR, EXTENSIONS)
    total_files = len(files)

    if total_files == 0:
        print("⚠️ Не найдено ни одного файла с указанными расширениями.")
        return

    # Суммарный размер исходных файлов
    total_raw_size = sum(f[1].stat().st_size for f in files)
    estimated_parts = estimate_output_files(total_raw_size, total_files)

    print(f"\n📊 СТАТИСТИКА:")
    print(f"   📁 Найдено файлов: {total_files}")
    print(f"   💾 Общий исходный размер: {total_raw_size / 1024:.2f} КБ")
    print(f"   📄 Примерное количество генерируемых файлов: ~{estimated_parts}")
    print(f"   📂 Папка сохранения: {PROJECT_DIR}")
    if estimated_parts > 1:
        print(f"   ⚠️  Внимание: из-за лимита в {MAX_FILE_SIZE//1024} КБ будет создано {estimated_parts} файла(ов)")

    print("\n" + "=" * 70)
    answer = input("❓ Продолжить генерацию? (y/n): ").strip().lower()
    if answer not in ("y", "yes", "да", "+", "1"):
        print("❌ Отменено пользователем.")
        return

    print("\n🚀 НАЧАЛО ГЕНЕРАЦИИ...")
    print("=" * 70)
    
    # Генерация файлов
    part_num = 1
    current_output = None
    current_size = 0
    current_content_buffer = []
    files_written = 0

    for idx, (rel_path, full_path) in enumerate(files, 1):
        # Читаем содержимое файла
        try:
            if full_path.suffix in {".png", ".jpg", ".jpeg", ".zip", ".pdf"}:
                # Бинарные файлы — заменяем содержимое на уведомление
                content = f"[BINARY FILE: {full_path.name} — содержимое не показано]"
            else:
                with open(full_path, "r", encoding="utf-8", errors="replace") as f:
                    content = f.read()
        except Exception as e:
            content = f"[ОШИБКА ЧТЕНИЯ: {e}]"

        entry = format_file_entry(rel_path, content)
        entry_size = len(entry.encode("utf-8"))

        # Проверяем, влезет ли запись в текущий файл
        if current_output is None or (current_size + entry_size > MAX_FILE_SIZE and current_size > 0):
            # Закрываем текущий файл, если он открыт
            if current_output is not None:
                current_output.write("".join(current_content_buffer))
                current_output.close()
                print(f"   ✅ Файл сохранён: {out_filename} ({(current_size/1024):.2f} КБ)")

            # Создаём новый выходной файл
            out_filename = PROJECT_DIR / f"{OUTPUT_BASE_NAME}_{part_num}.txt"
            current_output = open(out_filename, "w", encoding="utf-8")
            current_content_buffer = []
            current_size = 0
            print(f"\n📄 Создан новый файл: {out_filename}")
            part_num += 1

        # Добавляем запись в буфер
        current_content_buffer.append(entry)
        current_size += entry_size
        files_written += 1
        
        # Показываем прогресс
        percent = (idx / total_files) * 100
        print(f"   [{idx:3d}/{total_files}] {rel_path} ({(entry_size/1024):.2f} КБ, прогресс: {percent:.1f}%)")

    # Закрываем последний файл
    if current_output is not None:
        current_output.write("".join(current_content_buffer))
        current_output.close()
        print(f"   ✅ Файл сохранён: {out_filename} ({(current_size/1024):.2f} КБ)")

    print("\n" + "=" * 70)
    print("🎉 ГЕНЕРАЦИЯ УСПЕШНО ЗАВЕРШЕНА!")
    print("=" * 70)
    print(f"   📊 Всего обработано файлов: {files_written}")
    print(f"   📁 Создано выходных файлов: {part_num - 1}")
    print(f"   📂 Файлы сохранены в: {PROJECT_DIR}")
    print(f"\n💡 Совет: откройте полученные .txt-файлы в любом текстовом редакторе")
    print("=" * 70)

if __name__ == "__main__":
    main()