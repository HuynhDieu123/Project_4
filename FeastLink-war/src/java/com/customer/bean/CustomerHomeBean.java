package com.customer.bean;

import com.mypack.entity.Restaurants;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("customerHomeBean")
@RequestScoped
public class CustomerHomeBean implements Serializable {

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    // Danh sách nhà hàng show ở trang index
    private List<Restaurants> topRestaurants;

    @PostConstruct
    public void init() {
        // Lấy toàn bộ restaurants từ DB
        List<Restaurants> all = restaurantsFacade.findAll();
        if (all == null) {
            topRestaurants = new ArrayList<>();
            return;
        }

        // Chỉ lấy tối đa 6 nhà hàng để show ở trang chủ
        if (all.size() > 6) {
            topRestaurants = all.subList(0, 6);
        } else {
            topRestaurants = all;
        }
    }

    public List<Restaurants> getTopRestaurants() {
        return topRestaurants;
    }
}
