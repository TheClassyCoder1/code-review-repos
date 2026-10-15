DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS invoice_lines;
DROP TABLE IF EXISTS invoices;
DROP TABLE IF EXISTS transcripts;
DROP TABLE IF EXISTS student_guardians;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS guardian_documents;
DROP TABLE IF EXISTS guardians;
DROP TABLE IF EXISTS integration_keys;
DROP TABLE IF EXISTS portal_invites;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('admin', 'bursar', 'registrar', 'guardian')),
    guardian_id INTEGER REFERENCES guardians (id)
);

CREATE TABLE guardians (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name TEXT NOT NULL,
    email TEXT NOT NULL,
    phone TEXT,
    photo BLOB
);

CREATE TABLE guardian_documents (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    guardian_id INTEGER NOT NULL REFERENCES guardians (id),
    filename TEXT NOT NULL,
    uploaded_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE students (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_number TEXT NOT NULL UNIQUE,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    grade_level INTEGER NOT NULL CHECK (grade_level BETWEEN 1 AND 12),
    enrollment_status TEXT NOT NULL DEFAULT 'active'
        CHECK (enrollment_status IN ('active', 'withdrawn', 'graduated', 'suspended')),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE student_guardians (
    student_id INTEGER NOT NULL REFERENCES students (id),
    guardian_id INTEGER NOT NULL REFERENCES guardians (id),
    relationship TEXT NOT NULL,
    PRIMARY KEY (student_id, guardian_id)
);

CREATE TABLE invoices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    reference TEXT NOT NULL UNIQUE,
    student_id INTEGER NOT NULL REFERENCES students (id),
    guardian_id INTEGER NOT NULL REFERENCES guardians (id),
    term_code TEXT NOT NULL,
    amount_cents INTEGER NOT NULL CHECK (amount_cents >= 0),
    paid_cents INTEGER NOT NULL DEFAULT 0,
    due_date TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'paid', 'void')),
    note TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE invoice_lines (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id INTEGER NOT NULL REFERENCES invoices (id),
    description TEXT NOT NULL,
    amount_cents INTEGER NOT NULL
);

CREATE TABLE payments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id INTEGER NOT NULL REFERENCES invoices (id),
    amount_cents INTEGER NOT NULL CHECK (amount_cents > 0),
    gateway_ref TEXT NOT NULL UNIQUE,
    paid_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transcripts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id INTEGER NOT NULL REFERENCES students (id),
    term_code TEXT NOT NULL,
    course TEXT NOT NULL,
    credits REAL NOT NULL,
    grade_points REAL NOT NULL
);

CREATE TABLE integration_keys (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    label TEXT NOT NULL,
    key_hash TEXT NOT NULL UNIQUE,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE portal_invites (
    token TEXT PRIMARY KEY,
    guardian_id INTEGER NOT NULL REFERENCES guardians (id),
    expires_at TEXT NOT NULL,
    accepted_at TEXT
);
