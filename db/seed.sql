INSERT INTO clients (full_name, phone, email) VALUES
('Иванов Иван Иванович', '+79001112233', 'ivanov@mail.ru'),
('Петрова Мария Сергеевна', '+79002223344', 'petrova@mail.ru'),
('Сидоров Алексей Викторович', '+79003334455', 'sidorov@mail.ru'),
('Кузнецова Ольга Дмитриевна', '+79004445566', 'kuznecova@mail.ru'),
('Смирнов Дмитрий Павлович', '+79005556677', NULL);

INSERT INTO repair_requests (client_id, device_type, problem_description, status, priority, cost, created_at) VALUES
(1, 'Ноутбук', 'Не включается, подозрение на блок питания', 'NEW', 'HIGH', 0, now() - interval '10 days'),
(1, 'ПК', 'Синий экран при загрузке Windows', 'IN_PROGRESS', 'NORMAL', 1500.00, now() - interval '9 days'),
(2, 'Принтер', 'Не печатает, замятие бумаги', 'DONE', 'LOW', 800.00, now() - interval '8 days'),
(2, 'Ноутбук', 'Разбит экран после падения', 'WAITING_PARTS', 'URGENT', 5200.00, now() - interval '7 days'),
(3, 'ПК', 'Шумит кулер, перегрев процессора', 'DIAGNOSTICS', 'NORMAL', 0, now() - interval '6 days'),
(3, 'Моноблок', 'Не работает Wi-Fi модуль', 'DONE', 'NORMAL', 1200.00, now() - interval '5 days'),
(4, 'Ноутбук', 'Залили клавиатуру кофе', 'IN_PROGRESS', 'HIGH', 3000.00, now() - interval '4 days'),
(4, 'Принтер', 'Полосит при печати', 'CANCELLED', 'LOW', 0, now() - interval '3 days'),
(5, 'ПК', 'Установка Windows и драйверов', 'NEW', 'NORMAL', 0, now() - interval '2 days'),
(5, 'Ноутбук', 'Замена термопасты, чистка от пыли', 'DONE', 'LOW', 900.00, now() - interval '1 days');
