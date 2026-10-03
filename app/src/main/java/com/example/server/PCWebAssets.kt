package com.example.server

object PCWebAssets {

    fun getIndexHtml(appName: String, currencySymbol: String): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>$appName — PC Manager</title>
  <style>
    :root {
      --bg-main: #121417;
      --bg-surface: #1B1E23;
      --bg-surface-elevated: #242930;
      --bg-input: #29303A;
      --border-color: #313843;
      --border-subtle: #2A303A;
      --text-main: #F3F4F6;
      --text-muted: #9CA3AF;
      --text-dim: #6B7280;
      --primary: #2563EB;
      --primary-hover: #1D4ED8;
      --income: #22C55E;
      --expense: #EF4444;
      --transfer: #3B82F6;
      --font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: var(--font-family);
      background-color: var(--bg-main);
      color: var(--text-main);
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }
    header {
      background-color: var(--bg-surface);
      border-bottom: 1px solid var(--border-color);
      padding: 12px 24px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      position: sticky;
      top: 0;
      z-index: 100;
    }
    .header-brand {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .brand-icon {
      width: 36px;
      height: 36px;
      background: linear-gradient(135deg, #10B981, #059669);
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 800;
      color: white;
      font-size: 15px;
      letter-spacing: -0.5px;
      box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
    }
    .brand-title {
      font-size: 18px;
      font-weight: 700;
      letter-spacing: -0.02em;
    }
    .brand-tag {
      background: rgba(37, 99, 235, 0.2);
      color: #60A5FA;
      padding: 2px 8px;
      border-radius: 6px;
      font-size: 11px;
      font-weight: 600;
      margin-left: 6px;
    }
    .header-status {
      display: flex;
      align-items: center;
      gap: 8px;
      background: rgba(34, 197, 94, 0.12);
      border: 1px solid rgba(34, 197, 94, 0.3);
      padding: 5px 12px;
      border-radius: 20px;
      font-size: 12px;
      color: #4ADE80;
      font-weight: 500;
    }
    .pulse-dot {
      width: 8px;
      height: 8px;
      background-color: #22C55E;
      border-radius: 50%;
      box-shadow: 0 0 8px #22C55E;
    }
    .header-actions {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    button {
      font-family: inherit;
      cursor: pointer;
      border: none;
      outline: none;
      border-radius: 8px;
      font-weight: 600;
      transition: all 0.15s ease;
    }
    .btn {
      padding: 8px 16px;
      font-size: 13px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
    }
    .btn-primary {
      background-color: #22C55E;
      color: #FFFFFF;
    }
    .btn-primary:hover:not(:disabled) { background-color: #16A34A; }
    .btn-secondary {
      background-color: var(--bg-surface-elevated);
      color: var(--text-main);
      border: 1px solid var(--border-color);
    }
    .btn-secondary:hover:not(:disabled) { background-color: var(--bg-input); }
    .btn-danger {
      background-color: rgba(239, 68, 68, 0.15);
      color: #F87171;
      border: 1px solid rgba(239, 68, 68, 0.3);
    }
    .btn-danger:hover:not(:disabled) { background-color: rgba(239, 68, 68, 0.25); }
    .btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .main-container {
      max-width: 1380px;
      margin: 0 auto;
      padding: 24px;
      width: 100%;
      flex: 1;
    }

    .summary-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
    }
    .card {
      background-color: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: 14px;
      padding: 18px;
    }
    .card-label {
      font-size: 12px;
      color: var(--text-muted);
      font-weight: 600;
      letter-spacing: 0.05em;
      margin-bottom: 6px;
    }
    .card-value {
      font-size: 24px;
      font-weight: 700;
      letter-spacing: -0.02em;
    }
    .val-income { color: var(--income); }
    .val-expense { color: var(--expense); }
    .val-net { color: #60A5FA; }

    .nav-tabs {
      display: flex;
      gap: 4px;
      background-color: var(--bg-surface);
      padding: 4px;
      border-radius: 12px;
      border: 1px solid var(--border-color);
      width: fit-content;
      margin-bottom: 20px;
    }
    .tab-btn {
      padding: 8px 20px;
      font-size: 13px;
      color: var(--text-muted);
      background: transparent;
      border-radius: 8px;
    }
    .tab-btn.active {
      background-color: var(--bg-surface-elevated);
      color: var(--text-main);
      font-weight: 700;
    }

    .controls-bar {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 18px;
    }
    .month-picker {
      display: flex;
      align-items: center;
      gap: 10px;
      background-color: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: 10px;
      padding: 6px 14px;
    }
    .month-label {
      font-weight: 700;
      font-size: 16px;
      min-width: 160px;
      text-align: center;
    }
    .month-nav-btn {
      background: none;
      color: var(--text-main);
      font-size: 16px;
      padding: 4px 8px;
    }
    .month-nav-btn:hover { color: var(--primary); }

    .view-mode-toggle {
      display: flex;
      background: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: 10px;
      padding: 3px;
    }
    .view-mode-btn {
      padding: 6px 14px;
      font-size: 12px;
      background: transparent;
      color: var(--text-muted);
      border-radius: 7px;
    }
    .view-mode-btn.active {
      background: var(--bg-surface-elevated);
      color: var(--text-main);
      font-weight: 700;
    }

    .filter-group {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 10px;
    }
    .select-input, .text-input {
      background-color: var(--bg-input);
      border: 1px solid var(--border-color);
      color: var(--text-main);
      padding: 8px 12px;
      border-radius: 8px;
      font-size: 13px;
      outline: none;
    }
    .select-input:focus, .text-input:focus {
      border-color: var(--primary);
    }

    /* App Format Daily Grouped View */
    .daily-list-container {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    .day-card {
      background-color: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: 14px;
      overflow: hidden;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    }
    .day-header {
      background-color: var(--bg-surface-elevated);
      border-bottom: 1px solid var(--border-subtle);
      padding: 10px 18px;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    .day-header-left {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .day-num {
      font-size: 18px;
      font-weight: 800;
      color: var(--text-main);
      min-width: 24px;
    }
    .day-chip {
      padding: 2px 7px;
      border-radius: 5px;
      font-size: 11px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.03em;
    }
    .day-chip-weekday {
      background: rgba(255, 255, 255, 0.08);
      color: #D1D5DB;
    }
    .day-chip-sat {
      background: rgba(59, 130, 246, 0.2);
      color: #60A5FA;
    }
    .day-chip-sun {
      background: rgba(239, 68, 68, 0.2);
      color: #F87171;
    }
    .day-my {
      font-size: 12px;
      color: var(--text-muted);
      font-weight: 500;
    }
    .day-header-right {
      display: flex;
      align-items: center;
      gap: 16px;
      font-size: 13px;
      font-weight: 600;
    }
    .day-income-total { color: var(--income); }
    .day-expense-total { color: var(--expense); }

    /* App Format Transaction Row */
    .app-tx-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 14px 18px;
      border-bottom: 1px solid var(--border-subtle);
      cursor: pointer;
      transition: background 0.15s ease;
    }
    .app-tx-row:last-child {
      border-bottom: none;
    }
    .app-tx-row:hover {
      background-color: rgba(255, 255, 255, 0.03);
    }
    .app-tx-left {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 130px;
    }
    .tx-cat-chip {
      font-size: 13px;
      font-weight: 600;
      color: var(--text-main);
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .tx-acc-subtitle {
      font-size: 12px;
      color: var(--text-muted);
    }
    .app-tx-center {
      flex: 1;
      padding: 0 20px;
      display: flex;
      flex-direction: column;
      gap: 3px;
    }
    .tx-primary-text {
      font-size: 14px;
      font-weight: 600;
      color: #FFFFFF;
    }
    .tx-secondary-text {
      font-size: 12px;
      color: var(--text-muted);
    }
    .app-tx-right {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .tx-amount-col {
      text-align: right;
      min-width: 120px;
    }
    .tx-amount-val {
      font-size: 15px;
      font-weight: 700;
      letter-spacing: -0.01em;
    }
    .tx-time-val {
      font-size: 11px;
      color: var(--text-muted);
      margin-top: 2px;
    }
    .tx-actions-col {
      display: flex;
      gap: 6px;
    }
    .action-icon-btn {
      background: var(--bg-surface-elevated);
      border: 1px solid var(--border-color);
      color: var(--text-muted);
      width: 28px;
      height: 28px;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
    }
    .action-icon-btn:hover {
      background: var(--bg-input);
      color: var(--text-main);
    }
    .action-icon-btn.btn-del:hover {
      background: rgba(239, 68, 68, 0.2);
      color: #F87171;
      border-color: rgba(239, 68, 68, 0.4);
    }

    /* Table View */
    .table-container {
      background-color: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: 14px;
      overflow: hidden;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;
    }
    th {
      background-color: var(--bg-surface-elevated);
      color: var(--text-muted);
      font-size: 12px;
      font-weight: 600;
      padding: 14px 16px;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }
    td {
      padding: 14px 16px;
      border-bottom: 1px solid var(--border-color);
      font-size: 13px;
    }
    tr:last-child td { border-bottom: none; }
    tr:hover td { background-color: rgba(255, 255, 255, 0.02); }

    .badge {
      display: inline-block;
      padding: 3px 8px;
      border-radius: 6px;
      font-size: 11px;
      font-weight: 600;
    }
    .badge-expense { background: rgba(239, 68, 68, 0.15); color: #F87171; }
    .badge-income { background: rgba(34, 197, 94, 0.15); color: #4ADE80; }
    .badge-transfer { background: rgba(59, 130, 246, 0.15); color: #60A5FA; }

    .amount-expense { color: var(--expense); }
    .amount-income { color: var(--income); }
    .amount-transfer { color: var(--transfer); }

    /* Modal */
    .modal-overlay {
      position: fixed;
      inset: 0;
      background-color: rgba(0, 0, 0, 0.7);
      backdrop-filter: blur(4px);
      display: none;
      align-items: center;
      justify-content: center;
      z-index: 200;
      padding: 20px;
    }
    .modal-overlay.open { display: flex; }
    .modal {
      background-color: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: 16px;
      width: 100%;
      max-width: 520px;
      overflow: hidden;
      box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5);
    }
    .modal-header {
      padding: 18px 22px;
      border-bottom: 1px solid var(--border-color);
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .modal-title { font-size: 17px; font-weight: 700; }
    .modal-close {
      background: none;
      color: var(--text-muted);
      font-size: 20px;
    }
    .modal-body {
      padding: 20px 22px;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    .modal-footer {
      padding: 16px 22px;
      background-color: var(--bg-surface-elevated);
      border-top: 1px solid var(--border-color);
      display: flex;
      justify-content: flex-end;
      gap: 10px;
    }
    .form-group {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .form-label {
      font-size: 12px;
      font-weight: 600;
      color: var(--text-muted);
      text-transform: uppercase;
    }
    .type-selector {
      display: grid;
      grid-template-columns: 1fr 1fr 1fr;
      gap: 8px;
    }
    .type-btn {
      padding: 10px;
      border-radius: 8px;
      background-color: var(--bg-input);
      border: 1px solid var(--border-color);
      color: var(--text-muted);
      font-weight: 600;
      font-size: 13px;
    }
    .type-btn.selected-expense { background-color: var(--expense); color: white; border-color: var(--expense); }
    .type-btn.selected-income { background-color: var(--income); color: white; border-color: var(--income); }
    .type-btn.selected-transfer { background-color: var(--transfer); color: white; border-color: var(--transfer); }

    /* Accounts grid */
    .accounts-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 16px;
    }
    .account-card {
      background-color: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: 12px;
      padding: 16px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .account-name { font-weight: 700; font-size: 15px; }
    .account-type { font-size: 12px; color: var(--text-muted); }
    .account-balance { font-size: 17px; font-weight: 700; }

    /* Passcode Lock Modal */
    .passcode-box {
      max-width: 360px;
      text-align: center;
      padding: 32px 24px;
    }
    .lock-icon {
      font-size: 44px;
      margin-bottom: 12px;
    }
    .passcode-input {
      font-size: 28px;
      letter-spacing: 12px;
      text-align: center;
      padding: 10px;
      width: 180px;
      margin: 18px auto;
      background: var(--bg-input);
      border: 2px solid var(--border-color);
      color: var(--text-main);
      border-radius: 10px;
    }
    .passcode-error {
      color: #F87171;
      font-size: 13px;
      margin-top: 8px;
      display: none;
    }

    .empty-state {
      padding: 60px 20px;
      text-align: center;
      color: var(--text-muted);
      background-color: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: 14px;
    }
    .empty-state h3 { color: var(--text-main); margin-bottom: 6px; font-size: 17px; }
    .empty-state p { font-size: 13px; margin-bottom: 16px; }

    /* Spinner */
    .spinner {
      display: inline-block;
      width: 14px;
      height: 14px;
      border: 2px solid rgba(255, 255, 255, 0.3);
      border-top-color: #FFFFFF;
      border-radius: 50%;
      animation: spin 0.6s linear infinite;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    /* Toast Notification */
    .toast {
      position: fixed;
      bottom: 24px;
      right: 24px;
      background: #10B981;
      color: white;
      padding: 12px 20px;
      border-radius: 10px;
      font-size: 13px;
      font-weight: 600;
      box-shadow: 0 4px 14px rgba(0, 0, 0, 0.4);
      display: flex;
      align-items: center;
      gap: 8px;
      opacity: 0;
      transform: translateY(20px);
      transition: all 0.25s ease;
      z-index: 300;
      pointer-events: none;
    }
    .toast.show {
      opacity: 1;
      transform: translateY(0);
      pointer-events: auto;
    }
  </style>
</head>
<body>

  <!-- Top Bar -->
  <header>
    <div class="header-brand">
      <div class="brand-icon">CT</div>
      <div>
        <span class="brand-title">$appName</span>
        <span class="brand-tag">PC MANAGER</span>
      </div>
    </div>

    <div class="header-status">
      <span class="pulse-dot"></span>
      <span>Connected to Mobile Device</span>
    </div>

    <div class="header-actions">
      <button class="btn btn-secondary" onclick="loadAllData()">↻ Refresh</button>
      <button class="btn btn-secondary" onclick="downloadCsv()">📥 Export CSV</button>
      <button class="btn btn-primary" onclick="openAddTransactionModal()">+ New Transaction</button>
    </div>
  </header>

  <!-- Passcode Unlock Screen Overlay -->
  <div id="lockModal" class="modal-overlay">
    <div class="modal passcode-box">
      <div class="lock-icon">🔒</div>
      <h2 style="font-size: 20px; margin-bottom: 6px;">Passcode Required</h2>
      <p style="color: var(--text-muted); font-size: 13px;">Enter the 4-digit passcode displayed on your Cash Tracker mobile screen.</p>
      <input type="password" id="passcodeInput" class="passcode-input" maxlength="8" placeholder="••••" autofocus onkeydown="if(event.key==='Enter') submitPasscode()">
      <div id="passcodeError" class="passcode-error">Incorrect Passcode. Try again.</div>
      <button class="btn btn-primary" style="width: 100%; margin-top: 10px; justify-content: center; padding: 12px;" onclick="submitPasscode()">Unlock</button>
    </div>
  </div>

  <!-- Toast Notification -->
  <div id="toast" class="toast">
    <span>✓</span>
    <span id="toastMsg">Transaction saved successfully!</span>
  </div>

  <!-- Main Content -->
  <div class="main-container">
    <!-- Quick Metrics Strip -->
    <div class="summary-grid">
      <div class="card">
        <div class="card-label">MONTH'S INCOME</div>
        <div class="card-value val-income" id="statIncome">$currencySymbol 0.00</div>
      </div>
      <div class="card">
        <div class="card-label">MONTH'S EXPENSES</div>
        <div class="card-value val-expense" id="statExpense">$currencySymbol 0.00</div>
      </div>
      <div class="card">
        <div class="card-label">NET BALANCE</div>
        <div class="card-value val-net" id="statBalance">$currencySymbol 0.00</div>
      </div>
      <div class="card">
        <div class="card-label">TOTAL NET WORTH</div>
        <div class="card-value" id="statNetWorth">$currencySymbol 0.00</div>
      </div>
    </div>

    <!-- Navigation Tabs -->
    <div class="nav-tabs">
      <button class="tab-btn active" id="tabBtnTransactions" onclick="switchTab('transactions')">Transactions</button>
      <button class="tab-btn" id="tabBtnAccounts" onclick="switchTab('accounts')">Accounts & Balances</button>
      <button class="tab-btn" id="tabBtnBackup" onclick="switchTab('backup')">Backup & Export</button>
    </div>

    <!-- Tab 1: Transactions -->
    <div id="tabTransactions">
      <div class="controls-bar">
        <div class="month-picker">
          <button class="month-nav-btn" onclick="prevMonth()">◀</button>
          <span class="month-label" id="currentMonthLabel">September 2026</span>
          <button class="month-nav-btn" onclick="nextMonth()">▶</button>
          <button class="btn btn-secondary" style="padding: 4px 10px; font-size: 11px;" onclick="currentMonthToday()">Current</button>
        </div>

        <div class="view-mode-toggle">
          <button class="view-mode-btn active" id="btnViewApp" onclick="setViewMode('app')">📱 App View</button>
          <button class="view-mode-btn" id="btnViewTable" onclick="setViewMode('table')">📄 Table View</button>
        </div>

        <div class="filter-group">
          <select id="filterType" class="select-input" onchange="renderTransactions()">
            <option value="ALL">All Types</option>
            <option value="EXPENSE">Expense</option>
            <option value="INCOME">Income</option>
            <option value="TRANSFER">Transfer</option>
          </select>

          <select id="filterAccount" class="select-input" onchange="renderTransactions()">
            <option value="ALL">All Accounts</option>
          </select>

          <select id="filterCategory" class="select-input" onchange="renderTransactions()">
            <option value="ALL">All Categories</option>
          </select>

          <input type="text" id="searchQuery" class="text-input" placeholder="Search payee, note..." oninput="renderTransactions()" style="width: 180px;">
        </div>
      </div>

      <!-- App View: Daily Grouped List (Matching Cash Tracker Android App) -->
      <div id="appViewContainer" class="daily-list-container">
        <!-- Dynamically rendered day cards -->
      </div>

      <!-- Table View: Flat Ledger Table -->
      <div id="tableViewContainer" class="table-container" style="display: none;">
        <table>
          <thead>
            <tr>
              <th>Date & Time</th>
              <th>Type</th>
              <th>Account</th>
              <th>Category</th>
              <th>Payee / Note</th>
              <th style="text-align: right;">Amount</th>
              <th style="text-align: center; width: 100px;">Actions</th>
            </tr>
          </thead>
          <tbody id="transactionsTbody">
            <!-- Dynamically populated -->
          </tbody>
        </table>
      </div>

      <!-- Empty State -->
      <div id="emptyTransactionsState" class="empty-state" style="display: none;">
        <h3>No transactions found</h3>
        <p>There are no transactions recorded for this period with the current filters.</p>
        <button class="btn btn-primary" onclick="openAddTransactionModal()">+ Add New Transaction</button>
      </div>
    </div>

    <!-- Tab 2: Accounts -->
    <div id="tabAccounts" style="display: none;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px;">
        <h2 style="font-size: 18px;">All Accounts & Assets</h2>
        <button class="btn btn-primary" onclick="openAddAccountModal()">+ New Account</button>
      </div>
      <div class="accounts-grid" id="accountsGrid">
        <!-- Dynamically populated -->
      </div>
    </div>

    <!-- Tab 3: Backup & Export -->
    <div id="tabBackup" style="display: none;">
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 20px;">
        <div class="card">
          <h3 style="font-size: 16px; margin-bottom: 8px;">Export Realbyte CSV Spreadsheet</h3>
          <p style="color: var(--text-muted); font-size: 13px; margin-bottom: 16px;">
            Export transactions into a standard .CSV spreadsheet compatible with Microsoft Excel, Google Sheets, and Cash Tracker mobile app.
          </p>
          <button class="btn btn-primary" onclick="downloadCsv()">📥 Download CSV Export</button>
        </div>

        <div class="card">
          <h3 style="font-size: 16px; margin-bottom: 8px;">Full JSON Ledger Backup</h3>
          <p style="color: var(--text-muted); font-size: 13px; margin-bottom: 16px;">
            Download a full complete backup file (.json) containing all accounts, categories, transactions, memos, and budgets.
          </p>
          <button class="btn btn-secondary" onclick="downloadJsonBackup()">💾 Download JSON Backup</button>
        </div>
      </div>
    </div>
  </div>

  <!-- Add / Edit Transaction Modal -->
  <div id="transactionModal" class="modal-overlay">
    <div class="modal">
      <div class="modal-header">
        <div class="modal-title" id="txModalTitle">New Transaction</div>
        <button class="modal-close" onclick="closeTransactionModal()">✕</button>
      </div>
      <div class="modal-body">
        <input type="hidden" id="txEditId" value="0">
        
        <div class="form-group">
          <label class="form-label">Transaction Type</label>
          <div class="type-selector">
            <button type="button" class="type-btn selected-expense" id="btnTypeExpense" onclick="setTxType('EXPENSE')">Expense</button>
            <button type="button" class="type-btn" id="btnTypeIncome" onclick="setTxType('INCOME')">Income</button>
            <button type="button" class="type-btn" id="btnTypeTransfer" onclick="setTxType('TRANSFER')">Transfer</button>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">Amount ($currencySymbol)</label>
          <input type="number" step="0.01" id="txAmount" class="text-input" placeholder="0.00" style="font-size: 18px; font-weight: 700;">
        </div>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
          <div class="form-group">
            <label class="form-label" id="txAccountLabel">Account</label>
            <select id="txAccount" class="select-input"></select>
          </div>

          <div class="form-group" id="txToAccountGroup" style="display: none;">
            <label class="form-label">To Account</label>
            <select id="txToAccount" class="select-input"></select>
          </div>

          <div class="form-group" id="txCategoryGroup">
            <label class="form-label">Category</label>
            <select id="txCategory" class="select-input"></select>
          </div>
        </div>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
          <div class="form-group">
            <label class="form-label">Date</label>
            <input type="date" id="txDate" class="text-input">
          </div>
          <div class="form-group">
            <label class="form-label">Time</label>
            <input type="time" id="txTime" class="text-input">
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">Payee / Merchant</label>
          <input type="text" id="txPayee" class="text-input" placeholder="e.g. Starbucks, Amazon, Salary">
        </div>

        <div class="form-group">
          <label class="form-label">Note / Description</label>
          <input type="text" id="txNote" class="text-input" placeholder="Optional notes...">
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-secondary" onclick="closeTransactionModal()">Cancel</button>
        <button class="btn btn-primary" id="btnSaveTx" onclick="saveTransaction()">Save Transaction</button>
      </div>
    </div>
  </div>

  <!-- Add Account Modal -->
  <div id="accountModal" class="modal-overlay">
    <div class="modal">
      <div class="modal-header">
        <div class="modal-title">New Account</div>
        <button class="modal-close" onclick="closeAccountModal()">✕</button>
      </div>
      <div class="modal-body">
        <div class="form-group">
          <label class="form-label">Account Name</label>
          <input type="text" id="accName" class="text-input" placeholder="e.g. Chase Checking, Cash Wallet">
        </div>
        <div class="form-group">
          <label class="form-label">Account Type</label>
          <select id="accType" class="select-input">
            <option value="BANK">Bank Account</option>
            <option value="CASH">Cash</option>
            <option value="CREDIT_CARD">Credit Card</option>
            <option value="SAVINGS">Savings</option>
            <option value="INVESTMENT">Investment</option>
            <option value="LOAN">Loan</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Initial Balance ($currencySymbol)</label>
          <input type="number" step="0.01" id="accInitialBalance" class="text-input" placeholder="0.00">
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-secondary" onclick="closeAccountModal()">Cancel</button>
        <button class="btn btn-primary" id="btnSaveAcc" onclick="saveAccount()">Create Account</button>
      </div>
    </div>
  </div>

  <script>
    let appState = {
      accounts: [],
      categories: [],
      transactions: [],
      currencySymbol: '$currencySymbol',
      currentYear: new Date().getFullYear(),
      currentMonth: new Date().getMonth(),
      selectedType: 'EXPENSE',
      viewMode: 'app', // 'app' (Daily Grouped) or 'table'
      passcodeToken: localStorage.getItem('mm_pc_token') || ''
    };

    let isSavingTx = false;
    let isSavingAcc = false;

    const monthNames = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
    const dayNames = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

    function init() {
      updateMonthLabel();
      checkAuthAndLoad();
    }

    function showToast(msg) {
      const toast = document.getElementById('toast');
      const toastMsg = document.getElementById('toastMsg');
      toastMsg.textContent = msg;
      toast.classList.add('show');
      setTimeout(() => {
        toast.classList.remove('show');
      }, 3000);
    }

    async function checkAuthAndLoad() {
      try {
        const res = await fetch('/api/status');
        const data = await res.json();
        if (data.passcodeRequired && !data.authenticated && !appState.passcodeToken) {
          document.getElementById('lockModal').classList.add('open');
          document.getElementById('passcodeInput').focus();
        } else {
          loadAllData();
        }
      } catch (e) {
        console.error("Connection error:", e);
      }
    }

    async function submitPasscode() {
      const code = document.getElementById('passcodeInput').value.trim();
      if (!code) return;
      try {
        const res = await fetch('/api/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ passcode: code })
        });
        const data = await res.json();
        if (data.success) {
          appState.passcodeToken = data.token || code;
          localStorage.setItem('mm_pc_token', appState.passcodeToken);
          document.getElementById('lockModal').classList.remove('open');
          document.getElementById('passcodeError').style.display = 'none';
          loadAllData();
        } else {
          document.getElementById('passcodeError').style.display = 'block';
        }
      } catch (e) {
        document.getElementById('passcodeError').style.display = 'block';
      }
    }

    async function apiFetch(url, options = {}) {
      options.headers = options.headers || {};
      options.credentials = 'include';
      if (appState.passcodeToken) {
        options.headers['X-Passcode'] = appState.passcodeToken;
      }
      const res = await fetch(url, options);
      if (res.status === 401) {
        document.getElementById('lockModal').classList.add('open');
        throw new Error("Unauthorized");
      }
      return res;
    }

    async function loadAllData() {
      try {
        const res = await apiFetch('/api/data');
        const data = await res.json();
        appState.accounts = data.accounts || [];
        appState.categories = data.categories || [];
        appState.transactions = data.transactions || [];
        if (data.currencySymbol) appState.currencySymbol = data.currencySymbol;

        populateFilterDropdowns();
        renderStats();
        renderTransactions();
        renderAccounts();
      } catch (e) {
        console.error("Failed to load data:", e);
      }
    }

    function setViewMode(mode) {
      appState.viewMode = mode;
      document.getElementById('btnViewApp').className = 'view-mode-btn ' + (mode === 'app' ? 'active' : '');
      document.getElementById('btnViewTable').className = 'view-mode-btn ' + (mode === 'table' ? 'active' : '');
      document.getElementById('appViewContainer').style.display = mode === 'app' ? 'flex' : 'none';
      document.getElementById('tableViewContainer').style.display = mode === 'table' ? 'block' : 'none';
      renderTransactions();
    }

    function updateMonthLabel() {
      document.getElementById('currentMonthLabel').textContent = monthNames[appState.currentMonth] + ' ' + appState.currentYear;
    }

    function prevMonth() {
      appState.currentMonth--;
      if (appState.currentMonth < 0) {
        appState.currentMonth = 11;
        appState.currentYear--;
      }
      updateMonthLabel();
      renderStats();
      renderTransactions();
    }

    function nextMonth() {
      appState.currentMonth++;
      if (appState.currentMonth > 11) {
        appState.currentMonth = 0;
        appState.currentYear++;
      }
      updateMonthLabel();
      renderStats();
      renderTransactions();
    }

    function currentMonthToday() {
      const now = new Date();
      appState.currentYear = now.getFullYear();
      appState.currentMonth = now.getMonth();
      updateMonthLabel();
      renderStats();
      renderTransactions();
    }

    function switchTab(tabId) {
      document.getElementById('tabTransactions').style.display = tabId === 'transactions' ? 'block' : 'none';
      document.getElementById('tabAccounts').style.display = tabId === 'accounts' ? 'block' : 'none';
      document.getElementById('tabBackup').style.display = tabId === 'backup' ? 'block' : 'none';

      document.getElementById('tabBtnTransactions').className = 'tab-btn ' + (tabId === 'transactions' ? 'active' : '');
      document.getElementById('tabBtnAccounts').className = 'tab-btn ' + (tabId === 'accounts' ? 'active' : '');
      document.getElementById('tabBtnBackup').className = 'tab-btn ' + (tabId === 'backup' ? 'active' : '');
    }

    function formatAmount(minorUnits) {
      const abs = Math.abs(minorUnits);
      const main = Math.floor(abs / 100);
      const cents = abs % 100;
      const formattedMain = main.toLocaleString();
      const centsStr = cents < 10 ? '0' + cents : cents;
      const sign = minorUnits < 0 ? '-' : '';
      return sign + appState.currencySymbol + ' ' + formattedMain + '.' + centsStr;
    }

    function renderStats() {
      let monthIncome = 0;
      let monthExpense = 0;
      let totalNetWorth = 0;

      appState.accounts.forEach(a => {
        totalNetWorth += (a.balance || 0);
      });

      appState.transactions.forEach(t => {
        const d = new Date(t.dateMillis);
        if (d.getFullYear() === appState.currentYear && d.getMonth() === appState.currentMonth) {
          if (t.type === 'INCOME') monthIncome += t.amount;
          else if (t.type === 'EXPENSE') monthExpense += t.amount;
        }
      });

      document.getElementById('statIncome').textContent = formatAmount(monthIncome);
      document.getElementById('statExpense').textContent = formatAmount(monthExpense);
      document.getElementById('statBalance').textContent = formatAmount(monthIncome - monthExpense);
      document.getElementById('statNetWorth').textContent = formatAmount(totalNetWorth);
    }

    function populateFilterDropdowns() {
      const accSelect = document.getElementById('filterAccount');
      const curAccVal = accSelect.value;
      accSelect.innerHTML = '<option value="ALL">All Accounts</option>' +
        appState.accounts.map(a => `<option value="${'$'}{a.id}">${'$'}{escapeHtml(a.name)}</option>`).join('');
      accSelect.value = curAccVal || 'ALL';

      const catSelect = document.getElementById('filterCategory');
      const curCatVal = catSelect.value;
      catSelect.innerHTML = '<option value="ALL">All Categories</option>' +
        appState.categories.map(c => `<option value="${'$'}{c.id}">${'$'}{escapeHtml(c.name)}</option>`).join('');
      catSelect.value = curCatVal || 'ALL';
    }

    function getFilteredTransactions() {
      const typeFilter = document.getElementById('filterType').value;
      const accFilter = document.getElementById('filterAccount').value;
      const catFilter = document.getElementById('filterCategory').value;
      const search = document.getElementById('searchQuery').value.toLowerCase().trim();

      return appState.transactions.filter(t => {
        const d = new Date(t.dateMillis);
        if (d.getFullYear() !== appState.currentYear || d.getMonth() !== appState.currentMonth) return false;
        if (typeFilter !== 'ALL' && t.type !== typeFilter) return false;
        if (accFilter !== 'ALL' && String(t.accountId) !== accFilter && String(t.toAccountId) !== accFilter) return false;
        if (catFilter !== 'ALL' && String(t.categoryId) !== catFilter && String(t.subcategoryId) !== catFilter) return false;
        if (search) {
          const matchNote = (t.note || '').toLowerCase().includes(search);
          const matchPayee = (t.payee || '').toLowerCase().includes(search);
          const matchAcc = (t.accountName || '').toLowerCase().includes(search);
          const matchCat = (t.categoryName || '').toLowerCase().includes(search);
          if (!matchNote && !matchPayee && !matchAcc && !matchCat) return false;
        }
        return true;
      }).sort((a, b) => b.dateMillis - a.dateMillis);
    }

    function renderTransactions() {
      const filtered = getFilteredTransactions();
      const emptyState = document.getElementById('emptyTransactionsState');
      const appContainer = document.getElementById('appViewContainer');
      const tableContainer = document.getElementById('tableViewContainer');

      if (filtered.length === 0) {
        appContainer.innerHTML = '';
        document.getElementById('transactionsTbody').innerHTML = '';
        appContainer.style.display = 'none';
        tableContainer.style.display = 'none';
        emptyState.style.display = 'block';
        return;
      }

      emptyState.style.display = 'none';

      if (appState.viewMode === 'app') {
        appContainer.style.display = 'flex';
        tableContainer.style.display = 'none';
        renderAppView(filtered);
      } else {
        appContainer.style.display = 'none';
        tableContainer.style.display = 'block';
        renderTableView(filtered);
      }
    }

    // App Format: Group by Day with Day Headers matching the Android App
    function renderAppView(transactions) {
      const container = document.getElementById('appViewContainer');
      
      // Group by calendar day YYYY-MM-DD
      const groups = {};
      transactions.forEach(t => {
        const d = new Date(t.dateMillis);
        const key = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
        if (!groups[key]) {
          groups[key] = {
            dateKey: key,
            dateMillis: t.dateMillis,
            totalIncome: 0,
            totalExpense: 0,
            transactions: []
          };
        }
        groups[key].transactions.push(t);
        if (t.type === 'INCOME') groups[key].totalIncome += t.amount;
        if (t.type === 'EXPENSE') groups[key].totalExpense += t.amount;
      });

      const sortedDateKeys = Object.keys(groups).sort((a, b) => b.localeCompare(a));

      container.innerHTML = sortedDateKeys.map(key => {
        const grp = groups[key];
        const d = new Date(grp.dateMillis);
        const dayNum = String(d.getDate()).padStart(2, '0');
        const dayOfWeekIdx = d.getDay();
        const dayOfWeekStr = dayNames[dayOfWeekIdx];
        const monthStr = String(d.getMonth() + 1).padStart(2, '0');
        const yearStr = d.getFullYear();
        const monthYearStr = monthStr + '.' + yearStr;

        let chipClass = 'day-chip-weekday';
        if (dayOfWeekIdx === 0) chipClass = 'day-chip-sun';
        else if (dayOfWeekIdx === 6) chipClass = 'day-chip-sat';

        const incomeHtml = grp.totalIncome > 0 ? `<span class="day-income-total">+ ${'$'}{formatAmount(grp.totalIncome)}</span>` : '';
        const expenseHtml = grp.totalExpense > 0 ? `<span class="day-expense-total">- ${'$'}{formatAmount(grp.totalExpense)}</span>` : '';

        const txRowsHtml = grp.transactions.map(t => {
          const td = new Date(t.dateMillis);
          const timeStr = td.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

          const isTransfer = t.type === 'TRANSFER';
          const isIncome = t.type === 'INCOME';
          
          let amountColorClass = 'amount-expense';
          let sign = '-';
          if (isIncome) {
            amountColorClass = 'amount-income';
            sign = '+';
          } else if (isTransfer) {
            amountColorClass = 'amount-transfer';
            sign = '⇄';
          }

          // In app: note is primary highlighted text if present, else payee, else subcategory, else category
          let primaryText = t.note || t.payee || (isTransfer ? 'Transfer' : (t.categoryName || 'Expense'));
          let secondaryText = '';
          if (t.note && t.payee) {
            secondaryText = t.payee;
          } else if (t.subcategoryName) {
            secondaryText = t.subcategoryName;
          }

          let categoryDisplay = isTransfer ? '⇄ Transfer' : (t.categoryName || 'General');
          let accountDisplay = isTransfer && t.toAccountName
            ? escapeHtml(t.accountName) + ' → ' + escapeHtml(t.toAccountName)
            : escapeHtml(t.accountName || 'Account');

          return `
            <div class="app-tx-row" onclick="openEditModal(${'$'}{t.id})">
              <div class="app-tx-left">
                <div class="tx-cat-chip">${'$'}{escapeHtml(categoryDisplay)}</div>
                <div class="tx-acc-subtitle">${'$'}{accountDisplay}</div>
              </div>

              <div class="app-tx-center">
                <div class="tx-primary-text">${'$'}{escapeHtml(primaryText)}</div>
                ${'$'}{secondaryText ? `<div class="tx-secondary-text">${'$'}{escapeHtml(secondaryText)}</div>` : ''}
              </div>

              <div class="app-tx-right">
                <div class="tx-amount-col">
                  <div class="tx-amount-val ${'$'}{amountColorClass}">
                    ${'$'}{sign} ${'$'}{formatAmount(t.amount)}
                  </div>
                  <div class="tx-time-val">${'$'}{timeStr}</div>
                </div>

                <div class="tx-actions-col">
                  <button class="action-icon-btn" title="Edit" onclick="event.stopPropagation(); openEditModal(${'$'}{t.id})">✏️</button>
                  <button class="action-icon-btn btn-del" title="Delete" onclick="event.stopPropagation(); deleteTransaction(${'$'}{t.id})">✕</button>
                </div>
              </div>
            </div>
          `;
        }).join('');

        return `
          <div class="day-card">
            <div class="day-header">
              <div class="day-header-left">
                <span class="day-num">${'$'}{dayNum}</span>
                <span class="day-chip ${'$'}{chipClass}">${'$'}{dayOfWeekStr}</span>
                <span class="day-my">${'$'}{monthYearStr}</span>
              </div>
              <div class="day-header-right">
                ${'$'}{incomeHtml}
                ${'$'}{expenseHtml}
              </div>
            </div>
            <div class="day-transactions">
              ${'$'}{txRowsHtml}
            </div>
          </div>
        `;
      }).join('');
    }

    // Flat Table Format
    function renderTableView(transactions) {
      const tbody = document.getElementById('transactionsTbody');
      tbody.innerHTML = transactions.map(t => {
        const d = new Date(t.dateMillis);
        const dateStr = d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
        const timeStr = d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
        
        let badgeClass = 'badge-expense';
        let amountClass = 'amount-expense';
        let sign = '-';
        if (t.type === 'INCOME') {
          badgeClass = 'badge-income';
          amountClass = 'amount-income';
          sign = '+';
        } else if (t.type === 'TRANSFER') {
          badgeClass = 'badge-transfer';
          amountClass = 'amount-transfer';
          sign = '⇄';
        }

        const accountDisplay = t.type === 'TRANSFER' && t.toAccountName
          ? escapeHtml(t.accountName) + ' → ' + escapeHtml(t.toAccountName)
          : escapeHtml(t.accountName || '—');

        return `
          <tr>
            <td>
              <div style="font-weight: 600;">${'$'}{dateStr}</div>
              <div style="font-size: 11px; color: var(--text-muted);">${'$'}{timeStr}</div>
            </td>
            <td><span class="badge ${'$'}{badgeClass}">${'$'}{t.type}</span></td>
            <td><span style="font-weight: 500;">${'$'}{accountDisplay}</span></td>
            <td>${'$'}{escapeHtml(t.categoryName || (t.type === 'TRANSFER' ? 'Transfer' : 'Uncategorized'))}</td>
            <td>
              <div style="font-weight: 600;">${'$'}{escapeHtml(t.payee || '—')}</div>
              <div style="color: var(--text-muted); font-size: 11px;">${'$'}{escapeHtml(t.note || '')}</div>
            </td>
            <td style="text-align: right; font-weight: 700;" class="${'$'}{amountClass}">
              ${'$'}{sign} ${'$'}{formatAmount(t.amount)}
            </td>
            <td style="text-align: center;">
              <button class="btn btn-secondary" style="padding: 4px 8px; font-size: 11px;" onclick="openEditModal(${'$'}{t.id})">Edit</button>
              <button class="btn btn-danger" style="padding: 4px 8px; font-size: 11px; margin-left: 4px;" onclick="deleteTransaction(${'$'}{t.id})">✕</button>
            </td>
          </tr>
        `;
      }).join('');
    }

    function renderAccounts() {
      const grid = document.getElementById('accountsGrid');
      grid.innerHTML = appState.accounts.map(a => `
        <div class="account-card">
          <div>
            <div class="account-name">${'$'}{escapeHtml(a.name)}</div>
            <div class="account-type">${'$'}{escapeHtml(a.type)}</div>
          </div>
          <div class="account-balance" style="color: ${'$'}{(a.balance || 0) < 0 ? 'var(--expense)' : 'var(--income)'};">
            ${'$'}{formatAmount(a.balance || 0)}
          </div>
        </div>
      `).join('');
    }

    function formatInputDate(d) {
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return y + '-' + m + '-' + day;
    }

    function openAddTransactionModal() {
      document.getElementById('txModalTitle').textContent = 'New Transaction';
      document.getElementById('txEditId').value = '0';
      document.getElementById('txAmount').value = '';
      document.getElementById('txPayee').value = '';
      document.getElementById('txNote').value = '';

      const now = new Date();
      document.getElementById('txDate').value = formatInputDate(now);
      document.getElementById('txTime').value = String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0');

      setTxType('EXPENSE');
      populateModalAccountAndCategory();
      document.getElementById('transactionModal').classList.add('open');
      document.getElementById('txAmount').focus();
    }

    function openEditModal(txId) {
      const tx = appState.transactions.find(t => t.id === txId);
      if (!tx) return;

      document.getElementById('txModalTitle').textContent = 'Edit Transaction';
      document.getElementById('txEditId').value = tx.id;
      document.getElementById('txAmount').value = (tx.amount / 100).toFixed(2);
      document.getElementById('txPayee').value = tx.payee || '';
      document.getElementById('txNote').value = tx.note || '';

      const d = new Date(tx.dateMillis);
      document.getElementById('txDate').value = formatInputDate(d);
      document.getElementById('txTime').value = String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');

      setTxType(tx.type);
      populateModalAccountAndCategory(tx.accountId, tx.toAccountId, tx.categoryId);
      document.getElementById('transactionModal').classList.add('open');
    }

    function closeTransactionModal() {
      document.getElementById('transactionModal').classList.remove('open');
    }

    function setTxType(type) {
      appState.selectedType = type;
      document.getElementById('btnTypeExpense').className = 'type-btn ' + (type === 'EXPENSE' ? 'selected-expense' : '');
      document.getElementById('btnTypeIncome').className = 'type-btn ' + (type === 'INCOME' ? 'selected-income' : '');
      document.getElementById('btnTypeTransfer').className = 'type-btn ' + (type === 'TRANSFER' ? 'selected-transfer' : '');

      const toAccGroup = document.getElementById('txToAccountGroup');
      const catGroup = document.getElementById('txCategoryGroup');
      const accLabel = document.getElementById('txAccountLabel');

      if (type === 'TRANSFER') {
        toAccGroup.style.display = 'flex';
        catGroup.style.display = 'none';
        accLabel.textContent = 'From Account';
      } else {
        toAccGroup.style.display = 'none';
        catGroup.style.display = 'flex';
        accLabel.textContent = 'Account';
      }
      populateModalAccountAndCategory();
    }

    function populateModalAccountAndCategory(selectedAccId, selectedToAccId, selectedCatId) {
      const accSelect = document.getElementById('txAccount');
      const toAccSelect = document.getElementById('txToAccount');
      const catSelect = document.getElementById('txCategory');

      accSelect.innerHTML = appState.accounts.map(a => `<option value="${'$'}{a.id}">${'$'}{escapeHtml(a.name)}</option>`).join('');
      toAccSelect.innerHTML = appState.accounts.map(a => `<option value="${'$'}{a.id}">${'$'}{escapeHtml(a.name)}</option>`).join('');

      const filteredCats = appState.categories.filter(c => c.type === appState.selectedType);
      catSelect.innerHTML = filteredCats.map(c => `<option value="${'$'}{c.id}">${'$'}{escapeHtml(c.name)}</option>`).join('');

      if (selectedAccId) accSelect.value = selectedAccId;
      if (selectedToAccId) toAccSelect.value = selectedToAccId;
      if (selectedCatId) catSelect.value = selectedCatId;
    }

    async function saveTransaction() {
      if (isSavingTx) return;

      const editId = parseInt(document.getElementById('txEditId').value, 10) || 0;
      const amountVal = parseFloat(document.getElementById('txAmount').value);
      if (isNaN(amountVal) || amountVal <= 0) {
        alert("Please enter a valid positive amount.");
        return;
      }
      const amountMinor = Math.round(amountVal * 100);

      let accountId = parseInt(document.getElementById('txAccount').value, 10);
      if (isNaN(accountId) || accountId <= 0) {
        if (appState.accounts && appState.accounts.length > 0) {
          accountId = appState.accounts[0].id;
        } else {
          alert("Please create an account first before adding transactions.");
          return;
        }
      }

      let toAccountId = null;
      if (appState.selectedType === 'TRANSFER') {
        toAccountId = parseInt(document.getElementById('txToAccount').value, 10);
        if (isNaN(toAccountId) || toAccountId <= 0) {
          const other = appState.accounts.find(a => a.id !== accountId);
          toAccountId = other ? other.id : accountId;
        }
      }

      let categoryId = 0;
      if (appState.selectedType !== 'TRANSFER') {
        categoryId = parseInt(document.getElementById('txCategory').value || '0', 10);
        if (isNaN(categoryId) || categoryId <= 0) {
          const matching = appState.categories.find(c => c.type === appState.selectedType);
          categoryId = matching ? matching.id : 0;
        }
      }

      const dateStr = document.getElementById('txDate').value;
      const timeStr = document.getElementById('txTime').value || '12:00';
      let dateMillis = Date.now();
      if (dateStr) {
        const parts = dateStr.split('-');
        if (parts.length === 3) {
          const timeParts = timeStr.split(':');
          const hr = parseInt(timeParts[0] || '12', 10);
          const mn = parseInt(timeParts[1] || '0', 10);
          const parsed = new Date(parseInt(parts[0], 10), parseInt(parts[1], 10) - 1, parseInt(parts[2], 10), hr, mn).getTime();
          if (!isNaN(parsed) && parsed > 0) {
            dateMillis = parsed;
          }
        }
      }

      const payee = document.getElementById('txPayee').value.trim();
      const note = document.getElementById('txNote').value.trim();

      const payload = {
        id: editId,
        type: appState.selectedType,
        amount: amountMinor,
        accountId: accountId,
        toAccountId: toAccountId,
        categoryId: categoryId,
        dateMillis: dateMillis,
        payee: payee,
        note: note
      };

      const saveBtn = document.getElementById('btnSaveTx');
      const origBtnText = saveBtn.innerHTML;

      try {
        isSavingTx = true;
        saveBtn.disabled = true;
        saveBtn.innerHTML = '<span class="spinner"></span> Saving...';

        const url = editId > 0 ? '/api/transactions/update' : '/api/transactions';
        const res = await apiFetch(url, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });

        const respData = await res.json();
        if (respData.success) {
          closeTransactionModal();
          showToast(editId > 0 ? "Transaction updated successfully!" : "Transaction saved successfully!");
          await loadAllData();
        } else {
          alert("Error saving transaction: " + (respData.error || "Please check input and try again."));
        }
      } catch (e) {
        console.error("Save error:", e);
        alert("Failed to save transaction: " + (e.message || "Network error. Make sure Cash Tracker is running."));
      } finally {
        isSavingTx = false;
        saveBtn.disabled = false;
        saveBtn.innerHTML = origBtnText;
      }
    }

    async function deleteTransaction(txId) {
      if (!confirm("Are you sure you want to delete this transaction?")) return;
      try {
        const res = await apiFetch('/api/transactions/delete', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ id: txId })
        });
        const respData = await res.json();
        if (respData.success) {
          showToast("Transaction deleted.");
          loadAllData();
        } else {
          alert("Could not delete transaction.");
        }
      } catch (e) {
        alert("Failed to delete transaction.");
      }
    }

    function openAddAccountModal() {
      document.getElementById('accName').value = '';
      document.getElementById('accInitialBalance').value = '';
      document.getElementById('accountModal').classList.add('open');
    }

    function closeAccountModal() {
      document.getElementById('accountModal').classList.remove('open');
    }

    async function saveAccount() {
      if (isSavingAcc) return;

      const name = document.getElementById('accName').value.trim();
      if (!name) { alert("Please enter an account name."); return; }
      const type = document.getElementById('accType').value;
      const initBalVal = parseFloat(document.getElementById('accInitialBalance').value || '0');
      const initialBalance = Math.round(initBalVal * 100);

      const saveBtn = document.getElementById('btnSaveAcc');
      const origBtnText = saveBtn.innerHTML;

      try {
        isSavingAcc = true;
        saveBtn.disabled = true;
        saveBtn.innerHTML = '<span class="spinner"></span> Creating...';

        const res = await apiFetch('/api/accounts', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name, type, initialBalance })
        });
        const data = await res.json();
        if (data.success) {
          closeAccountModal();
          showToast("Account created successfully!");
          await loadAllData();
        } else {
          alert("Error creating account.");
        }
      } catch (e) {
        alert("Failed to create account.");
      } finally {
        isSavingAcc = false;
        saveBtn.disabled = false;
        saveBtn.innerHTML = origBtnText;
      }
    }

    function downloadCsv() {
      window.location.href = '/api/export/csv';
    }

    function downloadJsonBackup() {
      window.location.href = '/api/export/json';
    }

    function escapeHtml(text) {
      if (!text) return '';
      return String(text)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
    }

    window.onload = init;
  </script>
</body>
</html>
        """.trimIndent()
    }
}
