DROP TABLE IF EXISTS repair_requests;
DROP TABLE IF EXISTS clients;

CREATE TABLE clients (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(100) UNIQUE,
    registered_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE repair_requests (
    id SERIAL PRIMARY KEY,
    client_id INT NOT NULL REFERENCES clients(id),
    device_type VARCHAR(50) NOT NULL,
    problem_description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    cost NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (cost >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    completed_at TIMESTAMP
);

CREATE INDEX idx_repair_requests_client_id ON repair_requests(client_id);
CREATE INDEX idx_repair_requests_status ON repair_requests(status);
