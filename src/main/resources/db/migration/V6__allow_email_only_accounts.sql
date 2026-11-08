-- Google sign-in accounts may have an email address without a phone number.
ALTER TABLE users ALTER COLUMN phone_number DROP NOT NULL;
