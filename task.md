# PayGuard — Personal Expense Tracker Tasks (Phases 23–28)

## Prerequisites (Completed)
- [x] Create Flyway Schema Migration `V3__create_expense_tracker_tables.sql` (6 tables + composite indexes)
- [x] Register OpenAPI description & tags in `OpenApiConfig.java`
- [x] Enforce Security HTTP-Only JWT authentication in `SecurityConfig.java`

## Phase 23 — Expense & Account Core Backend
- [x] Create `ExpenseCategory` & `AccountType` enums
- [x] Create `ExpenseEntry` JPA entity with user ownership & activity invoice mapping
- [x] Create `ExpenseRepository` with search filters, date range, and tax-deductible query methods
- [x] Create `ExpenseRequest`, `ExpenseResponse`, `ExpenseSummaryResponse` DTOs
- [x] Implement `ExpenseService` with manual logging, search filters, and audit publishing
- [x] Implement `ExpenseController` (`POST /api/v1/expenses`, `GET /api/v1/expenses`, `GET /api/v1/expenses/summary`)

## Phase 24 — Activity-Linked Invoice Engine ✅ COMPLETE
- [x] Create `ActivityInvoice` and `InvoiceLineItem` JPA entities
- [x] Create `ActivityInvoiceRepository`
- [x] Create `ActivityInvoiceRequest`, `ActivityInvoiceResponse`, `InvoiceLineItemDto`
- [x] Implement `ActivityInvoiceService` with Net Profit & Profit Margin % calculation, CSV export
- [x] Implement `ActivityInvoiceController` (`POST /api/v1/activity-invoices`, `GET`, `GET /{id}`, `GET /{id}/export/csv`)

## Phase 25 — Budgets, Subscriptions & Net Worth Engine ✅ COMPLETE
- [x] Create `BudgetLimit`, `SubscriptionTracker`, and `SavingsGoal` JPA entities
- [x] Create `BudgetLimitRepository`, `SubscriptionTrackerRepository`, `SavingsGoalRepository`
- [x] Create `BudgetProgressDto`, `SubscriptionDto`, `NetWorthDto` response DTOs
- [x] Implement `BudgetService` (80% WARNING / 100% OVERSPENT alert pills & rollover calculation)
- [x] Implement `SubscriptionService` (renewal countdown lookahead & 1-click cancellation)
- [x] Implement `NetWorthService` (Assets: Wallet + Savings Goals minus Liabilities: Credit expenses)
- [x] Implement `BudgetController` (`POST /api/v1/budgets`, `GET /api/v1/budgets/progress`)
- [x] Implement `SubscriptionController` (`POST`, `GET`, `POST /{id}/cancel`)
- [x] Implement `NetWorthController` (`GET /api/v1/net-worth`)

## Phase 26 — AI Risk Insights, Receipt OCR & SSE Alerts ✅ COMPLETE
- [x] Create `ExpenseAiInsightService` (category dominance, subscription audit, tax deductible insights)
- [x] Create `ReceiptOcrService` (receipt filename-based OCR simulator → merchant, amount, category, tax flag)
- [x] Implement `ExpenseAnalyticsController` (`GET /api/v1/expense-analytics/insights`, `POST /scan-receipt`)
- [x] SSE budget threshold alerts wired via existing `NotificationService.broadcast()`

## Phase 27 — React Frontend Expense Portal ✅ COMPLETE
- [x] Create `expenseApi.ts` — expense log, summary, AI insights, OCR scan endpoints
- [x] Create `activityInvoiceApi.ts` — invoice create, list, detail, CSV export download
- [x] Create `budgetApi.ts` — set category cap, get spending progress bars
- [x] Create `subscriptionApi.ts` — add subscription, list, 1-click cancel
- [x] Create `netWorthApi.ts` — net worth assets/liabilities query
- [x] Build `ExpenseTrackerPage.tsx` with 5 tabbed modules:
  - [x] **Overview & Analytics** — quick log form, OCR receipt scan, expense history + instant search, AI insight banner
  - [x] **⭐ Activity-Linked Invoices** — single-ledger income+expense invoice builder with live Net Profit & Margin % preview, CSV export
  - [x] **Budgets & Spending Caps** — category cap form, animated progress bars with OK / WARNING_80 / OVERSPENT_100 pills
  - [x] **Subscription Hub** — SaaS tracker with renewal countdown calendar & 1-click cancel
  - [x] **Tax & Deductibles** — filtered tax-deductible expense report
- [x] Update `Navbar.tsx` — Expense Tracker purple-highlighted nav link
- [x] Update `App.tsx` — `/expense-tracker` protected route registered
- [x] Verified: `npm run build` → ✅ `built in 637ms`, **0 TypeScript errors**
- [x] Verified: `./mvnw test` → ✅ `21/21 tests pass, BUILD SUCCESS`

## Phase 28 — Automated Tests & E2E Validation
- [ ] Write backend unit tests (`ExpenseServiceTest`, `ActivityInvoiceTest`, `BudgetLimitTest`)
- [ ] Execute `./mvnw test` with new expense tracker tests (target: 30+ tests)
- [ ] Execute automated E2E PowerShell checklist for all 9 Expense Tracker API endpoints
