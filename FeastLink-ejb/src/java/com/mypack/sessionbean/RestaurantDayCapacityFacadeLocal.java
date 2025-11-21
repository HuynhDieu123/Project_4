/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.RestaurantDayCapacity;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface RestaurantDayCapacityFacadeLocal {

    void create(RestaurantDayCapacity restaurantDayCapacity);

    void edit(RestaurantDayCapacity restaurantDayCapacity);

    void remove(RestaurantDayCapacity restaurantDayCapacity);

    RestaurantDayCapacity find(Object id);

    List<RestaurantDayCapacity> findAll();

    List<RestaurantDayCapacity> findRange(int[] range);

    int count();
    
}
