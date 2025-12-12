/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.BookingMenuItems;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;    

/**
 *
 * @author Laptop
 */
@Stateless
public class BookingMenuItemsFacade extends AbstractFacade<BookingMenuItems> implements BookingMenuItemsFacadeLocal {

    @PersistenceContext(unitName = "FeastLink-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public BookingMenuItemsFacade() {
        super(BookingMenuItems.class);
    }
    
    @Override
    public List<BookingMenuItems> findByBookingId(Long bookingId) {
        return em.createNamedQuery("BookingMenuItems.findByBookingId", BookingMenuItems.class)
                 .setParameter("bookingId", bookingId)
                 .getResultList();
    }
    
}
