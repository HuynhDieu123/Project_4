/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.Bookings;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
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
    public double calculateCancelRate();
    
    List<Bookings> findRecentBookings();

    int count();
    
    long countByEventType(Integer eventTypeId);

    
}
