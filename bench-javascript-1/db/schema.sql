CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE clinics (
  id SERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  address TEXT NOT NULL,
  timezone TEXT NOT NULL DEFAULT 'UTC'
);

CREATE TABLE patients (
  id SERIAL PRIMARY KEY,
  clinic_id INT NOT NULL REFERENCES clinics(id),
  first_name TEXT NOT NULL,
  last_name TEXT NOT NULL,
  date_of_birth DATE,
  phone TEXT,
  email TEXT
);

CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  email TEXT NOT NULL,
  username TEXT,
  role TEXT NOT NULL,
  patient_id INT REFERENCES patients(id),
  password_hash TEXT NOT NULL
);
CREATE UNIQUE INDEX users_email_key ON users (lower(email));

CREATE TABLE password_resets (
  user_id INT NOT NULL REFERENCES users(id),
  token_hash TEXT NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE doctors (
  id SERIAL PRIMARY KEY,
  clinic_id INT NOT NULL REFERENCES clinics(id),
  first_name TEXT NOT NULL,
  last_name TEXT NOT NULL,
  specialty TEXT NOT NULL,
  years_experience INT NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE doctor_shifts (
  id SERIAL PRIMARY KEY,
  doctor_id INT NOT NULL REFERENCES doctors(id),
  start_at TIMESTAMPTZ NOT NULL,
  end_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE appointments (
  id SERIAL PRIMARY KEY,
  reference TEXT NOT NULL UNIQUE,
  patient_id INT NOT NULL REFERENCES patients(id),
  doctor_id INT NOT NULL REFERENCES doctors(id),
  clinic_id INT NOT NULL REFERENCES clinics(id),
  start_at TIMESTAMPTZ NOT NULL,
  end_at TIMESTAMPTZ NOT NULL,
  status TEXT NOT NULL,
  reason TEXT,
  cancelled_at TIMESTAMPTZ,
  EXCLUDE USING gist (doctor_id WITH =, tstzrange(start_at, end_at) WITH &&) WHERE (status <> 'cancelled')
);

CREATE TABLE reminders (
  id SERIAL PRIMARY KEY,
  appointment_id INT NOT NULL REFERENCES appointments(id),
  send_at TIMESTAMPTZ NOT NULL,
  sent_at TIMESTAMPTZ,
  custom_message TEXT
);
