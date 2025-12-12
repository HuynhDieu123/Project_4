/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.BookingCombos;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;    
/**
 *
 * @author Laptop
 */
@Stateless
public class BookingCombosFacade extends AbstractFacade<BookingCombos> implements BookingCombosFacadeLocal {

    @PersistenceContext(unitName = "FeastLink-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public BookingCombosFacade() {
        super(BookingCombos.class);
    }
    
     @Override
    public List<BookingCombos> findByBookingId(Long bookingId) {
        return em.createNamedQuery("BookingCombos.findByBookingId", BookingCombos.class)
                 .setParameter("bookingId", bookingId)
                 .getResultList();
    }
    
}
