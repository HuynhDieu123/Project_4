/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.BookingMenuItems;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface BookingMenuItemsFacadeLocal {
    
    List<BookingMenuItems> findByBookingId(Long bookingId);

    void create(BookingMenuItems bookingMenuItems);

    void edit(BookingMenuItems bookingMenuItems);

    void remove(BookingMenuItems bookingMenuItems);

    BookingMenuItems find(Object id);

    List<BookingMenuItems> findAll();

    List<BookingMenuItems> findRange(int[] range);

    int count();
    
}
