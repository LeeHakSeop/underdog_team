CREATE TABLE users (
  user_id BIGSERIAL PRIMARY KEY,
  login_id VARCHAR(50) UNIQUE NOT NULL,
  password VARCHAR(100) NOT NULL,
  user_name VARCHAR(100),
  role_code VARCHAR(20) NOT NULL,
  status VARCHAR(20) DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT now()
);

ALTER TABLE carrier ADD COLUMN user_id BIGINT;
ALTER TABLE driver ADD COLUMN user_id BIGINT;

ALTER TABLE carrier
ADD CONSTRAINT fk_carrier_user
FOREIGN KEY (user_id) REFERENCES users(user_id);

ALTER TABLE driver
ADD CONSTRAINT fk_driver_user
FOREIGN KEY (user_id) REFERENCES users(user_id);