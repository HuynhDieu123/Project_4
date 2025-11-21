/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.FavoriteRestaurants;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface FavoriteRestaurantsFacadeLocal {

    void create(FavoriteRestaurants favoriteRestaurants);

    void edit(FavoriteRestaurants favoriteRestaurants);

    void remove(FavoriteRestaurants favoriteRestaurants);

    FavoriteRestaurants find(Object id);

    List<FavoriteRestaurants> findAll();

    List<FavoriteRestaurants> findRange(int[] range);

    int count();
    
}
