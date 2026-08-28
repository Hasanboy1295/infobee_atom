INSERT INTO roles (name) VALUES ('ADMIN');
INSERT INTO roles (name) VALUES ('USER');

INSERT INTO users (username, password, full_name, role)
VALUES ('admin', '$2a$10$y6xte60O.elkZeufk0PebufGxLGA/QnMaZuICWxy5HBPER6A.xBhi', 'System Administrator', 'ADMIN');

INSERT INTO users (username, password, full_name, role)
VALUES ('infobee', '$2b$10$mxMT.6ExddAtvsiuCcxn0OrGf/.Om0NnrI3ghBLzWUgd96/DlhGoG', 'Infobee User', 'USER');
