-- Insert roles (ignore if already exist)
INSERT INTO roles (name) VALUES ('ADMIN') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('LIBRARIAN') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('USER') ON CONFLICT (name) DO NOTHING;

-- Insert demo users with BCrypt-encoded passwords
-- admin / admin123
INSERT INTO users (username, email, password, first_name, last_name, enabled)
VALUES ('admin', 'admin@bookflow.com', '$2a$10$sRHjdObarVRd7yTAWQPwJegIQkSzJtuGfy4d5ZTlCQuT6/T7PyuTq', 'Admin', 'User', true)
ON CONFLICT (username) DO NOTHING;

-- librarian / librarian123
INSERT INTO users (username, email, password, first_name, last_name, enabled)
VALUES ('librarian', 'librarian@bookflow.com', '$2a$10$CbeAGfkhaJb16oX9nPkeKuCheuyzPssoL7bTIf/yEsfTqxeq9Qrj.', 'Library', 'Manager', true)
ON CONFLICT (username) DO NOTHING;

-- user / user123
INSERT INTO users (username, email, password, first_name, last_name, enabled)
VALUES ('user', 'user@bookflow.com', '$2a$10$7m6fZdpNeg5TlgraKkZZfOTzykZEgB4HAV5d0BwCAwN9z0lmZFTpy', 'John', 'Doe', true)
ON CONFLICT (username) DO NOTHING;

-- Assign roles to users
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'librarian' AND r.name = 'LIBRARIAN'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'user' AND r.name = 'USER'
ON CONFLICT DO NOTHING;

-- Backfill token/fine columns for existing borrowing rows (added by renewal-token feature)
UPDATE borrowings SET renewal_tokens_acquired = 0 WHERE renewal_tokens_acquired IS NULL;
UPDATE borrowings SET renewal_tokens_used = 0 WHERE renewal_tokens_used IS NULL;
UPDATE borrowings SET fine_amount = 0 WHERE fine_amount IS NULL;
UPDATE borrowings SET fine_cleared = false WHERE fine_cleared IS NULL;

-- Insert some sample books
INSERT INTO books (title, author, isbn, publication_year, publisher, total_copies, available_copies)
VALUES ('The Great Gatsby', 'F. Scott Fitzgerald', '978-0743273565', 1925, 'Scribner', 5, 3)
ON CONFLICT (isbn) DO NOTHING;

INSERT INTO books (title, author, isbn, publication_year, publisher, total_copies, available_copies)
VALUES ('1984', 'George Orwell', '978-0451524935', 1949, 'Signet Classic', 4, 2)
ON CONFLICT (isbn) DO NOTHING;

INSERT INTO books (title, author, isbn, publication_year, publisher, total_copies, available_copies)
VALUES ('Clean Code', 'Robert C. Martin', '978-0132350884', 2008, 'Prentice Hall', 3, 3)
ON CONFLICT (isbn) DO NOTHING;

INSERT INTO books (title, author, isbn, publication_year, publisher, total_copies, available_copies)
VALUES ('The Pragmatic Programmer', 'David Thomas', '978-0135957059', 2019, 'Addison-Wesley', 2, 1)
ON CONFLICT (isbn) DO NOTHING;

INSERT INTO books (title, author, isbn, publication_year, publisher, total_copies, available_copies)
VALUES ('Design Patterns', 'Gang of Four', '978-0201633610', 1994, 'Addison-Wesley', 3, 2)
ON CONFLICT (isbn) DO NOTHING;

