
INSERT INTO item (id, title, description, img_path, price) VALUES
                                                               (1, 'Wooden Chair', 'A sturdy handcrafted wooden chair.', 'images/1.png', 49.99),
                                                               (2, 'Modern Desk Lamp', 'Stylish LED desk lamp with touch dimmer.', 'images/2.png', 29.50),
                                                               (3, 'Wireless Mouse', 'Ergonomic wireless mouse with long battery life.', 'images/3.png', 19.95),
                                                               (4, 'Notebook', 'A5 notebook with dotted pages for journaling.', 'images/4.png', 5.99),
                                                               (5, 'Ceramic Mug', 'Classic ceramic coffee mug, 350ml.', 'images/5.png', 7.75);


INSERT INTO users (id, name, email, password) VALUES
    (1, 'Slava', 'slaval@mail.ru', 'password');

INSERT INTO cart (id, user_id) VALUES
    (1, 1);