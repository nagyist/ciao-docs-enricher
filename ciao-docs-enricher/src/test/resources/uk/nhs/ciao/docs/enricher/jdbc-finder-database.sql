CREATE TABLE NAMES (id VARCHAR(20), name VARCHAR(20));
CREATE TABLE JSON (id VARCHAR(20), json VARCHAR(400));

INSERT INTO NAMES VALUES ('1', 'John Smith');
INSERT INTO NAMES VALUES ('2', 'Mary Jones');
INSERT INTO NAMES VALUES ('3', 'Peter Davies');

INSERT INTO JSON VALUES ('1', '{"firstName": "John", "secondName": "Smith"  -- with invalid json format--   ');
INSERT INTO JSON VALUES ('2', '{"firstName": "Mary", "secondName": "Jones"}');
INSERT INTO JSON VALUES ('3', '{"firstName": "Peter", "secondName": "Davies"}');
