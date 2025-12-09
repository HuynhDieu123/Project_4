/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.RestaurantImages;
import com.mypack.entity.Restaurants;   // <<== THÊM IMPORT NÀY
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 *
 * @author Laptop
 */
@Stateless
public class RestaurantImagesFacade extends AbstractFacade<RestaurantImages>
                                   implements RestaurantImagesFacadeLocal {

    @PersistenceContext(unitName = "FeastLink-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public RestaurantImagesFacade() {
        super(RestaurantImages.class);
    }

    // ================== OVERRIDE THÊM PHẦN CACHE ==================

    @Override
    public void create(RestaurantImages restaurantImages) {
        super.create(restaurantImages);
        em.flush(); // đẩy xuống DB ngay

        // Xóa cache RestaurantImages
        em.getEntityManagerFactory()
          .getCache()
          .evict(RestaurantImages.class);

        // Xóa cache Restaurants cho nhà hàng tương ứng
        if (restaurantImages != null
                && restaurantImages.getRestaurantId() != null
                && restaurantImages.getRestaurantId().getRestaurantId() != null) {
            em.getEntityManagerFactory()
              .getCache()
              .evict(Restaurants.class,
                     restaurantImages.getRestaurantId().getRestaurantId());
        }
    }

    @Override
    public void edit(RestaurantImages restaurantImages) {
        super.edit(restaurantImages);
        em.flush();

        em.getEntityManagerFactory()
          .getCache()
          .evict(RestaurantImages.class);

        if (restaurantImages != null
                && restaurantImages.getRestaurantId() != null
                && restaurantImages.getRestaurantId().getRestaurantId() != null) {
            em.getEntityManagerFactory()
              .getCache()
              .evict(Restaurants.class,
                     restaurantImages.getRestaurantId().getRestaurantId());
        }
    }

    @Override
    public void remove(RestaurantImages restaurantImages) {
        if (restaurantImages == null) {
            return;
        }

        // Đảm bảo entity managed
        RestaurantImages managed = em.contains(restaurantImages)
                ? restaurantImages
                : em.merge(restaurantImages);

        Restaurants restaurant = managed.getRestaurantId();

        // Xóa DB
        em.remove(managed);
        em.flush();

        // Xóa cache RestaurantImages
        em.getEntityManagerFactory()
          .getCache()
          .evict(RestaurantImages.class);

        // Xóa cache Restaurants cho đúng nhà hàng
        if (restaurant != null && restaurant.getRestaurantId() != null) {
            em.getEntityManagerFactory()
              .getCache()
              .evict(Restaurants.class, restaurant.getRestaurantId());
        }
    }
}
