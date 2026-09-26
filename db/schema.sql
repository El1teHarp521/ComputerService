-- Удаляем таблицу repair_requests, если она существует (для перезапуска скрипта)
DROP TABLE IF EXISTS repair_requests;
-- Удаляем таблицу clients, если она существует
DROP TABLE IF EXISTS clients;

-- Создаем таблицу клиентов
CREATE TABLE clients (
    id SERIAL PRIMARY KEY, -- Уникальный идентификатор, автоматически увеличивается (1, 2, 3...)
    full_name VARCHAR(150) NOT NULL, -- ФИО, строка до 150 символов, обязательное поле
    phone VARCHAR(20) NOT NULL UNIQUE, -- Телефон, строка до 20 символов, обязательное и уникальное поле
    email VARCHAR(100) UNIQUE, -- Email, строка до 100 символов, уникальное поле (может быть NULL)
    registered_at TIMESTAMP NOT NULL DEFAULT now() -- Дата регистрации, по умолчанию текущее время
);

-- Создаем таблицу заявок на ремонт
CREATE TABLE repair_requests (
    id SERIAL PRIMARY KEY, -- Уникальный идентификатор заявки
    client_id INT NOT NULL REFERENCES clients(id), -- Ссылка на ID клиента из таблицы clients (внешний ключ)
    device_type VARCHAR(50) NOT NULL, -- Тип устройства, строка до 50 символов
    problem_description TEXT NOT NULL, -- Описание проблемы, текстовое поле (может быть длинным)
    status VARCHAR(20) NOT NULL, -- Статус заявки (NEW, DONE и т.д.)
    priority VARCHAR(20) NOT NULL, -- Приоритет (LOW, HIGH и т.д.)
    cost NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (cost >= 0), -- Стоимость, число с 2 знаками после запятой, по умолчанию 0, не может быть отрицательной
    created_at TIMESTAMP NOT NULL DEFAULT now(), -- Дата создания заявки
    completed_at TIMESTAMP -- Дата завершения (может быть NULL, пока заявка не выполнена)
);

-- Создаем индекс для ускорения поиска заявок по client_id
CREATE INDEX idx_repair_requests_client_id ON repair_requests(client_id);
-- Создаем индекс для ускорения фильтрации заявок по статусу
CREATE INDEX idx_repair_requests_status ON repair_requests(status);