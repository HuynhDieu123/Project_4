/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.BookingStatusHistory;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface BookingStatusHistoryFacadeLocal {

    void create(BookingStatusHistory bookingStatusHistory);

    void edit(BookingStatusHistory bookingStatusHistory);

    void remove(BookingStatusHistory bookingStatusHistory);

    BookingStatusHistory find(Object id);

    List<BookingStatusHistory> findAll();

    List<BookingStatusHistory> findRange(int[] range);

    int count();
    
}
