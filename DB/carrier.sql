-- 운송사
CREATE TABLE carrier(
  carrier_id BIGSERIAL PRIMARY KEY,
  carrier_name VARCHAR(100) NOT NULL,
  carrier_contact VARCHAR(100),
  manager_name VARCHAR(100),
  carrier_status VARCHAR(30)
);

INSERT INTO carrier(carrier_name, carrier_contact, manager_name, carrier_status)
VALUES
 ('한진', '051-111-1111', '김과장', '승인'),
 ('CJ대한통운', '051-222-2222', '이대리', '승인'),
 ('동방', '051-333-3333', '박부장', '승인'),
 ('롯데택배', '051-444-4444', '박과장', '승인');
 
