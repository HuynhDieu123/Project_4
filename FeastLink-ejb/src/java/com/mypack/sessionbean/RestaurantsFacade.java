/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.Restaurants;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 *
 * @author Laptop
 */
@Stateless
public class RestaurantsFacade extends AbstractFacade<Restaurants> implements RestaurantsFacadeLocal {

    @PersistenceContext(unitName = "FeastLink-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public RestaurantsFacade() {
        super(Restaurants.class);
    }
    
    @Override
    public long countRestaurants() {
        return (long) em.createQuery("SELECT COUNT(r) FROM Restaurants r").getSingleResult();
    }
    
    public boolean existsByEmail(String email) {
    Long count = em.createQuery(
            "SELECT COUNT(r) FROM Restaurants r WHERE r.email = :email", Long.class)
        .setParameter("email", email)
        .getSingleResult();
    return count != null && count > 0;
}


}
