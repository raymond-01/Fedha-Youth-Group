# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Common commands

This is a plain Java Swing + MySQL project without a build tool. Use `javac`/`java` directly, or configure your IDE (IntelliJ project files are already present).

### Compile all sources

From the project root:

```bash path=null start=null
mkdir -p bin
javac -cp "lib/mysql-connector-j-9.1.0.jar" -d bin src/*.java
```

On Windows, replace `:`/`"..."` conventions with the appropriate classpath syntax, e.g. `-cp "lib\\mysql-connector-j-9.1.0.jar;bin"` when running.

### Run the main application

The primary entrypoint is `HomePage` (main navigation window):

```bash path=null start=null
java -cp "bin:lib/mysql-connector-j-9.1.0.jar" HomePage
```

Other screens can be launched directly for focused work/debugging:

```bash path=null start=null
# Members management only
java -cp "bin:lib/mysql-connector-j-9.1.0.jar" MembersSection

# Loan management only
java -cp "bin:lib/mysql-connector-j-9.1.0.jar" LoanManagement

# Fixed deposit view/updates
java -cp "bin:lib/mysql-connector-j-9.1.0.jar" FixedDepositManagement

# Reports UI
java -cp "bin:lib/mysql-connector-j-9.1.0.jar" ReportsForm
```

### Database connection smoke test ("single test")

There is no automated test framework; the project includes a small connectivity test via `TestConnection`:

```bash path=null start=null
javac -cp "lib/mysql-connector-j-9.1.0.jar" -d bin src/DatabaseConnection.java src/TestConnection.java
java -cp "bin:lib/mysql-connector-j-9.1.0.jar" TestConnection
```

This will print whether the JDBC connection can be created successfully.

### Database prerequisites

All data operations assume a local MySQL instance with:

- URL: `jdbc:mysql://localhost:3306/FedhaYouthGroup`
- Credentials: defined in `src/DatabaseConnection.java` (update this file rather than hardcoding secrets elsewhere).
- Core tables referenced by the code (names and key columns):
  - `Members` / `members`: `MemberID`, `FullName`, `Age`, `Shares`, `RegistrationFee`, `OutstandingLoan`, `ExitNoticeGiven`, `Dividends`.
  - `contributions`: `MemberID`, `ContributionAmount` (used to derive total shares in some loan rules).
  - `loans`: `LoanID`, `MemberID`, `LoanAmount`, `LoanType`, `InterestRate`, `RepaymentPeriod`, `MonthlyRepayment`, `OutstandingBalance`, `GuarantorIDs`, `LoanStatus`.
  - `fixed_deposits`: `DepositID`, `TotalSavings`, `MonthlyInterest`, `AccumulatedInterest`, `LastUpdated`.

If queries start failing, check that these tables and columns exist, respecting the exact casing and names used in the SQL strings in the Java files.

## High-level architecture

### Overview

The application is a desktop Swing client for managing a youth group SACCO-style system. All persistence is via JDBC to a MySQL database. There is no explicit domain or repository layer; Swing forms directly execute SQL via a shared `DatabaseConnection` helper.

Key characteristics:

- **UI technology**: Java Swing, mostly programmatic layout (no FXML or GUI builder files).
- **Persistence**: raw JDBC with hard-coded SQL strings; no ORM or transaction abstraction.
- **Structure**: single `src/` directory with one class per screen/concern.
- **Config**: database URL/user/password live in `DatabaseConnection`.

### Core components

- `DatabaseConnection.java`
  - Provides a singleton-style `Connection` to the MySQL database.
  - Initializes the connection in a static block and re-creates it if closed (`getConnection()`), so all other classes call this helper instead of opening their own connections.
  - Any work that changes DB host, port, schema name, or credentials should be centralized here.

- `HomePage.java`
  - Acts as the main application entrypoint (`public static void main`).
  - Shows a welcome dialog and then a main menu frame with buttons:
    - **Members** → instantiates `MembersSection`.
    - **Loans** → instantiates `LoanManagement`.
    - **Fixed Deposits** → instantiates `FixedDepositManagement`.
    - **Reports** → instantiates `ReportsForm`.
    - **View Revenue** → placeholder dialog.
    - **Exit** → confirmation and `System.exit(0)`.
  - There is no global application controller; navigation is done by constructing new frames for each feature.

- `MembersSection.java`
  - Encapsulates all **member management** UI in a hidden-tab `JTabbedPane` with three logical screens:
    - **Main State**: high-level actions (add member, view all members).
    - **Form View**: form to add a new member, validating age (18–35) and minimum shares (> 1000) before inserting into `Members`.
    - **Table View**: table showing all members with computed loan eligibility.
  - DB interactions:
    - Reads all members from `Members` for the table, computing a derived "max loan amount" based on shares (`calculateLoanEligibility`).
    - Inserts new members into `Members` with fixed `RegistrationFee=1000`, `OutstandingLoan=0`, `ExitNoticeGiven=false`.
    - Aggregates total shares and total registration fees across `Members` for summary labels.
  - Note the project uses both capitalized `Members` and lower-case `members` in queries across files; schema should be created accordingly on case-sensitive systems.

- `LoanManagement.java`
  - Manages the **loan lifecycle** in another hidden-tab `JTabbedPane`:
    - **Main State**: navigation buttons.
    - **Loan Application Form**: creates new loans.
    - **View Loans**: table of existing loans.
    - **Repay Loan**: form to record repayments and update loan status.
  - Business rules are encoded in three `Map`s initialized in the constructor:
    - `loanInterestRates` – interest rate factors by type (`Emergency`, `Short`, `Normal`, `Development`).
    - `loanRepaymentPeriods` – repayment duration in months by type.
    - `loanMultipliers` – how many times a member’s shares determine max loan amount.
  - Key data flows:
    - **Eligibility check**: `isEligibleForLoan(memberId)` sums `ContributionAmount` from `contributions`; requires at least 4,000 to qualify.
    - **Max loan amount**: `getMaxLoanAmount` uses `loanMultipliers` × total shares (from `contributions`).
    - **Apply for loan**: `applyForLoan` inserts into `loans` with status `Active`, storing comma-separated guarantor IDs and initial `OutstandingBalance = LoanAmount`.
    - **View loans**: `loadLoansIntoTable` selects all key columns into a `DefaultTableModel` for a `JTable`.
    - **Repay loan**: `repayLoan` adjusts `OutstandingBalance` and flips `LoanStatus` to `Cleared` if fully paid.
  - Duplication/consistency note: loan eligibility logic also appears in `MembersSection.calculateLoanEligibility` with similar but not identical thresholds/multipliers; keep them in sync if business rules change.

- `FixedDepositManagement.java`
  - Handles **fixed deposit aggregation and interest accrual** for the group.
  - Main responsibilities:
    - Display the latest fixed deposit snapshot (total savings, monthly interest, accumulated interest) from the most recent row in `fixed_deposits`.
    - On **Update Fixed Deposit**:
      - Computes `TotalSavings` from `members.Shares` for members without active loans.
      - Calculates `MonthlyInterest` as a fixed 0.6% of `TotalSavings`.
      - Determines months elapsed since last update (`LastUpdated`) and multiplies to get `AccumulatedInterest`.
      - Inserts a new row into `fixed_deposits` with the current date.
    - On **Export to CSV**: writes the entire `fixed_deposits` table to `FixedDeposits.csv` in the working directory.
  - Uses `LocalDate` and `ChronoUnit.MONTHS` for date arithmetic; all DB interactions go through `DatabaseConnection`.

- `ReportsForm.java`
  - Single frame for generating and exporting various reports based on the database contents.
  - Report types (selected via combo box) and their backing queries:
    - **Members Report** – basic member data from `members`.
    - **Loans Report** – core loan fields from `loans`.
    - **Fixed Deposit Report** – rows from `fixed_deposits`.
    - **Dividends Report** – member-level `Dividends` information from `members`.
    - **Revenue Report** – aggregated revenue across loans and fixed deposits from `loans` and `fixed_deposits`.
    - **Exiting Members Report** – members where `ExitNoticeGiven = true`.
  - The generic helper `populateTableFromQuery` executes the given SQL and populates a shared `DefaultTableModel` for display.
  - CSV export uses a `JFileChooser` to let the user pick a target location, then writes the current table model to `<chosen-name>.csv`.

- `TestConnection.java`
  - Minimal command-line tool to validate the ability to obtain a `Connection` from `DatabaseConnection` and log success/failure.

### Project configuration files

- `.idea/` and `Fedha Youth Group.iml`
  - IntelliJ IDEA project metadata; you can open the project directly in IDEA instead of using raw `javac`.
- `.vscode/settings.json`
  - Editor configuration for VS Code (no build tasks or debug configurations are defined here by default).

## How future Warp agents should operate here

- Prefer using `DatabaseConnection.getConnection()` for any new DB interactions rather than creating ad-hoc connections.
- When adding new features, follow the existing pattern of one Swing frame/class per major functional area, and wire it from `HomePage` if it should be reachable from the main menu.
- Maintain consistency between loan eligibility rules in `MembersSection` and `LoanManagement` whenever the business logic changes.
- Be cautious when modifying table or column names in SQL strings; changes here must be coordinated with the actual MySQL schema used by the deployment environment.
