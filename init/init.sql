-- Create databases
CREATE DATABASE authdb;
CREATE DATABASE userdb;
CREATE DATABASE appointmentsdb;

-- Create roles (one per service)
CREATE USER authuser WITH ENCRYPTED PASSWORD 'auth_password';
CREATE USER useruser WITH ENCRYPTED PASSWORD 'user_password';
CREATE USER appointmentuser WITH ENCRYPTED PASSWORD 'appointment_pass';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE authdb TO auth_user;
GRANT ALL PRIVILEGES ON DATABASE userdb TO user_user;
GRANT ALL PRIVILEGES ON DATABASE appointmentsdb TO appointment_user;
