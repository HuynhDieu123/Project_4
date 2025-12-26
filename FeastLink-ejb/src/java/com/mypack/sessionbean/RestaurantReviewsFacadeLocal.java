/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.RestaurantReviews;
import jakarta.ejb.Local;
import java.util.List;
import java.util.Map;

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

    List<RestaurantReviews> findApprovedByRestaurant(Long restaurantId, int offset, int limit, String sortKey);

    long countApprovedByRestaurant(Long restaurantId);

    Double avgApprovedRating(Long restaurantId);

    List<Object[]> ratingBreakdownApproved(Long restaurantId); // each row: [rating, count]

    List<RestaurantReviews> findByRestaurantAndCustomer(Long restaurantId, Long customerId); // include pending (approved=false)
     Map<Long, Object[]> statsByRestaurantIds(List<Long> restaurantIds, int badThreshold);
     
    long countByRestaurant(Long restaurantId, Integer ratingFilter);

    List<RestaurantReviews> findByRestaurant(Long restaurantId,
                                            boolean approvedOnly,
                                            boolean includeDeleted,
                                            int first,
                                            int pageSize);

    // NEW: count total reviews for paging
    long countByRestaurant(Long restaurantId,
                           boolean approvedOnly,
                           boolean includeDeleted);

    long countPendingByRestaurant(Long restaurantId);

    long countForRestaurant(Long restaurantId, Boolean approved, Integer rating, String keyword);

    List<RestaurantReviews> findForRestaurant(Long restaurantId, Boolean approved, Integer rating, String keyword,
            int offset, int limit, String sortKey);

    long countForRestaurant(Long restaurantId, Boolean approved, Integer rating, String keyword,
            java.util.Date dateFrom, java.util.Date dateTo);

    List<RestaurantReviews> findForRestaurant(Long restaurantId, Boolean approved, Integer rating, String keyword,
            java.util.Date dateFrom, java.util.Date dateTo,
            int offset, int limit, String sortKey);

// Stats theo ngày
    Double avgRatingForRestaurant(Long restaurantId, java.util.Date dateFrom, java.util.Date dateTo);

    List<Object[]> ratingBreakdownForRestaurant(Long restaurantId, java.util.Date dateFrom, java.util.Date dateTo);

}
