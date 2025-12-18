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

    long countAllBookings();

    double calculateMonthlyRevenue();

    double getCancelRate();

    long countPendingApprovals();

    double calculateCancelRate();

    List<Bookings> findRecentBookings();

    int count();

    long countByEventType(Integer eventTypeId);

    List<Bookings> findCompletedBookingsForReview(Long restaurantId, Long customerId);

    // ✅ NEW: gom booking theo ngày để vẽ chấm trên calendar
    // rows: [eventDate(Date), totalGuests(Number), bookingCount(Long)]
    List<Object[]> aggregateForCalendar(Long restaurantId, Date fromInclusive, Date toExclusive);
}
