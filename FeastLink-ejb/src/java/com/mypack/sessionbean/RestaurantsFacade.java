/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.Restaurants;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.TypedQuery;

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

    @Override
    public Restaurants findFresh(Long id) {
        if (id == null) {
            return null;
        }

        // clear persistence context để tránh dính entity đã managed cũ
        try {
            em.clear();
        } catch (Exception ignore) {
        }

        // evict cache entry (L2)
        try {
            em.getEntityManagerFactory().getCache().evict(Restaurants.class, id);
        } catch (Exception ignore) {
        }

        try {
            TypedQuery<Restaurants> q = em.createQuery(
                    "SELECT r FROM Restaurants r WHERE r.restaurantId = :id", Restaurants.class
            );
            q.setParameter("id", id);

            // Bypass / refresh cache
            try {
                q.setHint("jakarta.persistence.cache.retrieveMode", CacheRetrieveMode.BYPASS);
                q.setHint("jakarta.persistence.cache.storeMode", CacheStoreMode.REFRESH);
            } catch (Exception ignore) {
            }

            // EclipseLink-friendly (GlassFish hay dùng EclipseLink)
            try {
                q.setHint("eclipselink.refresh", "true");
            } catch (Exception ignore) {
            }

            Restaurants r = q.getSingleResult();

            // Force reload entity state + force load combos collection
            try {
                em.refresh(r);
            } catch (Exception ignore) {
            }
            try {
                r.getMenuCombosCollection().size();
            } catch (Exception ignore) {
            }

            return r;
        } catch (Exception e) {
            // fallback
            return em.find(Restaurants.class, id);
        }
    }

}
