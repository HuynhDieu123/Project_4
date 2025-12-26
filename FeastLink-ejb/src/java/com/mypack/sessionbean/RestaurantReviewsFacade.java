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
import java.util.HashMap;
import java.util.Map;

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
    @Override
    public Map<Long, Object[]> statsByRestaurantIds(List<Long> restaurantIds, int badThreshold) {
        Map<Long, Object[]> map = new HashMap<>();
        if (restaurantIds == null || restaurantIds.isEmpty()) return map;

        List<Object[]> rows = em.createQuery(
                "SELECT rr.restaurantId.restaurantId, " +
                "       AVG(rr.rating), " +
                "       COUNT(rr.reviewId), " +
                "       SUM(CASE WHEN rr.rating <= :bad THEN 1 ELSE 0 END) " +
                "FROM RestaurantReviews rr " +
                "WHERE rr.isApproved = true AND rr.isDeleted = false " +
                "  AND rr.restaurantId.restaurantId IN :ids " +
                "GROUP BY rr.restaurantId.restaurantId", Object[].class)
            .setParameter("bad", badThreshold)
            .setParameter("ids", restaurantIds)
            .getResultList();

        for (Object[] r : rows) {
            Long rid = (Long) r[0];
            Object avg = r[1];
            Object total = r[2];
            Object bad = r[3];
            map.put(rid, new Object[]{avg, total, bad});
        }
        return map;
    }
        @Override
    public long countByRestaurant(Long restaurantId, Integer ratingFilter) {
        String jpql =
                "SELECT COUNT(rr) " +
                "FROM RestaurantReviews rr " +
                "WHERE rr.restaurantId.restaurantId = :rid " +
                "AND (:rating IS NULL OR rr.rating = :rating)";
        return em.createQuery(jpql, Long.class)
                .setParameter("rid", restaurantId)
                .setParameter("rating", (ratingFilter != null && ratingFilter > 0) ? ratingFilter : null)
                .getSingleResult();
    }

    @Override
    public List<RestaurantReviews> findByRestaurant(Long restaurantId,
                                                    boolean approvedOnly,
                                                    boolean includeDeleted,
                                                    int first,
                                                    int pageSize) {

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT rr FROM RestaurantReviews rr ")
            .append("JOIN FETCH rr.customerId c ")
            .append("LEFT JOIN FETCH rr.bookingId b ")
            .append("WHERE rr.restaurantId.restaurantId = :rid ");

        if (!includeDeleted) {
            jpql.append("AND rr.isDeleted = false ");
        }
        if (approvedOnly) {
            jpql.append("AND rr.isApproved = true ");
        }

        jpql.append("ORDER BY rr.createdAt DESC");

        TypedQuery<RestaurantReviews> q = em.createQuery(jpql.toString(), RestaurantReviews.class);
        q.setParameter("rid", restaurantId);
        q.setFirstResult(Math.max(0, first));
        q.setMaxResults(Math.max(1, pageSize));
        return q.getResultList();
    }

    @Override
    public long countByRestaurant(Long restaurantId,
                                  boolean approvedOnly,
                                  boolean includeDeleted) {

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT COUNT(rr) FROM RestaurantReviews rr ")
            .append("WHERE rr.restaurantId.restaurantId = :rid ");

        if (!includeDeleted) {
            jpql.append("AND rr.isDeleted = false ");
        }
        if (approvedOnly) {
            jpql.append("AND rr.isApproved = true ");
        }

        return em.createQuery(jpql.toString(), Long.class)
                 .setParameter("rid", restaurantId)
                 .getSingleResult();
    }

}
