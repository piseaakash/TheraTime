-- Create databases
CREATE DATABASE authdb;
CREATE DATABASE userdb;
CREATE DATABASE appointmentdb;

-- Create roles (one per service)
CREATE USER authuser WITH ENCRYPTED PASSWORD 'authpass';
CREATE USER useruser WITH ENCRYPTED PASSWORD 'userpass';
CREATE USER appointmentuser WITH ENCRYPTED PASSWORD 'appointmentpass';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE authdb TO auth_user;
GRANT ALL PRIVILEGES ON DATABASE userdb TO user_user;
GRANT ALL PRIVILEGES ON DATABASE appointmentdb TO appointment_user;
