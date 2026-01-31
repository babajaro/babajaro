# Миграция Commentary на Tomcat - Инструкции по установке

## Обзор

Это руководство описывает процесс миграции приложения Commentary с SAP NetWeaver на Apache Tomcat с полной поддержкой аутентификации и проверки ролей SAP BusinessObjects.

---

## Версия 2.0 - Основные исправления

### Что было исправлено:

1. **Безопасность (критические уязвимости)**
   - ✅ Добавлено экранирование переменных в CMS запросах (escapeSAPQuery)
   - ✅ Удален открытый тестовый endpoint check_sdk.jsp
   - ✅ Добавлена валидация формата токена перед использованием
   - ✅ Добавлена проверка длины токена (MIN_TOKEN_LENGTH)

2. **Надёжность (обработка ошибок)**
   - ✅ Добавлена разумная обработка исключений SDKException
   - ✅ Различие между ошибками подключения (503) и неавторизованными (401)
   - ✅ Правильная логирование всех ошибок
   - ✅ Обработка исключений в sessionDestroyed()

3. **Конфигурация**
   - ✅ Улучшен web.xml с безопасностью и документацией
   - ✅ Создан улучшенный context.xml с пулингом соединений
   - ✅ Добавлены HTTP Security Constraints (принудительно HTTPS)

---

## Файлы для миграции

Нижеследующие файлы должны быть скопированы в ваш проект:

```
src/
  └── com/
      └── commentary/
          └── security/
              ├── SAPAuthFilter.java          (НОВЫЙ - обновлённая версия)
              └── SAPSessionListener.java     (НОВЫЙ - обновлённая версия)

WebContent/
  └── WEB-INF/
      └── web.xml                             (ЗАМЕНА - обновленная версия)

$CATALINA_BASE/conf/Catalina/localhost/
  └── commentary.xml                          (НОВЫЙ - переименованный context.xml)
```

---

## Пошаговая установка

### Шаг 1: Подготовка проекта в Eclipse

```bash
1. Откройте проект Commentary в Eclipse
2. Скопируйте файлы:
   - SAPAuthFilter.java → src/com/commentary/security/
   - SAPSessionListener.java → src/com/commentary/security/
3. Замените файл:
   - web.xml → WebContent/WEB-INF/web.xml
4. Очистите проект (Project → Clean)
5. Проверьте отсутствие ошибок компиляции (Problems tab)
```

### Шаг 2: Конфигурация Tomcat

```bash
1. Откройте: $CATALINA_BASE/conf/Catalina/localhost/

2. Создайте новый файл: commentary.xml
   (используйте содержимое из предоставленного context.xml)

3. Отредактируйте параметры DataSource:
   - Замените [HOST], [PORT], [DATABASE] на реальные значения
   - Замените [USERNAME], [PASSWORD] на учётные данные БД
   
   Пример:
   url="jdbc:sybase:Tds:bobjogb.sap.commerzbank.com:15001/CDBEGB"
```

### Шаг 3: Параметры запуска Tomcat

```bash
# Добавьте в $CATALINA_BASE/bin/setenv.sh или setenv.bat:

# Linux/Mac
export CATALINA_OPTS="-Dbobj.cms=bobjogb.sap.commerzbank.com:6400"

# Windows (setenv.bat)
set CATALINA_OPTS=-Dbobj.cms=bobjogb.sap.commerzbank.com:6400
```

### Шаг 4: JAR-зависимости

Убедитесь, что следующие JAR-файлы находятся в:
`$CATALINA_BASE/lib/` или `WebContent/WEB-INF/lib/`

```
Из SAP BO SDK:
- celib.jar
- cejava.jar
- ceutility.jar
- com.businessobjects.CMS.SDK.jar
- com.businessobjects.enterprise.queries.jar
- common-collections-2.1.1.jar
- *.jar из SAP BO SDK\lib\

Стандартные:
- javax.servlet-api-3.1.0.jar (уже в Tomcat)
```

### Шаг 5: Развёртывание

```bash
# Вариант 1: Экспорт как WAR файл
1. Right-click на проект → Export → WAR file
2. Выберите путь: $CATALINA_BASE/webapps/commentary.war
3. Перезагрузите Tomcat

# Вариант 2: Прямое развёртывание
1. Скопируйте папку WebContent в:
   $CATALINA_BASE/webapps/commentary/
2. Перезагрузите Tomcat
```

---

## Тестирование

### Проверка 1: Логирование

```bash
# Просмотрите логи Tomcat:
$CATALINA_BASE/logs/catalina.out

# Ищите сообщения:
- "SAPAuthFilter initialisiert"
- "HTTP-Session erstellt"
- "Neue SAP BO Sitzung für Benutzer ID: ..."
```

### Проверка 2: Доступ из Web Intelligence

```bash
1. Откройте Web Intelligence отчёт в BI Launchpad
2. Добавьте iframe, указывающий на Commentary:
   <iframe src="http://localhost:8080/commentary/index.jsp"></iframe>

3. Проверьте:
   - Приложение загружается без ошибок
   - Логин происходит автоматически (из текущей сессии BO)
   - Пользователь может добавлять комментарии (если есть роль)
```

### Проверка 3: Отсутствие роли

```bash
1. Если пользователь НЕ имеет роли ZPB_S.GF_COMMENT_BO:
   - Должна появиться ошибка 403 (Forbidden)
   - Сообщение: "Zugriff verweigert: Fehlende Rolle ZPB_S.GF_COMMENT_BO"
```

### Проверка 4: CMS недоступен

```bash
1. Остановите CMS или отключите сеть
2. Попробуйте обновить страницу
3. Должна появиться ошибка 503 (Service Unavailable)
   (не 401/401 как было раньше)
```

---

## Различия между версиями

| Функция | Оригинальный код | Версия 2.0 |
|---------|------------------|-----------|
| Экранирование токенов | ❌ SQL Injection | ✅ escapeSAPQuery() |
| Обработка ошибок | ❌ Молчаливые отказы | ✅ Подробное логирование |
| Валидация токена | ❌ Нет проверки | ✅ Проверка длины и формата |
| Различие 401 vs 503 | ❌ Всегда 401 | ✅ Правильные коды |
| Обработка logoff | ❌ Может выбросить исключение | ✅ Try-catch блок |
| Документация | ❌ Минимальная | ✅ Подробные комментарии |
| HTTPS enforcement | ⚠️ Опционально | ✅ Обязательно в web.xml |

---

## Решение проблем

### Проблема: "Token format invalid or too short"

**Причина:** Токен из Cookie/URL параметра имеет неправильный формат

**Решение:**
```
1. Проверьте, что Web Intelligence передаёт правильный токен
2. Проверьте, что iframe src содержит параметр ?sToken=...
3. Проверьте логи Tomcat для деталей
```

### Проблема: "CMS SessionManager could not be obtained"

**Причина:** CMS не инициализирован или недоступен

**Решение:**
```
1. Проверьте параметр запуска: -Dbobj.cms=...
2. Проверьте, что CMS слушает на указанном порту:
   telnet bobjogb.sap.commerzbank.com 6400
3. Проверьте firewall правила
```

### Проблема: "User does not have required role"

**Причина:** Пользователь не добавлен в группу ZPB_S.GF_COMMENT_BO

**Решение:**
```
1. Откройте SAP BO CMC (Central Management Console)
2. Перейдите в Users and Groups
3. Найдите пользователя и добавьте его в группу ZPB_S.GF_COMMENT_BO
4. Очистите кэш браузера
5. Перезагрузитесь
```

### Проблема: "Session expired"

**Причина:** HTTP-сессия Tomcat истекла

**Решение:**
```
1. Увеличьте timeout в web.xml:
   <timeout>60</timeout>  (в минутах)
2. Проверьте, что CMS сессия НЕ истекла одновременно
```

---

## Production Checklist

Перед развёртыванием в Production проверьте:

- [ ] Все JAR-зависимости добавлены в $CATALINA_BASE/lib/
- [ ] context.xml настроен с правильной DataSource
- [ ] HTTPS включён (transport-guarantee=CONFIDENTIAL)
- [ ] Cookies настроены: SameSite=None, Secure=true, HttpOnly=true
- [ ] Логирование настроено (logging.properties)
- [ ] Резервные копии SQL баз данных сделаны
- [ ] Проведено тестирование с реальными пользователями
- [ ] Все пользователи добавлены в группу ZPB_S.GF_COMMENT_BO
- [ ] Firewall правила настроены для CMS
- [ ] Проведено load-тестирование (минимум 50 одновременных пользователей)

---

## Контакты и поддержка

При возникновении проблем:

1. Проверьте логи: $CATALINA_BASE/logs/
2. Включите DEBUG логирование в logging.properties
3. Свяжитесь с администратором SAP BO
4. Если CMS недоступен, проверьте сетевые настройки
