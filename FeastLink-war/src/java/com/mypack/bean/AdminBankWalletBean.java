package com.mypack.bean;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.annotation.Resource;

import javax.sql.DataSource;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Admin Bank Wallet (fixed AdminUserId = 1).
 *
 * ✅ No Entity / No SessionBean changes
 * ✅ No @PersistenceContext / EntityManager (avoids PU deploy error)
 * ✅ Uses JDBC DataSource: jdbc/myFeastLink
 *
 * Fee rule:
 * - When Bookings.PaymentStatus = 'PAID', collect 2% of TotalAmount into admin wallet.
 * - Idempotent: ONE fee per booking using TransferCode = FEE-BKG-{BookingId}
 * - Uses UPDLOCK+HOLDLOCK check so refreshing page won't add again.
 *
 * Fee history:
 * - Read from dbo.BankTransfers (TransferCode like 'FEE-BKG-%')
 * - Parses booking/restaurant info from the transfer message (stored at fee time)
 *
 * IMPORTANT:
 * - This page "syncs" fees on load + button click. It is not a background job.
 */
@Named("adminBankWalletBean")
@ViewScoped
public class AdminBankWalletBean implements Serializable {

    private static final long serialVersionUID = 1L;

    // Fixed admin id (as you requested)
    private static final long ADMIN_USER_ID = 1L;

    private static final BigDecimal FEE_RATE = new BigDecimal("0.02");

    @Resource(lookup = "jdbc/myFeastLink")
    private DataSource ds;

    // -------- schema cached --------
    private transient Set<String> btCols = null;
    private transient Set<String> vaCols = null;
    private transient Set<String> wCols  = null;
    private transient Set<String> wtCols = null;

    // ===== DTOs =====
    public static class WalletInfo implements Serializable {
        private Long walletId;
        private BigDecimal balance;
        private String status;

        public Long getWalletId() { return walletId; }
        public void setWalletId(Long walletId) { this.walletId = walletId; }
        public BigDecimal getBalance() { return balance; }
        public void setBalance(BigDecimal balance) { this.balance = balance; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class VirtualAccountInfo implements Serializable {
        private Long accountId;
        private String bankCode;
        private String accountNumber;
        private String displayName;
        private String status;

        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }
        public String getBankCode() { return bankCode; }
        public void setBankCode(String bankCode) { this.bankCode = bankCode; }
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class BookingFeeRow implements Serializable {
        private Timestamp createdAt;
        private String transferCode;
        private Long bookingId;
        private String bookingCode;
        private String restaurantName;
        private BigDecimal totalAmount;
        private BigDecimal feeAmount;

        public Timestamp getCreatedAt() { return createdAt; }
        public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
        public String getTransferCode() { return transferCode; }
        public void setTransferCode(String transferCode) { this.transferCode = transferCode; }
        public Long getBookingId() { return bookingId; }
        public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
        public String getBookingCode() { return bookingCode; }
        public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }
        public String getRestaurantName() { return restaurantName; }
        public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public BigDecimal getFeeAmount() { return feeAmount; }
        public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    }

    public static class TxnRow implements Serializable {
        private Long walletTxnId;
        private String direction;
        private BigDecimal amount;
        private Timestamp createdAt;
        private String transferCode;

        public Long getWalletTxnId() { return walletTxnId; }
        public void setWalletTxnId(Long walletTxnId) { this.walletTxnId = walletTxnId; }
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public Timestamp getCreatedAt() { return createdAt; }
        public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
        public String getTransferCode() { return transferCode; }
        public void setTransferCode(String transferCode) { this.transferCode = transferCode; }
    }

    // ===== state =====
    private WalletInfo adminWallet;
    private VirtualAccountInfo adminAccount;
    private List<BookingFeeRow> bookingFeeHistory = new ArrayList<>();
    private List<TxnRow> recentTxns = new ArrayList<>();

    // transfer form
    private String toAccountNumber;
    private BigDecimal transferAmount;
    private String transferMessage;

    private int lastFeeProcessed = 0;

    @PostConstruct
    public void init() {
        try {
            try (Connection conn = ds.getConnection()) {
                warmUpSchema(conn);
            }
            ensureAdminWalletAndAccount();
            syncPaidBookingFees(); // safe on every load (idempotent)
            reloadAll();
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Init error", safeMsg(e));
        }
    }

    public void refresh() {
        try {
            reloadAll();
            addMsg(FacesMessage.SEVERITY_INFO, "Refreshed", "Reloaded data.");
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Refresh error", safeMsg(e));
        }
    }

    // =========================================================
    // 1) SYNC FEE: PaymentStatus=PAID => +2% TotalAmount to admin
    // =========================================================
    private static class PaidBooking {
        long bookingId;
        String bookingCode;
        BigDecimal totalAmount;
        long restaurantId;
        String restaurantName;
        long customerId;
    }

    public void syncPaidBookingFees() {
        int processed = 0;
        Connection conn = null;

        try {
            conn = ds.getConnection();
            conn.setAutoCommit(false);
            try { conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE); } catch (Exception ignore) {}

            warmUpSchema(conn);

            ensureWalletExists(conn, ADMIN_USER_ID);

            WalletInfo wAdmin = loadWalletByUserId(conn, ADMIN_USER_ID);
            VirtualAccountInfo va = loadVirtualAccountByUserId(conn, ADMIN_USER_ID);
            final String adminAcc = (va == null ? null : va.getAccountNumber());

            List<PaidBooking> paid = loadPaidBookings(conn);

            for (PaidBooking b : paid) {
                BigDecimal total = nz(b.totalAmount);
                if (total.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal fee = total.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
                if (fee.compareTo(BigDecimal.ZERO) <= 0) continue;

                String feeCode = "FEE-BKG-" + b.bookingId;

                // ✅ LOCK + CHECK to avoid double fee (page refresh safe)
                if (existsTransferCodeLocked(conn, feeCode)) {
                    continue;
                }

                // Store info in message so history always can show booking + restaurant
                String msg = "BOOKING_FEE|BookingId=" + b.bookingId
                        + "|BookingCode=" + nn(b.bookingCode)
                        + "|Restaurant=" + nn(b.restaurantName)
                        + "|RestaurantId=" + b.restaurantId
                        + "|TotalAmount=" + total.toPlainString()
                        + "|FeeAmount=" + fee.toPlainString();

                Long transferId = insertTransferAndGetIdSmart(conn, feeCode, b.customerId, ADMIN_USER_ID, adminAcc, fee, msg);

                // wallet txn record (optional TransferId)
                insertWalletTxnSmart(conn, wAdmin.getWalletId(), transferId, "CREDIT", fee);

                // ✅ update wallet LAST
                execUpdate(conn,
                        "UPDATE dbo.Wallets SET CurrentBalance = CurrentBalance + ?, UpdatedAt = SYSDATETIME() WHERE UserId = ?",
                        ps -> { ps.setBigDecimal(1, fee); ps.setLong(2, ADMIN_USER_ID); });

                processed++;
            }

            conn.commit();
            lastFeeProcessed = processed;

            if (processed > 0) {
                reloadAll();
                addMsg(FacesMessage.SEVERITY_INFO, "Fee sync OK", "Collected from " + processed + " PAID booking(s).");
            }

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignore) {}
            }
            addMsg(FacesMessage.SEVERITY_ERROR, "Fee sync error", safeMsg(e));
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception ignore) {}
        }
    }

    private boolean existsTransferCodeLocked(Connection conn, String transferCode) {
        // If TransferCode column doesn't exist, we can't guarantee idempotent.
        // But in your case you already have TransferCode (from earlier UI).
        if (!hasCol(btCols, "TransferCode")) return false;

        try {
            Long id = queryOne(conn,
                    "SELECT TOP 1 " + btIdCol() + " FROM dbo.BankTransfers WITH (UPDLOCK, HOLDLOCK) WHERE TransferCode = ?",
                    ps -> ps.setString(1, transferCode),
                    rs -> rs.getLong(1));
            return id != null;
        } catch (Exception ignore) {
            return false;
        }
    }

    private List<PaidBooking> loadPaidBookings(Connection conn) throws Exception {
        List<PaidBooking> rows = queryList(conn,
                "SELECT TOP 500 BookingId, BookingCode, TotalAmount, RestaurantId, CustomerId FROM dbo.Bookings WHERE PaymentStatus = N'PAID' ORDER BY BookingId DESC",
                ps -> {},
                rs -> {
                    PaidBooking b = new PaidBooking();
                    b.bookingId = rs.getLong(1);
                    b.bookingCode = rs.getString(2);
                    b.totalAmount = rs.getBigDecimal(3);
                    b.restaurantId = rs.getLong(4);
                    b.customerId = rs.getLong(5);
                    b.restaurantName = null;
                    return b;
                });

        // Try load restaurant name (best-effort, won't break)
        for (PaidBooking b : rows) {
            try {
                String rn = queryOne(conn,
                        "SELECT TOP 1 [Name] FROM dbo.Restaurants WHERE RestaurantId = ?",
                        ps -> ps.setLong(1, b.restaurantId),
                        rs -> rs.getString(1));
                b.restaurantName = rn;
            } catch (Exception ignore) {
                b.restaurantName = null;
            }
        }
        return rows;
    }

    // =========================================================
    // 2) HISTORY + RECENT TXNS
    // =========================================================
    private void reloadAll() throws Exception {
        try (Connection conn = ds.getConnection()) {
            warmUpSchema(conn);
            adminWallet = loadWalletByUserId(conn, ADMIN_USER_ID);
            adminAccount = loadVirtualAccountByUserId(conn, ADMIN_USER_ID);
            bookingFeeHistory = loadFeeHistory(conn);
            recentTxns = loadRecentTxns(conn, adminWallet == null ? null : adminWallet.getWalletId());
        }
    }

    private List<BookingFeeRow> loadFeeHistory(Connection conn) {
        /*
         * ✅ FIX: Always show Restaurant name by JOINing Bookings + Restaurants
         * Do NOT depend on Message parsing (old rows may not have Restaurant in message).
         *
         * We parse BookingId from TransferCode: FEE-BKG-{BookingId}
         */
        final String sql =
                "SELECT TOP 80 " +
                        "bt.CreatedAt, " +
                        "b.BookingId, b.BookingCode, " +
                        "r.[Name] AS RestaurantName, " +
                        "b.TotalAmount, " +
                        "bt.Amount AS FeeAmount, " +
                        "bt.TransferCode " +
                "FROM dbo.BankTransfers bt " +
                "LEFT JOIN dbo.Bookings b " +
                    "ON b.BookingId = TRY_CONVERT(BIGINT, SUBSTRING(bt.TransferCode, 9, 50)) " +
                "LEFT JOIN dbo.Restaurants r " +
                    "ON r.RestaurantId = b.RestaurantId " +
                "WHERE bt.TransferCode LIKE N'FEE-BKG-%' AND bt.ToUserId = ? " +
                "ORDER BY bt.CreatedAt DESC, bt.TransferId DESC";

        try {
            return queryList(conn, sql,
                    ps -> ps.setLong(1, ADMIN_USER_ID),
                    rs -> {
                        BookingFeeRow row = new BookingFeeRow();
                        row.setCreatedAt(rs.getTimestamp(1));

                        try { row.setBookingId(rs.getLong(2)); } catch (Exception ignore) {}
                        row.setBookingCode(rs.getString(3));
                        row.setRestaurantName(rs.getString(4));
                        try { row.setTotalAmount(nz(rs.getBigDecimal(5))); } catch (Exception ignore) {}
                        row.setFeeAmount(nz(rs.getBigDecimal(6)));
                        row.setTransferCode(rs.getString(7));

                        // Fallback: if bookingId null, derive from transfer code
                        if (row.getBookingId() == null && row.getTransferCode() != null && row.getTransferCode().startsWith("FEE-BKG-")) {
                            try { row.setBookingId(Long.parseLong(row.getTransferCode().substring("FEE-BKG-".length()))); } catch (Exception ignore) {}
                        }
                        return row;
                    });
        } catch (Exception ignore) {
            return new ArrayList<>();
        }
    }

    private void parseFeeMessageIntoRow(BookingFeeRow row, String msg) {
        if (msg == null) return;
        if (!msg.startsWith("BOOKING_FEE")) return;

        String[] parts = msg.split("\\|");
        for (String p : parts) {
            if (p.startsWith("BookingId=")) {
                try { row.setBookingId(Long.parseLong(p.substring("BookingId=".length()))); } catch (Exception ignore) {}
            } else if (p.startsWith("BookingCode=")) {
                row.setBookingCode(p.substring("BookingCode=".length()));
            } else if (p.startsWith("Restaurant=")) {
                row.setRestaurantName(p.substring("Restaurant=".length()));
            } else if (p.startsWith("TotalAmount=")) {
                try { row.setTotalAmount(new BigDecimal(p.substring("TotalAmount=".length())).setScale(2, RoundingMode.HALF_UP)); } catch (Exception ignore) {}
            } else if (p.startsWith("FeeAmount=")) {
                try { row.setFeeAmount(new BigDecimal(p.substring("FeeAmount=".length())).setScale(2, RoundingMode.HALF_UP)); } catch (Exception ignore) {}
            }
        }
    }

    private List<TxnRow> loadRecentTxns(Connection conn, Long walletId) throws Exception {
        if (walletId == null) return new ArrayList<>();

        // Try join to BankTransfers by TransferId if exists
        if (hasCol(wtCols, "TransferId") && hasCol(btCols, btIdCol())) {
            try {
                return queryList(conn,
                        "SELECT TOP 30 wt.WalletTxnId, wt.Direction, wt.Amount, wt.CreatedAt, bt.TransferCode " +
                                "FROM dbo.WalletTransactions wt " +
                                "LEFT JOIN dbo.BankTransfers bt ON wt.TransferId = bt." + btIdCol() + " " +
                                "WHERE wt.WalletId = ? ORDER BY wt.CreatedAt DESC, wt.WalletTxnId DESC",
                        ps -> ps.setLong(1, walletId),
                        rs -> {
                            TxnRow t = new TxnRow();
                            t.setWalletTxnId(rs.getLong(1));
                            t.setDirection(rs.getString(2));
                            t.setAmount(nz(rs.getBigDecimal(3)));
                            t.setCreatedAt(rs.getTimestamp(4));
                            t.setTransferCode(rs.getString(5));
                            return t;
                        });
            } catch (Exception ignore) {}
        }

        // Fallback without join
        return queryList(conn,
                "SELECT TOP 30 WalletTxnId, Direction, Amount, CreatedAt FROM dbo.WalletTransactions WHERE WalletId = ? ORDER BY CreatedAt DESC, WalletTxnId DESC",
                ps -> ps.setLong(1, walletId),
                rs -> {
                    TxnRow t = new TxnRow();
                    t.setWalletTxnId(rs.getLong(1));
                    t.setDirection(rs.getString(2));
                    t.setAmount(nz(rs.getBigDecimal(3)));
                    t.setCreatedAt(rs.getTimestamp(4));
                    t.setTransferCode(null);
                    return t;
                });
    }

    // =========================================================
    // 3) TRANSFER (Admin -> Other AccountNumber)
    // =========================================================
    public void doTransfer() {
        if (toAccountNumber == null || toAccountNumber.trim().isEmpty()) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Invalid", "Receiver account number required.");
            return;
        }
        if (transferAmount == null || transferAmount.compareTo(new BigDecimal("0.01")) < 0) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Invalid", "Amount must be > 0.");
            return;
        }

        final BigDecimal amt = transferAmount.setScale(2, RoundingMode.HALF_UP);
        final String toAcc = toAccountNumber.trim();
        final String msg = (transferMessage == null ? null : transferMessage.trim());

        Connection conn = null;
        try {
            conn = ds.getConnection();
            conn.setAutoCommit(false);

            warmUpSchema(conn);

            ensureWalletExists(conn, ADMIN_USER_ID);

            Long toUserId = findActiveUserIdByAccountNumber(conn, toAcc);
            if (toUserId == null) {
                conn.rollback();
                addMsg(FacesMessage.SEVERITY_ERROR, "Not found", "Receiver account not found/ACTIVE.");
                return;
            }
            if (toUserId == ADMIN_USER_ID) {
                conn.rollback();
                addMsg(FacesMessage.SEVERITY_ERROR, "Invalid", "Cannot transfer to yourself.");
                return;
            }

            ensureWalletExists(conn, toUserId);

            // debit admin if enough
            int ok = execUpdate(conn,
                    "UPDATE dbo.Wallets SET CurrentBalance = CurrentBalance - ?, UpdatedAt = SYSDATETIME() " +
                            "WHERE UserId = ? AND CurrentBalance >= ?",
                    ps -> { ps.setBigDecimal(1, amt); ps.setLong(2, ADMIN_USER_ID); ps.setBigDecimal(3, amt); });

            if (ok == 0) {
                conn.rollback();
                addMsg(FacesMessage.SEVERITY_ERROR, "Insufficient", "Not enough balance.");
                return;
            }

            execUpdate(conn,
                    "UPDATE dbo.Wallets SET CurrentBalance = CurrentBalance + ?, UpdatedAt = SYSDATETIME() WHERE UserId = ?",
                    ps -> { ps.setBigDecimal(1, amt); ps.setLong(2, toUserId); });

            WalletInfo wAdmin = loadWalletByUserId(conn, ADMIN_USER_ID);
            WalletInfo wTo = loadWalletByUserId(conn, toUserId);

            String code = generateTransferCode();
            Long transferId = insertTransferAndGetIdSmart(conn, code, ADMIN_USER_ID, toUserId, toAcc, amt, msg);

            insertWalletTxnSmart(conn, wAdmin.getWalletId(), transferId, "DEBIT", amt);
            insertWalletTxnSmart(conn, wTo.getWalletId(), transferId, "CREDIT", amt);

            conn.commit();

            toAccountNumber = null;
            transferAmount = null;
            transferMessage = null;

            reloadAll();
            addMsg(FacesMessage.SEVERITY_INFO, "Transfer success", "TransferCode: " + code);

        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (Exception ignore) {}
            addMsg(FacesMessage.SEVERITY_ERROR, "Transfer error", safeMsg(e));
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception ignore) {}
        }
    }

    // =========================================================
    // 4) ENSURE ADMIN WALLET + VIRTUAL ACCOUNT
    // =========================================================
    private void ensureAdminWalletAndAccount() throws Exception {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            conn.setAutoCommit(false);

            warmUpSchema(conn);

            ensureWalletExists(conn, ADMIN_USER_ID);

            VirtualAccountInfo va = loadVirtualAccountByUserId(conn, ADMIN_USER_ID);
            if (va == null) {
                String displayName = loadUserFullName(conn, ADMIN_USER_ID);
                if (displayName == null || displayName.trim().isEmpty()) displayName = "ADMIN";

                String acc = generateAccountNumber(ADMIN_USER_ID);

                final long adminId = ADMIN_USER_ID;
                final String accFinal = acc;
                final String displayNameFinal = displayName;

                // Insert VA with columns that exist in your schema
                StringBuilder cols = new StringBuilder("UserId, BankCode, AccountNumber, DisplayName");
                StringBuilder vals = new StringBuilder("?, N'FEASTBANK', ?, ?");

                if (hasCol(vaCols, "Status")) { cols.append(", [Status]"); vals.append(", N'ACTIVE'"); }
                if (hasCol(vaCols, "CreatedAt")) { cols.append(", CreatedAt"); vals.append(", SYSDATETIME()"); }

                String sql = "INSERT INTO dbo.VirtualAccounts (" + cols + ") VALUES (" + vals + ")";
                execUpdate(conn, sql, ps -> { ps.setLong(1, adminId); ps.setString(2, accFinal); ps.setString(3, displayNameFinal); });
            }

            conn.commit();
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (Exception ignore) {}
            throw e;
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception ignore) {}
        }
    }

    private void ensureWalletExists(Connection conn, Long userId) throws Exception {
        WalletInfo w = loadWalletByUserId(conn, userId);
        if (w == null) {
            StringBuilder cols = new StringBuilder("UserId, CurrentBalance");
            StringBuilder vals = new StringBuilder("?, 0");
            if (hasCol(wCols, "Status")) { cols.append(", [Status]"); vals.append(", N'ACTIVE'"); }
            if (hasCol(wCols, "CreatedAt")) { cols.append(", CreatedAt"); vals.append(", SYSDATETIME()"); }
            String sql = "INSERT INTO dbo.Wallets (" + cols + ") VALUES (" + vals + ")";
            execUpdate(conn, sql, ps -> ps.setLong(1, userId));
        }
    }

    private WalletInfo loadWalletByUserId(Connection conn, Long userId) throws Exception {
        String statusCol = hasCol(wCols, "Status") ? "[Status]" : null;

        String sql = "SELECT TOP 1 WalletId, CurrentBalance" + (statusCol != null ? (", " + statusCol) : "") +
                " FROM dbo.Wallets WHERE UserId = ?";

        return queryOne(conn, sql, ps -> ps.setLong(1, userId), rs -> {
            WalletInfo w = new WalletInfo();
            w.setWalletId(rs.getLong(1));
            w.setBalance(nz(rs.getBigDecimal(2)));
            if (statusCol != null) w.setStatus(rs.getString(3));
            else w.setStatus("ACTIVE");
            return w;
        });
    }

    private VirtualAccountInfo loadVirtualAccountByUserId(Connection conn, Long userId) throws Exception {
        String statusCol = hasCol(vaCols, "Status") ? "[Status]" : null;

        String sql = "SELECT TOP 1 AccountId, BankCode, AccountNumber, DisplayName" + (statusCol != null ? (", " + statusCol) : "") +
                " FROM dbo.VirtualAccounts WHERE UserId = ?";

        return queryOne(conn, sql, ps -> ps.setLong(1, userId), rs -> {
            VirtualAccountInfo a = new VirtualAccountInfo();
            a.setAccountId(rs.getLong(1));
            a.setBankCode(rs.getString(2));
            a.setAccountNumber(rs.getString(3));
            a.setDisplayName(rs.getString(4));
            if (statusCol != null) a.setStatus(rs.getString(5));
            else a.setStatus("ACTIVE");
            return a;
        });
    }

    private Long findActiveUserIdByAccountNumber(Connection conn, String acc) throws Exception {
        String statusCol = hasCol(vaCols, "Status") ? "[Status]" : null;

        String sql = "SELECT TOP 1 UserId" + (statusCol != null ? (", " + statusCol) : "") +
                " FROM dbo.VirtualAccounts WHERE AccountNumber = ?";

        return queryOne(conn, sql, ps -> ps.setString(1, acc), rs -> {
            Long uid = rs.getLong(1);
            if (statusCol != null) {
                String st = rs.getString(2);
                if (st != null && !st.equalsIgnoreCase("ACTIVE")) return null;
            }
            return uid;
        });
    }

    private String loadUserFullName(Connection conn, Long userId) {
        try {
            return queryOne(conn,
                    "SELECT TOP 1 FullName FROM dbo.Users WHERE UserId = ?",
                    ps -> ps.setLong(1, userId),
                    rs -> rs.getString(1));
        } catch (Exception ignore) {
            return "ADMIN";
        }
    }

    // =========================================================
    // 5) INSERT SMART: BankTransfers / WalletTransactions
    // =========================================================
    private Long insertTransferAndGetIdSmart(Connection conn,
                                            String code,
                                            Long fromUserId,
                                            Long toUserId,
                                            String toAcc,
                                            BigDecimal amount,
                                            String message) throws Exception {

        // columns
        final String idCol = btIdCol();
        final boolean hasId = hasCol(btCols, idCol);
        final boolean hasCode = hasCol(btCols, "TransferCode");
        final boolean hasFrom = hasCol(btCols, "FromUserId");
        final boolean hasTo = hasCol(btCols, "ToUserId");
        final boolean hasToAcc = hasCol(btCols, "ToAccountNumber");
        final boolean hasAmount = hasCol(btCols, "Amount"); // ✅ your schema has Amount, NOT TransferAmount
        final String msgCol = btMessageCol();
        final boolean hasStatus = hasCol(btCols, "Status");
        final boolean hasCreatedAt = hasCol(btCols, "CreatedAt");

        if (!hasCode || !hasFrom || !hasTo || !hasAmount) {
            throw new SQLException("BankTransfers schema missing required columns (TransferCode/FromUserId/ToUserId/Amount).");
        }

        StringBuilder cols = new StringBuilder("TransferCode, FromUserId, ToUserId");
        StringBuilder vals = new StringBuilder("?, ?, ?");

        List<Object> params = new ArrayList<>();
        params.add(code);
        params.add(fromUserId);
        params.add(toUserId);

        if (hasToAcc) {
            cols.append(", ToAccountNumber");
            vals.append(", ?");
            params.add(toAcc);
        }

        cols.append(", Amount");
        vals.append(", ?");
        params.add(amount);

        if (msgCol != null) {
            cols.append(", ").append(msgCol);
            vals.append(", ?");
            params.add(message);
        }

        if (hasStatus) {
            cols.append(", [Status]");
            vals.append(", N'SUCCESS'");
        }

        if (hasCreatedAt) {
            cols.append(", CreatedAt");
            vals.append(", SYSDATETIME()");
        }

        String sql = "INSERT INTO dbo.BankTransfers (" + cols + ") " +
                (hasId ? ("OUTPUT INSERTED." + idCol + " ") : "") +
                "VALUES (" + vals + ")";

        if (hasId) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bindParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        } else {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bindParams(ps, params);
                ps.executeUpdate();
                return null;
            }
        }
    }

    private void insertWalletTxnSmart(Connection conn,
                                     Long walletId,
                                     Long transferId,
                                     String direction,
                                     BigDecimal amount) throws Exception {

        final boolean hasTransferId = hasCol(wtCols, "TransferId");
        final boolean hasDirection  = hasCol(wtCols, "Direction");
        final boolean hasCreatedAt  = hasCol(wtCols, "CreatedAt");

        StringBuilder cols = new StringBuilder("WalletId, Amount");
        StringBuilder vals = new StringBuilder("?, ?");
        List<Object> params = new ArrayList<>();
        params.add(walletId);
        params.add(amount);

        if (hasTransferId) {
            cols.append(", TransferId");
            vals.append(", ?");
            params.add(transferId);
        }
        if (hasDirection) {
            cols.append(", Direction");
            vals.append(", ?");
            params.add(direction);
        }
        if (hasCreatedAt) {
            cols.append(", CreatedAt");
            vals.append(", SYSDATETIME()");
        }

        String sql = "INSERT INTO dbo.WalletTransactions (" + cols + ") VALUES (" + vals + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParams(ps, params);
            ps.executeUpdate();
        }
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws Exception {
        int i = 1;
        for (Object o : params) {
            if (o == null) {
                ps.setNull(i++, Types.NULL);
            } else if (o instanceof Long) {
                ps.setLong(i++, (Long) o);
            } else if (o instanceof BigDecimal) {
                ps.setBigDecimal(i++, (BigDecimal) o);
            } else {
                ps.setString(i++, String.valueOf(o));
            }
        }
    }

    // =========================================================
    // 6) Schema utilities (fix Invalid column errors)
    // =========================================================
    private void warmUpSchema(Connection conn) throws Exception {
        if (btCols == null) btCols = loadColumns(conn, "BankTransfers");
        if (vaCols == null) vaCols = loadColumns(conn, "VirtualAccounts");
        if (wCols  == null) wCols  = loadColumns(conn, "Wallets");
        if (wtCols == null) wtCols = loadColumns(conn, "WalletTransactions");
    }

    private Set<String> loadColumns(Connection conn, String table) throws Exception {
        Set<String> cols = new HashSet<>();
        DatabaseMetaData md = conn.getMetaData();
        // Try dbo schema first, then any schema.
        try (ResultSet rs = md.getColumns(null, "dbo", table, null)) {
            while (rs.next()) cols.add(rs.getString("COLUMN_NAME"));
        }
        if (cols.isEmpty()) {
            try (ResultSet rs = md.getColumns(null, null, table, null)) {
                while (rs.next()) cols.add(rs.getString("COLUMN_NAME"));
            }
        }
        return cols;
    }

    private boolean hasCol(Set<String> cols, String name) {
        return cols != null && name != null && cols.contains(name);
    }

    private String btIdCol() {
        // Most common: TransferId
        if (hasCol(btCols, "TransferId")) return "TransferId";
        if (hasCol(btCols, "BankTransferId")) return "BankTransferId";
        if (hasCol(btCols, "Id")) return "Id";
        return "TransferId";
    }

    private String btMessageCol() {
        // Return exact column name (may need brackets in SQL if reserved)
        if (hasCol(btCols, "Message")) return "[Message]";
        if (hasCol(btCols, "Notes")) return "Notes";
        if (hasCol(btCols, "Description")) return "Description";
        if (hasCol(btCols, "Remark")) return "Remark";
        return null;
    }

    // =========================================================
    // 7) JDBC helpers
    // =========================================================
    private interface Binder { void bind(PreparedStatement ps) throws Exception; }
    private interface Mapper<T> { T map(ResultSet rs) throws Exception; }

    private int execUpdate(Connection conn, String sql, Binder binder) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate();
        }
    }

    private <T> T queryOne(Connection conn, String sql, Binder binder, Mapper<T> mapper) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapper.map(rs);
            }
        }
    }

    private <T> List<T> queryList(Connection conn, String sql, Binder binder, Mapper<T> mapper) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) out.add(mapper.map(rs));
                return out;
            }
        }
    }

    // =========================================================
    // 8) utils
    // =========================================================
    private BigDecimal nz(BigDecimal x) {
        if (x == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return x.setScale(2, RoundingMode.HALF_UP);
    }

    private String nn(String s) { return (s == null ? "" : s); }

    private void addMsg(FacesMessage.Severity sev, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, summary, detail));
    }

    private String safeMsg(Exception e) {
        return (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
    }

    private String generateTransferCode() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder("TRX-");
        sb.append(new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()));
        sb.append("-");
        for (int i = 0; i < 6; i++) sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
        return sb.toString();
    }

    private String generateAccountNumber(Long userId) {
        SecureRandom rnd = new SecureRandom();
        String base = "FEAST-" + userId + "-" + System.currentTimeMillis();
        if (base.length() > 18) base = base.substring(0, 18);
        return base + (rnd.nextInt(90) + 10);
    }

    // =========================================================
    // getters/setters for JSF
    // =========================================================
    public WalletInfo getAdminWallet() { return adminWallet; }
    public VirtualAccountInfo getAdminAccount() { return adminAccount; }
    public List<BookingFeeRow> getBookingFeeHistory() { return bookingFeeHistory; }
    public List<TxnRow> getRecentTxns() { return recentTxns; }

    public String getToAccountNumber() { return toAccountNumber; }
    public void setToAccountNumber(String toAccountNumber) { this.toAccountNumber = toAccountNumber; }
    public BigDecimal getTransferAmount() { return transferAmount; }
    public void setTransferAmount(BigDecimal transferAmount) { this.transferAmount = transferAmount; }
    public String getTransferMessage() { return transferMessage; }
    public void setTransferMessage(String transferMessage) { this.transferMessage = transferMessage; }

    public long getAdminUserId() { return ADMIN_USER_ID; }
    public String getFeeRate() { return FEE_RATE.toPlainString(); }
    public int getLastFeeProcessed() { return lastFeeProcessed; }
}
