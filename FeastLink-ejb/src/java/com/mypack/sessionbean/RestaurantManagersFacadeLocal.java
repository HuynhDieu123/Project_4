/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.RestaurantManagers;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface RestaurantManagersFacadeLocal {

    void create(RestaurantManagers restaurantManagers);

    void edit(RestaurantManagers restaurantManagers);

    void remove(RestaurantManagers restaurantManagers);

    RestaurantManagers find(Object id);

    List<RestaurantManagers> findAll();

    List<RestaurantManagers> findRange(int[] range);

    int count();
    
}
