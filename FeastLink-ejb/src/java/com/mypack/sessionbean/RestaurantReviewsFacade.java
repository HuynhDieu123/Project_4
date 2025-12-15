/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.RestaurantReviews;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import jakarta.persistence.TypedQuery;

/**
 *
 * @author Laptop
 */
@Stateless
public class RestaurantReviewsFacade extends AbstractFacade<RestaurantReviews> implements RestaurantReviewsFacadeLocal {

    @PersistenceContext(unitName = "FeastLink-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public RestaurantReviewsFacade() {
        super(RestaurantReviews.class);
    }

    public List<RestaurantReviews> findApprovedByRestaurant(Long restaurantId, int offset, int limit, String sortKey) {
        String orderBy = "rr.createdAt DESC"; // default recent
        if ("highest".equalsIgnoreCase(sortKey)) {
            orderBy = "rr.rating DESC, rr.createdAt DESC";
        } else if ("lowest".equalsIgnoreCase(sortKey)) {
            orderBy = "rr.rating ASC, rr.createdAt DESC";
        }

        String jpql
                = "SELECT rr FROM RestaurantReviews rr "
                + "JOIN FETCH rr.customerId c "
                + "WHERE rr.restaurantId.restaurantId = :rid "
                + "AND rr.isDeleted = false AND rr.isApproved = true "
                + "ORDER BY " + orderBy;

        TypedQuery<RestaurantReviews> q = em.createQuery(jpql, RestaurantReviews.class);
        q.setParameter("rid", restaurantId);
        q.setFirstResult(Math.max(0, offset));
        q.setMaxResults(Math.max(1, limit));
        return q.getResultList();
    }

    public long countApprovedByRestaurant(Long restaurantId) {
        Long n = em.createQuery(
                "SELECT COUNT(rr) FROM RestaurantReviews rr "
                + "WHERE rr.restaurantId.restaurantId = :rid "
                + "AND rr.isDeleted = false AND rr.isApproved = true",
                Long.class
        ).setParameter("rid", restaurantId)
                .getSingleResult();
        return (n == null) ? 0 : n;
    }

    public Double avgApprovedRating(Long restaurantId) {
        Double avg = em.createQuery(
                "SELECT AVG(rr.rating) FROM RestaurantReviews rr "
                + "WHERE rr.restaurantId.restaurantId = :rid "
                + "AND rr.isDeleted = false AND rr.isApproved = true",
                Double.class
        ).setParameter("rid", restaurantId)
                .getSingleResult();
        return avg; // có thể null nếu chưa có review
    }

    public List<Object[]> ratingBreakdownApproved(Long restaurantId) {
        return em.createQuery(
                "SELECT rr.rating, COUNT(rr) FROM RestaurantReviews rr "
                + "WHERE rr.restaurantId.restaurantId = :rid "
                + "AND rr.isDeleted = false AND rr.isApproved = true "
                + "GROUP BY rr.rating",
                Object[].class
        ).setParameter("rid", restaurantId)
                .getResultList();
    }

    public List<RestaurantReviews> findByRestaurantAndCustomer(Long restaurantId, Long customerId) {
        return em.createQuery(
                "SELECT rr FROM RestaurantReviews rr "
                + "JOIN FETCH rr.customerId c "
                + "WHERE rr.restaurantId.restaurantId = :rid "
                + "AND rr.customerId.userId = :cid "
                + "AND rr.isDeleted = false "
                + "ORDER BY rr.createdAt DESC",
                RestaurantReviews.class
        ).setParameter("rid", restaurantId)
                .setParameter("cid", customerId)
                .getResultList();
    }

}
