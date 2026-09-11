# Shop API

Backend для интернет-магазина на Spring Boot.

Это учебный проект, тут я разбираюсь с разработкой REST API, работой с бд, авторизацией, Docker.
Проект ещё не готов.

## Что уже реализовано

- пользователи и роли;
- товары и категории;
- оформление заказов;
- хранение данных в PostgreSQL;
- миграции базы данных через Flyway;
- запуск приложения и базы данных в Docker.

## Технологии

- Java 26
- Spring Boot 4.1.1
- Spring Data JPA
- Spring Security
- PostgreSQL 18
- Flyway
- Maven
- Docker Compose

## Как запустить проект

Для запуска понадобятся GIT и Docker Desktop.

Сначала нужно клонировать репозиторий:

```bash
git clone https://github.com/Ammm1s/shop-api.git
cd shop-api
```

Затем создать файл `.env` на основе примера:

```bash
cp .env.example .env
```

В файле `.env` нужно указать пароль для базы данных:

```dotenv
SHOP_DB_PASSWORD=your_password
```

После этого можно собрать и запустить проект:

```bash
docker compose up -d --build
```

Приложение будет доступно по адресу:

```text
http://localhost:8080
```

Например, можно получить список товаров или категорий:

```text
GET http://localhost:8080/products
GET http://localhost:8080/categories
```

Для части запросов используется HTTP Basic-аутентификация.

## Как остановить проект

Остановить контейнеры:

```bash
docker compose stop
```

Остановить и удалить контейнеры:

```bash
docker compose down
```

Файл `.env` содержит локальные настройки и не должен добавляться в Git.