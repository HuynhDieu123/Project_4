package com.mypack.sessionbean;

import com.mypack.entity.Bookings;
import jakarta.ejb.Local;
import java.util.Date;
import java.util.List;

@Local
public interface BookingsFacadeLocal {

    void create(Bookings bookings);

    void edit(Bookings bookings);

    void remove(Bookings bookings);

    Bookings find(Object id);

    List<Bookings> findAll();

    List<Bookings> findRange(int[] range);

    int count();

    // ===== Dashboard/Admin =====
    long countAllBookings();

    double calculateMonthlyRevenue();

    long countPendingApprovals();

    double calculateCancelRate();

    // ⚠️ trước bạn để throw ở Facade, giờ nên implement lại cho an toàn
    double getCancelRate();

    List<Bookings> findRecentBookings();

    long countByEventType(Integer eventTypeId);

    List<Bookings> findCompletedBookingsForReview(Long restaurantId, Long customerId);

    // ✅ NEW: gom booking theo ngày để vẽ chấm trên calendar
    // rows: [eventDate(Date), totalGuests(Number), bookingCount(Long)]
    List<Object[]> aggregateForCalendar(Long restaurantId, Date fromInclusive, Date toExclusive);

    // ✅ NEW: Fix MyBookings bị thiếu package/menu do cache + lazy collection
    List<Bookings> findByCustomerIdWithDetails(Long customerId);

    // ✅ NEW: (optional) evict cache để khỏi dính dữ liệu cũ sau create
    void evictBookingsCache();
}
