/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.BookingCombos;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface BookingCombosFacadeLocal {

    void create(BookingCombos bookingCombos);

    void edit(BookingCombos bookingCombos);

    void remove(BookingCombos bookingCombos);

    BookingCombos find(Object id);

    List<BookingCombos> findAll();

    List<BookingCombos> findRange(int[] range);

    int count();
    
}
