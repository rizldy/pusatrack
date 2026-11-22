-- PUSATRACK Database Schema (MySQL 8.4.3 / InnoDB)
-- Menampung seluruh 13 tabel sesuai ERD proyek[cite: 7, 10]

CREATE DATABASE IF NOT EXISTS pusatrack_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE pusatrack_db;

-- =========================================================
-- 1. MASTER & AUTENTIKASI
-- =========================================================

CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS expense_categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ingredients (
    ingredient_id INT AUTO_INCREMENT PRIMARY KEY,
    ingredient_name VARCHAR(100) NOT NULL,
    purchase_unit VARCHAR(30) NOT NULL,
    package_size DECIMAL(12,2) NOT NULL DEFAULT 1.00,
    usage_unit VARCHAR(30) NOT NULL,
    current_stock DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    minimum_stock DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    unit_cost DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    selling_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS product_recipes (
    recipe_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    ingredient_id INT NOT NULL,
    quantity_required DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_recipes_product
        FOREIGN KEY (product_id) REFERENCES products(product_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_recipes_ingredient
        FOREIGN KEY (ingredient_id) REFERENCES ingredients(ingredient_id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

-- =========================================================
-- 2. LOG AKTIVITAS & STOK OPNAME
-- =========================================================

CREATE TABLE IF NOT EXISTS activity_history (
    activity_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    activity_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_counts (
    count_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    count_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    CONSTRAINT fk_stock_counts_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_movements (
    movement_id INT AUTO_INCREMENT PRIMARY KEY,
    ingredient_id INT NOT NULL,
    user_id INT NOT NULL,
    activity_id INT NULL,
    movement_type VARCHAR(30) NOT NULL,
    quantity DECIMAL(12,2) NOT NULL,
    stock_before DECIMAL(12,2) NOT NULL,
    stock_after DECIMAL(12,2) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_id INT NULL,
    description TEXT NULL,
    reason TEXT NULL,
    movement_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movements_ingredient
        FOREIGN KEY (ingredient_id) REFERENCES ingredients(ingredient_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_movements_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_movements_activity
        FOREIGN KEY (activity_id) REFERENCES activity_history(activity_id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

-- =========================================================
-- 3. PEMBELIAN & LEAD TIME PENGIRIMAN
-- =========================================================

CREATE TABLE IF NOT EXISTS purchase_orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    supplier_name VARCHAR(100) NOT NULL,
    order_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    received_at DATETIME NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ORDERED',
    CONSTRAINT fk_po_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS purchase_order_details (
    order_detail_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    ingredient_id INT NOT NULL,
    quantity DECIMAL(12,2) NOT NULL,
    package_size_snapshot DECIMAL(12,2) NOT NULL,
    package_price_snapshot DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_pod_order
        FOREIGN KEY (order_id) REFERENCES purchase_orders(order_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_pod_ingredient
        FOREIGN KEY (ingredient_id) REFERENCES ingredients(ingredient_id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

-- =========================================================
-- 4. TRANSAKSI PENJUALAN & PENGELUARAN
-- =========================================================

CREATE TABLE IF NOT EXISTS sales (
    sale_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    sale_number VARCHAR(50) NOT NULL UNIQUE,
    sale_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_method VARCHAR(20) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    confirmed_at DATETIME NULL,
    CONSTRAINT fk_sales_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sale_details (
    sale_detail_id INT AUTO_INCREMENT PRIMARY KEY,
    sale_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    selling_price_snapshot DECIMAL(12,2) NOT NULL,
    hpp_snapshot DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_sd_sale
        FOREIGN KEY (sale_id) REFERENCES sales(sale_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_sd_product
        FOREIGN KEY (product_id) REFERENCES products(product_id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS expenses (
    expense_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    category_id INT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    description TEXT NULL,
    expense_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    CONSTRAINT fk_expenses_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_expenses_category
        FOREIGN KEY (category_id) REFERENCES expense_categories(category_id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;
