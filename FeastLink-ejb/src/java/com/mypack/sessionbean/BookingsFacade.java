/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.Bookings;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Stateless
public class BookingsFacade extends AbstractFacade<Bookings> implements BookingsFacadeLocal {

    @PersistenceContext(unitName = "FeastLink-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public BookingsFacade() {
        super(Bookings.class);
    }

    // Tổng booking
    @Override
    public long countAllBookings() {
        return em.createQuery("SELECT COUNT(b) FROM Bookings b", Long.class)
                .getSingleResult();
    }

    // Doanh thu tháng hiện tại
    @Override
    public double calculateMonthlyRevenue() {
        String sql = "SELECT SUM(b.totalAmount) FROM Bookings b"; // JPQL hoặc native tùy bạn

        Query q = em.createQuery(sql);  // hoặc createNativeQuery
        Object raw = q.getSingleResult();

        if (raw == null) {
            return 0d;
        }

        Number num = (Number) raw;      // BigDecimal cũng là Number
        return num.doubleValue();       // chuyển về double
    }

    // Tỷ lệ hủy
    @Override
    public double calculateCancelRate() {

        long cancelled = em.createQuery(
                "SELECT COUNT(b) FROM Bookings b WHERE b.bookingStatus = 'CANCELLED'",
                Long.class).getSingleResult();

        long total = countAllBookings();

        if (total == 0) {
            return 0;
        }

        return ((double) cancelled / total) * 100;
    }

    // Chờ duyệt
    @Override
    public long countPendingApprovals() {
        return em.createQuery(
                "SELECT COUNT(b) FROM Bookings b WHERE b.bookingStatus = 'PENDING'",
                Long.class).getSingleResult();
    }

    // Booking gần nhất (3 cái)
    @Override
    public List<Bookings> findRecentBookings() {
        return em.createQuery(
                "SELECT b FROM Bookings b ORDER BY b.createdAt DESC", Bookings.class)
                .setMaxResults(3)
                .getResultList();
    }

    @Override
    public double getCancelRate() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public long countByEventType(Integer eventTypeId) {
        if (eventTypeId == null) {
            return 0L;
        }
        Long count = em.createQuery(
                "SELECT COUNT(b) FROM Bookings b WHERE b.eventTypeId.eventTypeId = :eventTypeId",
                Long.class)
                .setParameter("eventTypeId", eventTypeId)
                .getSingleResult();
        return (count != null) ? count : 0L;
    }

    public List<Bookings> findCompletedBookingsForReview(Long restaurantId, Long customerId) {
        // Chỉ lấy booking COMPLETED và chưa có review (isDeleted=false) cho booking đó
        return em.createQuery(
                "SELECT b FROM Bookings b "
                + "WHERE b.restaurantId.restaurantId = :rid "
                + "AND b.customerId.userId = :cid "
                + "AND UPPER(b.bookingStatus) = 'COMPLETED' "
                + "AND NOT EXISTS ("
                + "   SELECT rr FROM RestaurantReviews rr "
                + "   WHERE rr.bookingId = b AND rr.customerId.userId = :cid AND rr.isDeleted = false"
                + ") "
                + "ORDER BY b.eventDate DESC",
                Bookings.class
        ).setParameter("rid", restaurantId)
                .setParameter("cid", customerId)
                .getResultList();
    }

}
