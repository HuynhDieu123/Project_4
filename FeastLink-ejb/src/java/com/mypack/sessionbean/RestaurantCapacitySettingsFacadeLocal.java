/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.RestaurantCapacitySettings;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface RestaurantCapacitySettingsFacadeLocal {

    void create(RestaurantCapacitySettings restaurantCapacitySettings);

    void edit(RestaurantCapacitySettings restaurantCapacitySettings);

    void remove(RestaurantCapacitySettings restaurantCapacitySettings);

    RestaurantCapacitySettings find(Object id);

    List<RestaurantCapacitySettings> findAll();

    List<RestaurantCapacitySettings> findRange(int[] range);

    int count();
    
}
