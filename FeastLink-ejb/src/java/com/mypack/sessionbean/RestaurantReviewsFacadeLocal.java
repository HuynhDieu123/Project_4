/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.RestaurantReviews;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface RestaurantReviewsFacadeLocal {

    void create(RestaurantReviews restaurantReviews);

    void edit(RestaurantReviews restaurantReviews);

    void remove(RestaurantReviews restaurantReviews);

    RestaurantReviews find(Object id);

    List<RestaurantReviews> findAll();

    List<RestaurantReviews> findRange(int[] range);

    int count();
    
}
