/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.Restaurants;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface RestaurantsFacadeLocal {

    void create(Restaurants restaurants);

    void edit(Restaurants restaurants);

    void remove(Restaurants restaurants);

    Restaurants find(Object id);
    
    Restaurants findFresh(Long id);

    List<Restaurants> findAll();

    List<Restaurants> findRange(int[] range);
    
    long countRestaurants();

    int count();
    public boolean existsByEmail(String email);
    
}
