SELECT 'CREATE DATABASE incident' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'incident')\gexec
SELECT 'CREATE DATABASE context' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'context')\gexec
SELECT 'CREATE DATABASE auth' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'auth')\gexec
SELECT 'CREATE DATABASE profile' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'profile')\gexec
SELECT 'CREATE DATABASE review' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'review')\gexec
SELECT 'CREATE DATABASE notification' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notification')\gexec
SELECT 'CREATE DATABASE classifier' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'classifier')\gexec
SELECT 'CREATE DATABASE course' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'course')\gexec
