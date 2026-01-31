# Quick Start Guide - Краткая шпаргалка

## Фазы внедрения

### ⏱️ 10 минут - Базовая подготовка

```bash
# 1. Скопируйте новые классы
cp SAPAuthFilter.java → src/com/commentary/security/
cp SAPSessionListener.java → src/com/commentary/security/

# 2. Замените конфигурацию
cp web.xml → WebContent/WEB-INF/web.xml

# 3. Очистите проект в Eclipse
Project → Clean → Build All

# 4. Проверьте ошибки компиляции
Problems tab должен быть пуст
```

### ⏱️ 15 минут - Конфигурация Tomcat

```bash
# 1. Создайте context.xml
vi $CATALINA_BASE/conf/Catalina/localhost/commentary.xml
(скопируйте содержимое из context.xml)

# 2. Отредактируйте параметры БД
- Строка: url="jdbc:sybase:Tds:..."
- Замените: [HOST], [PORT], [DATABASE]
- Замените: [USERNAME], [PASSWORD]

# 3. Создайте logging.properties
cp logging.properties → $CATALINA_BASE/conf/logging.properties

# 4. Установите параметры JVM в setenv.sh
export CATALINA_OPTS="-Dbobj.cms=bobjogb.sap.commerzbank.com:6400"
```

### ⏱️ 5 минут - Error Pages (опционально)

```bash
# Создайте папку error pages
mkdir -p WebContent/error

# Скопируйте JSP файлы
cp forbidden.jsp → WebContent/error/
# (остальные error pages для 404, 500 создайте аналогично)
```

---

## Важные файлы и их назначение

```
📁 SAPAuthFilter.java
   ↳ Основной фильтр аутентификации
   ↳ Проверка токена, роли, ошибок
   ↳ Размещение: src/com/commentary/security/

📁 SAPSessionListener.java
   ↳ Управление жизненным циклом сессии
   ↳ Cleanup при завершении сессии
   ↳ Размещение: src/com/commentary/security/

📁 web.xml
   ↳ Конфигурация веб-приложения
   ↳ Регистрация фильтра и листнера
   ↳ HTTPS enforcement и error pages
   ↳ Размещение: WebContent/WEB-INF/web.xml

📁 context.xml → commentary.xml
   ↳ Конфигурация Tomcat для приложения
   ↳ Пулинг соединений, cookies, session
   ↳ Размещение: $CATALINA_BASE/conf/Catalina/localhost/commentary.xml

📁 logging.properties
   ↳ Конфигурация логирования
   ↳ File handler, console handler, уровни
   ↳ Размещение: $CATALINA_BASE/conf/logging.properties
```

---

## Проверочный список перед запуском

### ✅ В Eclipse проекте

- [ ] Нет ошибок компиляции (Problems tab)
- [ ] SAPAuthFilter.java скомпилирован
- [ ] SAPSessionListener.java скомпилирован
- [ ] web.xml открывается без ошибок
- [ ] Все import'ы от SAP BO SDK присутствуют

### ✅ В Tomcat

- [ ] commentary.xml создан в conf/Catalina/localhost/
- [ ] Параметры БД отредактированы
- [ ] CATALINA_OPTS содержит -Dbobj.cms=...
- [ ] JAR файлы SAP BO SDK в lib/ или WEB-INF/lib/
- [ ] logging.properties скопирован в conf/

### ✅ В базе данных

- [ ] DataSource доступен: jdbc/COMCON
- [ ] Учётные данные БД правильны
- [ ] Таблицы CDBEGB.EGB_RCCOMMENT существуют

### ✅ В SAP BO

- [ ] Пользователи добавлены в группу ZPB_S.GF_COMMENT_BO
- [ ] CMS доступен на указанном хосте и порту

---

## Команды для тестирования

### Проверка доступности CMS

```bash
telnet bobjogb.sap.commerzbank.com 6400

# Если соединение установлено - CMS доступен
# Нажмите Ctrl+] затем quit
```

### Проверка логов

```bash
# Linux/Mac
tail -f $CATALINA_BASE/logs/catalina.out
tail -f $CATALINA_BASE/logs/commentary-*.log

# Windows
type %CATALINA_BASE%\logs\catalina.out
```

### Проверка DataSource

```bash
# В Eclipse Console:
# 1. Развернуть приложение на Tomcat
# 2. Проверить нет ошибок "DataSource not found"
# 3. Искать в логах: "getConnection"
```

---

## Типичные ошибки и быстрые решения

### ❌ "Cannot resolve symbol SAPAuthFilter"

**Решение:**
```
1. Project → Clean
2. Project → Build All
3. Если не помогает - удалить проект из Eclipse и переимпортировать
```

### ❌ "Connection refused" или "CMS not reachable"

**Решение:**
```
1. Проверить параметр: -Dbobj.cms=...
2. Проверить firewall: telnet host port
3. Проверить что CMS запущен на SAP BO сервере
```

### ❌ "Access denied: Missing role ZPB_S.GF_COMMENT_BO"

**Решение:**
```
1. Открыть CMC (Central Management Console)
2. Users and Groups → найти пользователя
3. Добавить в группу ZPB_S.GF_COMMENT_BO
4. Перезагрузить браузер или очистить cookies
```

### ❌ HTTP 401 вместо автоматической аутентификации

**Решение:**
```
1. Проверить что Web Intelligence передаёт токен в iframe src
2. Проверить что параметр: ?sToken=... в URL
3. Посмотреть логи: "Token aus URL-Parameter extrahiert"
```

### ❌ HTTP 503 Service Unavailable

**Решение:**
```
1. CMS недоступен - проверить CMS сервер
2. Проверить: telnet host port
3. Посмотреть логи: "CMS ist derzeit nicht erreichbar"
```

---

## Логирование - ключевые сообщения

### ✅ Нормальный процесс

```
INFO: SAPAuthFilter initialisiert
FINE: HTTP-Session erstellt: abc123...
FINE: Keine gültige SAP BO Sitzung vorhanden. Versuche Token-basierte Anmeldung.
FINE: Token aus URL-Parameter 'sToken' extrahiert
INFO: Erfolgreich mit Token bei SAP BO angemeldet
FINE: Sitzung ist noch aktiv. Benutzer ID: 42
INFO: Benutzer hat erforderliche Rolle: ZPB_S.GF_COMMENT_BO
FINE: Neue SAP BO Sitzung erstellt
```

### ⚠️ Предупреждения

```
WARNING: Kein gültiger Logon-Token gefunden
WARNING: Token-Format ungültig oder zu kurz
WARNING: Benutzer hat NICHT die erforderliche Rolle
WARNING: Sitzung ist nicht mehr aktiv oder ungültig
WARNING: Fehler beim Abmelden von SAP BO
```

### 🔴 Ошибки

```
SEVERE: CMS SessionManager konnte nicht erhalten werden
SEVERE: CMS-Verbindungsfehler: connection refused
SEVERE: SAP BO SDK Fehler: ...
SEVERE: Fehler bei der Rollenprüfung: ...
```

---

## Шпаргалка команд

```bash
# Перезагрузка Tomcat
$CATALINA_HOME/bin/shutdown.sh
$CATALINA_HOME/bin/startup.sh

# Просмотр логов в реальном времени
tail -f $CATALINA_BASE/logs/commentary-*.log

# Проверка портов
netstat -tlnp | grep 8080
netstat -tlnp | grep 6400

# Kill процесс Tomcat если не закрывается
ps aux | grep tomcat
kill -9 PID

# Поиск ошибок в логах
grep -i "error" $CATALINA_BASE/logs/catalina.out
grep -i "exception" $CATALINA_BASE/logs/catalina.out
grep "SEVERE" $CATALINA_BASE/logs/commentary-*.log

# Проверка размера логов
du -sh $CATALINA_BASE/logs/
```

---

## Ссылки на документацию

- **Подробное руководство:** INSTALL_DE.md
- **Анализ и оценка:** tomcat_migration_analysis.md
- **Полное сравнение:** COMPARISON.md
- **Все изменения:** README_REFACTORED.md

---

## После успешного запуска

### 1️⃣ Проведите базовое тестирование

```
- Откройте Web Intelligence отчёт
- Добавьте iframe с Commentary
- Проверьте автоматическая аутентификация
- Добавьте комментарий
- Проверьте в логах правильные сообщения
```

### 2️⃣ Проведите тестирование ошибок

```
- Попробуйте как пользователь БЕЗ роли (должна быть 403)
- Отключите CMS (должна быть 503)
- Используйте неправильный токен (должен быть redirect на login)
```

### 3️⃣ Настройте мониторинг

```
- Настройте алерты на SEVERE логи
- Мониторьте размер логов
- Проверяйте еженедельно логи на ошибки
```

### 4️⃣ Подготовьтесь к Production

```
- Резервная копия БД
- Резервная копия конфигурации
- Plan для rollback если необходимо
- Информирование пользователей о обновлении
```

---

**Статус:** Готово к Production
**Время на реализацию:** 30-45 минут
**Версия:** 2.0