DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS customers;

CREATE TABLE customers
(
    id    BIGINT(10) AUTO_INCREMENT NOT NULL PRIMARY KEY,
    name  VARCHAR(255) NOT NULL,
    email VARCHAR(255) NULL
);

CREATE TABLE orders
(
    id          BIGINT(10) AUTO_INCREMENT NOT NULL PRIMARY KEY,
    sku         VARCHAR(255) NOT NULL,
    CUSTOMER_FK BIGINT       NOT NULL REFERENCES customers (id)
);

