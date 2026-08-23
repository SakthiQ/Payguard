-- Flyway Schema Migration V3: Personal Expense Tracker, Activity Invoices, Budgets, Subscriptions, and Savings Goals

-- 1. Expenses Table
CREATE TABLE expenses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_type VARCHAR(20) NOT NULL DEFAULT 'CASH', -- WALLET, BANK, CASH, CREDIT
    category VARCHAR(30) NOT NULL,                    -- FOOD, RENT, BILLS, ENTERTAINMENT, TRAVEL, BUSINESS, SUBSCRIPTION
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    merchant_name VARCHAR(100),
    description VARCHAR(255),
    tags VARCHAR(255),                                -- e.g. #vacation2026,#client_alpha
    tax_deductible BOOLEAN NOT NULL DEFAULT FALSE,
    recurring BOOLEAN NOT NULL DEFAULT FALSE,
    activity_invoice_id BIGINT NULL,                  -- Optional link to ActivityInvoice
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_expenses_user_date ON expenses(user_id, created_at);
CREATE INDEX idx_expenses_category ON expenses(user_id, category);
CREATE INDEX idx_expenses_tax ON expenses(user_id, tax_deductible);

-- 2. Activity Invoices Table (Flagship Activity-Linked Invoice Engine)
CREATE TABLE activity_invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_title VARCHAR(150) NOT NULL,            -- e.g. 'NYC Tech Conference 2026', 'Client Web Redesign'
    client_name VARCHAR(100),
    total_income DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    total_expenses DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    net_profit DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    profit_margin_percent DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',      -- DRAFT, FINALIZED, PAID
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_activity_invoices_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_activity_invoices_user_status ON activity_invoices(user_id, status);

-- 3. Invoice Line Items Table (Linking Income & Expense items to single Activity Invoice)
CREATE TABLE invoice_line_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_invoice_id BIGINT NOT NULL,
    type VARCHAR(10) NOT NULL,                       -- INCOME or EXPENSE
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    category VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_line_items_invoice FOREIGN KEY (activity_invoice_id) REFERENCES activity_invoices(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add Foreign Key from expenses to activity_invoices
ALTER TABLE expenses ADD CONSTRAINT fk_expenses_activity_invoice 
    FOREIGN KEY (activity_invoice_id) REFERENCES activity_invoices(id) ON DELETE SET NULL;

-- 4. Budget Limits Table (Category caps with rollover allowances)
CREATE TABLE budget_limits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category VARCHAR(30) NOT NULL,
    monthly_cap DECIMAL(19, 4) NOT NULL,
    spent_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    rollover_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    warning_threshold_percent INT NOT NULL DEFAULT 80,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_budget_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_category UNIQUE (user_id, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Subscriptions Table (SaaS and recurring bill tracker)
CREATE TABLE subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    service_name VARCHAR(100) NOT NULL,               -- e.g. 'Netflix', 'AWS Cloud', 'Gym Membership'
    amount DECIMAL(19, 4) NOT NULL,
    billing_cycle VARCHAR(20) NOT NULL DEFAULT 'MONTHLY', -- MONTHLY, YEARLY
    next_renewal_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',    -- ACTIVE, CANCEL_REQUESTED, CANCELLED
    cancellation_notes VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_subscriptions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_subscriptions_user_renewal ON subscriptions(user_id, next_renewal_date);

-- 6. Savings Goals Table
CREATE TABLE savings_goals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    goal_name VARCHAR(100) NOT NULL,
    target_amount DECIMAL(19, 4) NOT NULL,
    current_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    target_date DATE NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
