package com.mypack.bean;

import com.mypack.entity.RestaurantManagers;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Users;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import javax.sql.DataSource;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Restaurant Bank Wallet (per Manager account = per restaurant wallet)
 *
 * Rules:
 * - Eligible income: Bookings.PaymentStatus = 'PAID' AND Bookings.BookingStatus = 'COMPLETED'
 * - Net income = TotalAmount * 0.98 (fee 2%)
 * - Sync into Wallets.CurrentBalance (NET) with idempotent TransferCode: INC-BKG-{BookingId}
 * - Transfer uses NET (Wallets.CurrentBalance)
 *
 * IMPORTANT:
 * - No entity/sessionbean modifications required
 * - Uses JDBC DataSource: jdbc/myFeastLink
 */
@Named("restaurantBankWalletBean")
@ViewScoped
public class RestaurantBankWalletBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final BigDecimal FEE_RATE = new BigDecimal("0.02");
    private static final BigDecimal NET_RATE = new BigDecimal("0.98");

    @Resource(lookup = "jdbc/myFeastLink")
    private DataSource ds;

    // ===== UI state =====
    private Users currentUser;
    private Restaurants restaurant;

    private WalletInfo wallet;
    private VirtualAccountInfo account;

    private BigDecimal grossTotal = BigDecimal.ZERO; // SUM TotalAmount (eligible)
    private BigDecimal feeTotal   = BigDecimal.ZERO; // gross*2%
    private BigDecimal netTotal   = BigDecimal.ZERO; // gross-fee

    private int lastSynced = 0;

    private final List<BookingRow> rows = new ArrayList<>();

    // ✅ NEW: Incoming transfers (người khác chuyển vào wallet này)
    private final List<IncomingTransferRow> incomingTransfers = new ArrayList<>();
    private long lastSeenIncomingTransferId = 0;

    // transfer form
    private String toAccountNumber;
    private BigDecimal transferAmount;
    private String transferMessage;

    // ✅ FIX: tránh trường hợp ViewScoped giữ view cũ, ta có viewAction gọi mỗi lần GET
    public void onViewLoad() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null) return;

        // chỉ chạy khi GET / vào trang; postback (ajax) đã có action riêng
        if (!fc.isPostback()) {
            // nếu init chưa resolve xong thì thôi
            if (currentUser != null && restaurant != null) {
                refresh();
            }
        }
    }

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

    public static class BookingRow implements Serializable {
        private final Long bookingId;
        private final String bookingCode;
        private final Date eventDate;
        private final BigDecimal totalAmount;
        private final BigDecimal feeAmount;
        private final BigDecimal netAmount;

        public BookingRow(Long bookingId, String bookingCode, Date eventDate,
                          BigDecimal totalAmount, BigDecimal feeAmount, BigDecimal netAmount) {
            this.bookingId = bookingId;
            this.bookingCode = bookingCode;
            this.eventDate = eventDate;
            this.totalAmount = totalAmount;
            this.feeAmount = feeAmount;
            this.netAmount = netAmount;
        }

        public Long getBookingId() { return bookingId; }
        public String getBookingCode() { return bookingCode; }
        public Date getEventDate() { return eventDate; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public BigDecimal getFeeAmount() { return feeAmount; }
        public BigDecimal getNetAmount() { return netAmount; }
    }

    // ✅ NEW: Incoming Transfer Row
    public static class IncomingTransferRow implements Serializable {
        private final long transferId;
        private final String transferCode;
        private final Date createdAt;
        private final String fromName;
        private final String fromAccountNumber;
        private final BigDecimal amount;
        private final String message;

        public IncomingTransferRow(long transferId, String transferCode, Date createdAt,
                                   String fromName, String fromAccountNumber,
                                   BigDecimal amount, String message) {
            this.transferId = transferId;
            this.transferCode = transferCode;
            this.createdAt = createdAt;
            this.fromName = fromName;
            this.fromAccountNumber = fromAccountNumber;
            this.amount = amount;
            this.message = message;
        }

        public long getTransferId() { return transferId; }
        public String getTransferCode() { return transferCode; }
        public Date getCreatedAt() { return createdAt; }
        public String getFromName() { return fromName; }
        public String getFromAccountNumber() { return fromAccountNumber; }
        public BigDecimal getAmount() { return amount; }
        public String getMessage() { return message; }
    }

    @PostConstruct
    public void init() {
        currentUser = resolveLoggedInUser();
        if (currentUser == null) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Not logged in",
                    "You need to log in to your Manager account to view your wallet.");
            return;
        }

        restaurant = resolveRestaurantFromUser(currentUser);
        if (restaurant == null || restaurant.getRestaurantId() == null) {
            addMsg(FacesMessage.SEVERITY_ERROR, "No restaurant found.",
                    "This account has not yet been assigned as a manager for any restaurant.");
            return;
        }

        // ✅ vẫn refresh ở lần đầu create view (logic y như bạn)
        refresh();
    }

    public void refresh() {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            conn.setAutoCommit(false);

            // 1) ensure wallet + virtual account for currentUser
            ensureWalletExists(conn, currentUser.getUserId());
            ensureVirtualAccountExists(conn, currentUser.getUserId(), currentUser.getFullName());

            // 2) sync income from eligible bookings => Wallets.CurrentBalance (NET)
            lastSynced = syncIncomeFromBookings(conn);

            conn.commit();

        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (Exception ignore) {}
            addMsg(FacesMessage.SEVERITY_ERROR, "Refresh error", safeMsg(e));
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception ignore) {}
        }

        // 3) reload view (read-only)
        try (Connection c2 = ds.getConnection()) {
            wallet  = loadWalletByUserId(c2, currentUser.getUserId());
            account = loadVirtualAccountByUserId(c2, currentUser.getUserId());
            loadEligibleBookingsForView(c2);

            // ✅ NEW: load incoming transfers + notify if new
            loadIncomingTransfersForView(c2);
            notifyIfHasNewIncomingTransfer();

        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Load error", safeMsg(e));
        }
    }

    // =========================================================
    // A) VIEW: Load eligible bookings + compute gross/fee/net
    // =========================================================
    private void loadEligibleBookingsForView(Connection conn) throws Exception {
        rows.clear();
        grossTotal = BigDecimal.ZERO;

        String sql =
                "SELECT b.BookingId, b.BookingCode, b.EventDate, b.TotalAmount, b.CustomerId " +
                "FROM dbo.Bookings b " +
                "WHERE b.RestaurantId = ? AND b.PaymentStatus = N'PAID' AND b.BookingStatus = N'COMPLETED' " +
                "ORDER BY b.BookingId DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, restaurant.getRestaurantId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long bookingId = rs.getLong(1);
                    String code = rs.getString(2);
                    Date eventDate = rs.getTimestamp(3);
                    BigDecimal total = nz(rs.getBigDecimal(4));

                    BigDecimal fee = money(total.multiply(FEE_RATE));
                    BigDecimal net = money(total.subtract(fee));

                    grossTotal = grossTotal.add(money(total));
                    rows.add(new BookingRow(bookingId, code, eventDate, money(total), fee, net));
                }
            }
        }

        grossTotal = money(grossTotal);
        feeTotal = money(grossTotal.multiply(FEE_RATE));
        netTotal = money(grossTotal.subtract(feeTotal));
    }

    // =========================================================
    // ✅ NEW) VIEW: Incoming transfers to this user wallet
    // =========================================================
    private void loadIncomingTransfersForView(Connection conn) throws Exception {
        incomingTransfers.clear();

        String sql =
                "SELECT TOP 20 bt.TransferId, bt.TransferCode, bt.CreatedAt, " +
                "       u.FullName AS FromName, va.AccountNumber AS FromAccount, " +
                "       bt.Amount, bt.[Message] " +
                "FROM dbo.BankTransfers bt " +
                "JOIN dbo.Users u ON u.UserId = bt.FromUserId " +
                "LEFT JOIN dbo.VirtualAccounts va ON va.UserId = bt.FromUserId " +
                "WHERE bt.ToUserId = ? AND bt.[Status] = N'SUCCESS' " +
                "ORDER BY bt.TransferId DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, currentUser.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long transferId = rs.getLong(1);
                    String transferCode = rs.getString(2);
                    Date createdAt = rs.getTimestamp(3);
                    String fromName = rs.getString(4);
                    String fromAcc = rs.getString(5);
                    BigDecimal amount = money(nz(rs.getBigDecimal(6)));
                    String msg = rs.getString(7);

                    incomingTransfers.add(new IncomingTransferRow(
                            transferId,
                            transferCode,
                            createdAt,
                            (fromName == null ? "—" : fromName),
                            (fromAcc == null ? "—" : fromAcc),
                            amount,
                            (msg == null ? "" : msg)
                    ));
                }
            }
        }
    }

    // ✅ NEW: chỉ thông báo khi có incoming transfer mới
    private void notifyIfHasNewIncomingTransfer() {
        if (incomingTransfers.isEmpty()) return;

        long newestId = incomingTransfers.get(0).getTransferId();

        // lần đầu vào trang: set mốc, không spam
        if (lastSeenIncomingTransferId == 0) {
            lastSeenIncomingTransferId = newestId;
            return;
        }

        if (newestId > lastSeenIncomingTransferId) {
            IncomingTransferRow t = incomingTransfers.get(0);
            addMsg(FacesMessage.SEVERITY_INFO,
                    "New incoming transfer",
                    (t.getFromName() + " sent " + t.getAmount() + " (Code: " + t.getTransferCode() + ")"));
            lastSeenIncomingTransferId = newestId;
        }
    }

    // =========================================================
    // B) SYNC: credit NET income to Wallets once per booking
    //    TransferCode: INC-BKG-{BookingId}
    // =========================================================
    private int syncIncomeFromBookings(Connection conn) throws Exception {
        int synced = 0;

        String sql =
                "SELECT TOP 500 b.BookingId, b.BookingCode, b.TotalAmount, b.CustomerId " +
                "FROM dbo.Bookings b " +
                "WHERE b.RestaurantId = ? AND b.PaymentStatus = N'PAID' AND b.BookingStatus = N'COMPLETED' " +
                "ORDER BY b.BookingId DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, restaurant.getRestaurantId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long bookingId = rs.getLong(1);
                    String bookingCode = rs.getString(2);
                    BigDecimal total = nz(rs.getBigDecimal(3));
                    long customerId = rs.getLong(4);

                    if (total.compareTo(BigDecimal.ZERO) <= 0) continue;

                    BigDecimal net = money(total.multiply(NET_RATE));
                    if (net.compareTo(BigDecimal.ZERO) <= 0) continue;

                    String code = "INC-BKG-" + bookingId;

                    if (existsTransferCodeLocked(conn, code)) continue;

                    Long transferId = insertBankTransferSmart(
                            conn,
                            code,
                            customerId,
                            currentUser.getUserId(),
                            accountNumberOrNull(conn, currentUser.getUserId()),
                            net,
                            money(BigDecimal.ZERO),
                            "BOOKING_NET|BookingId=" + bookingId + "|BookingCode=" + nn(bookingCode),
                            "SUCCESS"
                    );

                    WalletInfo remindWallet = loadWalletByUserId(conn, currentUser.getUserId());
                    insertWalletTxnSmart(conn, remindWallet.getWalletId(), transferId, "CREDIT", net);

                    updateWalletBalance(conn, currentUser.getUserId(), net, true);

                    synced++;
                }
            }
        }

        return synced;
    }

    private boolean existsTransferCodeLocked(Connection conn, String transferCode) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT TOP 1 TransferId FROM dbo.BankTransfers WITH (UPDLOCK, HOLDLOCK) WHERE TransferCode = ?")) {
            ps.setString(1, transferCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================
    // C) TRANSFER
    // =========================================================
    public void doTransfer() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null) return;

        if (currentUser == null || currentUser.getUserId() == null) {
            addMsg(FacesMessage.SEVERITY_ERROR, "No user", "You need to log in.");
            return;
        }

        String toAcc = (toAccountNumber == null ? "" : toAccountNumber.trim());
        if (toAcc.isEmpty()) {
            fc.addMessage("transferForm:toAcc",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Receiver account required.", null));
            return;
        }

        if (transferAmount == null || transferAmount.compareTo(new BigDecimal("0.01")) < 0) {
            fc.addMessage("transferForm:amt",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Amount must be > 0.", null));
            return;
        }

        BigDecimal amt = money(transferAmount);
        String msg = (transferMessage == null ? null : transferMessage.trim());

        Connection conn = null;
        try {
            conn = ds.getConnection();
            conn.setAutoCommit(false);

            ensureWalletExists(conn, currentUser.getUserId());
            ensureVirtualAccountExists(conn, currentUser.getUserId(), currentUser.getFullName());

            Long toUserId = findUserIdByAccountNumber(conn, toAcc);
            if (toUserId == null) {
                conn.rollback();
                fc.addMessage("transferForm:toAcc",
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Account not found/ACTIVE.", null));
                return;
            }

            if (toUserId.equals(currentUser.getUserId())) {
                conn.rollback();
                fc.addMessage("transferForm:toAcc",
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Cannot transfer to yourself.", null));
                return;
            }

            ensureWalletExists(conn, toUserId);

            int ok = debitIfEnough(conn, currentUser.getUserId(), amt);
            if (ok == 0) {
                conn.rollback();
                fc.addMessage("transferForm:amt",
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Insufficient balance.", null));
                return;
            }

            updateWalletBalance(conn, toUserId, amt, true);

            String transferCode = generateTransferCode();

            Long transferId = insertBankTransferSmart(
                    conn,
                    transferCode,
                    currentUser.getUserId(),
                    toUserId,
                    toAcc,
                    amt,
                    money(BigDecimal.ZERO),
                    msg,
                    "SUCCESS"
            );

            WalletInfo wFrom = loadWalletByUserId(conn, currentUser.getUserId());
            WalletInfo wTo   = loadWalletByUserId(conn, toUserId);

            insertWalletTxnSmart(conn, wFrom.getWalletId(), transferId, "DEBIT", amt);
            insertWalletTxnSmart(conn, wTo.getWalletId(), transferId, "CREDIT", amt);

            conn.commit();

            toAccountNumber = "";
            transferAmount = null;
            transferMessage = "";

            addMsg(FacesMessage.SEVERITY_INFO, "Transfer success", "TransferCode: " + transferCode);

        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (Exception ignore) {}
            addMsg(FacesMessage.SEVERITY_ERROR, "Transfer failed", safeMsg(e));
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception ignore) {}
        }

        refresh();
    }

    private int debitIfEnough(Connection conn, Long userId, BigDecimal amt) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE dbo.Wallets SET CurrentBalance = CurrentBalance - ?, UpdatedAt = SYSDATETIME() " +
                "WHERE UserId = ? AND CurrentBalance >= ?")) {
            ps.setBigDecimal(1, amt);
            ps.setLong(2, userId);
            ps.setBigDecimal(3, amt);
            return ps.executeUpdate();
        }
    }

    // =========================================================
    // D) Ensure Wallet / Virtual Account
    // =========================================================
    private void ensureWalletExists(Connection conn, Long userId) throws Exception {
        WalletInfo w = loadWalletByUserId(conn, userId);
        if (w != null) return;

        String sql =
                "INSERT INTO dbo.Wallets (UserId, CurrentBalance, [Status], CreatedAt) " +
                "VALUES (?, 0, N'ACTIVE', SYSDATETIME())";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    private WalletInfo loadWalletByUserId(Connection conn, Long userId) throws Exception {
        String sql = "SELECT TOP 1 WalletId, CurrentBalance, [Status] FROM dbo.Wallets WHERE UserId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                WalletInfo w = new WalletInfo();
                w.setWalletId(rs.getLong(1));
                w.setBalance(money(nz(rs.getBigDecimal(2))));
                w.setStatus(rs.getString(3));
                return w;
            }
        }
    }

    private void updateWalletBalance(Connection conn, Long userId, BigDecimal amount, boolean plus) throws Exception {
        String sql = "UPDATE dbo.Wallets SET CurrentBalance = CurrentBalance " + (plus ? "+" : "-") +
                " ?, UpdatedAt = SYSDATETIME() WHERE UserId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    private void ensureVirtualAccountExists(Connection conn, Long userId, String fullName) throws Exception {
        VirtualAccountInfo va = loadVirtualAccountByUserId(conn, userId);
        if (va != null) return;

        String acc = generateRestaurantAccountNumber(userId);
        String display = (fullName == null || fullName.trim().isEmpty()) ? ("USER#" + userId) : fullName.trim();

        String sql =
                "INSERT INTO dbo.VirtualAccounts (UserId, BankCode, AccountNumber, DisplayName, [Status], CreatedAt) " +
                "VALUES (?, N'FEASTBANK', ?, ?, N'ACTIVE', SYSDATETIME())";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, acc);
            ps.setString(3, display);
            ps.executeUpdate();
        }
    }

    private VirtualAccountInfo loadVirtualAccountByUserId(Connection conn, Long userId) throws Exception {
        String sql = "SELECT TOP 1 AccountId, BankCode, AccountNumber, DisplayName, [Status] FROM dbo.VirtualAccounts WHERE UserId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                VirtualAccountInfo a = new VirtualAccountInfo();
                a.setAccountId(rs.getLong(1));
                a.setBankCode(rs.getString(2));
                a.setAccountNumber(rs.getString(3));
                a.setDisplayName(rs.getString(4));
                a.setStatus(rs.getString(5));
                return a;
            }
        }
    }

    private Long findUserIdByAccountNumber(Connection conn, String acc) throws Exception {
        String sql = "SELECT TOP 1 UserId, [Status] FROM dbo.VirtualAccounts WHERE AccountNumber = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, acc);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                long uid = rs.getLong(1);
                String st = rs.getString(2);
                if (st != null && !st.equalsIgnoreCase("ACTIVE")) return null;
                return uid;
            }
        }
    }

    private String accountNumberOrNull(Connection conn, Long userId) {
        try {
            VirtualAccountInfo a = loadVirtualAccountByUserId(conn, userId);
            return a == null ? null : a.getAccountNumber();
        } catch (Exception e) {
            return null;
        }
    }

    // =========================================================
    // E) Insert BankTransfers / WalletTransactions
    // =========================================================
    private Long insertBankTransferSmart(Connection conn,
                                         String transferCode,
                                         Long fromUserId,
                                         Long toUserId,
                                         String toAccountNumber,
                                         BigDecimal amount,
                                         BigDecimal feeAmount,
                                         String message,
                                         String status) throws Exception {

        String sql =
                "INSERT INTO dbo.BankTransfers (TransferCode, FromUserId, ToUserId, ToAccountNumber, Amount, FeeAmount, [Message], [Status], FailReason, CreatedAt, ConfirmedAt, CompletedAt) " +
                "OUTPUT INSERTED.TransferId " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, SYSDATETIME(), SYSDATETIME(), SYSDATETIME())";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transferCode);
            ps.setLong(2, fromUserId);
            ps.setLong(3, toUserId);
            ps.setString(4, toAccountNumber);
            ps.setBigDecimal(5, amount);
            ps.setBigDecimal(6, feeAmount);
            ps.setString(7, message);
            ps.setString(8, status);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private void insertWalletTxnSmart(Connection conn,
                                     Long walletId,
                                     Long transferId,
                                     String direction,
                                     BigDecimal amount) throws Exception {

        String sql =
                "INSERT INTO dbo.WalletTransactions (WalletId, TransferId, Direction, Amount, CreatedAt) " +
                "VALUES (?, ?, ?, ?, SYSDATETIME())";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, walletId);
            if (transferId == null) ps.setNull(2, Types.BIGINT);
            else ps.setLong(2, transferId);
            ps.setString(3, direction);
            ps.setBigDecimal(4, amount);
            ps.executeUpdate();
        }
    }

    // =========================================================
    // F) Resolve logged-in + restaurant mapping
    // =========================================================
    private Users resolveLoggedInUser() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null) return null;

        Map<String, Object> session = fc.getExternalContext().getSessionMap();
        if (session == null) return null;

        Object u = firstNonNull(
                session.get("user"),
                session.get("currentUser"),
                session.get("loggedInUser"),
                session.get("loginUser"),
                session.get("authUser")
        );

        return (u instanceof Users) ? (Users) u : null;
    }

    private Restaurants resolveRestaurantFromUser(Users u) {
        try {
            Collection<RestaurantManagers> rms = u.getRestaurantManagersCollection();
            if (rms == null || rms.isEmpty()) return null;

            for (RestaurantManagers rm : rms) {
                if (rm == null) continue;
                Restaurants r = rm.getRestaurantId();
                if (r != null && r.getRestaurantId() != null) return r;
            }
            return null;
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Mapping error", safeMsg(e));
            return null;
        }
    }

    private Object firstNonNull(Object... vals) {
        if (vals == null) return null;
        for (Object v : vals) if (v != null) return v;
        return null;
    }

    // =========================================================
    // utils
    // =========================================================
    private BigDecimal nz(BigDecimal x) { return x == null ? BigDecimal.ZERO : x; }

    private BigDecimal money(BigDecimal x) {
        if (x == null) x = BigDecimal.ZERO;
        return x.setScale(2, RoundingMode.HALF_UP);
    }

    private String nn(String s) { return s == null ? "" : s; }

    private void addMsg(FacesMessage.Severity sev, String summary, String detail) {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null) return;
        fc.addMessage(null, new FacesMessage(sev, summary, detail));
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

    private String generateRestaurantAccountNumber(Long userId) {
        long base = 1_000_000_000L + (userId == null ? 0L : userId);
        return "FB" + base;
    }

    // =========================================================
    // getters/setters
    // =========================================================
    public Users getCurrentUser() { return currentUser; }
    public Restaurants getRestaurant() { return restaurant; }

    public WalletInfo getWallet() { return wallet; }
    public VirtualAccountInfo getAccount() { return account; }

    public BigDecimal getGrossTotal() { return grossTotal; }
    public BigDecimal getFeeTotal() { return feeTotal; }
    public BigDecimal getNetTotal() { return netTotal; }

    public int getLastSynced() { return lastSynced; }

    public List<BookingRow> getRows() { return rows; }

    // ✅ NEW getter
    public List<IncomingTransferRow> getIncomingTransfers() { return incomingTransfers; }

    public String getToAccountNumber() { return toAccountNumber; }
    public void setToAccountNumber(String toAccountNumber) { this.toAccountNumber = toAccountNumber; }

    public BigDecimal getTransferAmount() { return transferAmount; }
    public void setTransferAmount(BigDecimal transferAmount) { this.transferAmount = transferAmount; }

    public String getTransferMessage() { return transferMessage; }
    public void setTransferMessage(String transferMessage) { this.transferMessage = transferMessage; }
}
