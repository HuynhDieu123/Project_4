/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.RestaurantImages;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface RestaurantImagesFacadeLocal {

    void create(RestaurantImages restaurantImages);

    void edit(RestaurantImages restaurantImages);

    void remove(RestaurantImages restaurantImages);

    RestaurantImages find(Object id);

    List<RestaurantImages> findAll();

    List<RestaurantImages> findRange(int[] range);

    int count();
    
}
