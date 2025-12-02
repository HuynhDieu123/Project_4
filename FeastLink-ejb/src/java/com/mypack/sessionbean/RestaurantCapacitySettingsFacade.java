/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.RestaurantCapacitySettings;
import com.mypack.entity.Restaurants;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

/**
 *
 * @author Laptop
 */
@Stateless
public class RestaurantCapacitySettingsFacade extends AbstractFacade<RestaurantCapacitySettings> implements RestaurantCapacitySettingsFacadeLocal {

    @PersistenceContext(unitName = "FeastLink-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public RestaurantCapacitySettingsFacade() {
        super(RestaurantCapacitySettings.class);
    }
    
    @Override
    public RestaurantCapacitySettings findByRestaurant(Restaurants restaurant) {
        try {
            return em.createQuery(
                            "SELECT r FROM RestaurantCapacitySettings r " +
                                    "WHERE r.restaurantId = :rest",
                            RestaurantCapacitySettings.class)
                    .setParameter("rest", restaurant)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }
    
}
