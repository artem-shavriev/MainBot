DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT,
  first_name VARCHAR(250) NOT NULL,
  last_name VARCHAR(250),
  user_name VARCHAR(250) NOT NULL,
  registered_at Timestamp NOT NULL,
  CONSTRAINT pk_user PRIMARY KEY (id),
  CONSTRAINT UQ_USER_user_name UNIQUE (user_name)
);