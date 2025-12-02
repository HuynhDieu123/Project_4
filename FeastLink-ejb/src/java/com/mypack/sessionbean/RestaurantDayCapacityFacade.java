/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.RestaurantDayCapacity;
import com.mypack.entity.Restaurants;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Stateless
public class RestaurantDayCapacityFacade extends AbstractFacade<RestaurantDayCapacity> implements RestaurantDayCapacityFacadeLocal {

    @PersistenceContext(unitName = "FeastLink-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public RestaurantDayCapacityFacade() {
        super(RestaurantDayCapacity.class);
    }
    
    @Override
    public List<RestaurantDayCapacity> findByRestaurantAndDateRange(
            Restaurants restaurant, Date start, Date end) {

        return em.createQuery(
                        "SELECT d FROM RestaurantDayCapacity d " +
                                "WHERE d.restaurantId = :rest " +
                                "AND d.eventDate BETWEEN :start AND :end",
                        RestaurantDayCapacity.class)
                .setParameter("rest", restaurant)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
    }

    @Override
    public RestaurantDayCapacity findByRestaurantAndDate(
            Restaurants restaurant, Date date) {

        try {
            return em.createQuery(
                            "SELECT d FROM RestaurantDayCapacity d " +
                                    "WHERE d.restaurantId = :rest " +
                                    "AND d.eventDate = :date " +
                                    "AND d.slotCode = 'ALLDAY'",
                            RestaurantDayCapacity.class)
                    .setParameter("rest", restaurant)
                    .setParameter("date", date)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }
    
}
