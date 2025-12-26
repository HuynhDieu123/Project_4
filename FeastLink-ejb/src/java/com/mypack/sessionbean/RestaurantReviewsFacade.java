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

    // 1) AVG + TOTAL
    List<Object[]> rows = em.createQuery(
        "SELECT rr.restaurantId.restaurantId, AVG(rr.rating), COUNT(rr.reviewId) " +
        "FROM RestaurantReviews rr " +
        "WHERE rr.isDeleted = false AND rr.restaurantId.restaurantId IN :ids " +
        "GROUP BY rr.restaurantId.restaurantId",
        Object[].class
    ).setParameter("ids", restaurantIds)
     .getResultList();

    for (Object[] r : rows) {
        Long rid = ((Number) r[0]).longValue();
        Double avg = (r[1] == null) ? 0.0 : ((Number) r[1]).doubleValue();  // ✅ số thực
        Long total = (r[2] == null) ? 0L : ((Number) r[2]).longValue();
        map.put(rid, new Object[]{avg, total, 0L});
    }

    // 2) BAD COUNT
    List<Object[]> badRows = em.createQuery(
        "SELECT rr.restaurantId.restaurantId, COUNT(rr.reviewId) " +
        "FROM RestaurantReviews rr " +
        "WHERE rr.isDeleted = false AND rr.rating <= :bad AND rr.restaurantId.restaurantId IN :ids " +
        "GROUP BY rr.restaurantId.restaurantId",
        Object[].class
    ).setParameter("bad", badThreshold)
     .setParameter("ids", restaurantIds)
     .getResultList();

    for (Object[] r : badRows) {
        Long rid = ((Number) r[0]).longValue();
        Long bad = (r[1] == null) ? 0L : ((Number) r[1]).longValue();
        Object[] cur = map.getOrDefault(rid, new Object[]{0.0, 0L, 0L});
        cur[2] = bad;
        map.put(rid, cur);
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

    @Override
    public long countPendingByRestaurant(Long restaurantId) {
        if (restaurantId == null) {
            return 0;
        }
        Long n = em.createQuery(
                "SELECT COUNT(rr) FROM RestaurantReviews rr "
                + "WHERE rr.restaurantId.restaurantId = :rid "
                + "AND rr.isDeleted = false AND rr.isApproved = false",
                Long.class
        ).setParameter("rid", restaurantId).getSingleResult();
        return (n == null) ? 0 : n;
    }

    @Override
    public long countForRestaurant(Long restaurantId, Boolean approved, Integer rating, String keyword) {
        if (restaurantId == null) {
            return 0;
        }

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT COUNT(rr) FROM RestaurantReviews rr ")
                .append("LEFT JOIN rr.customerId c ")
                .append("LEFT JOIN rr.bookingId b ")
                .append("WHERE rr.restaurantId.restaurantId = :rid ")
                .append("AND rr.isDeleted = false ");

        if (approved != null) {
            jpql.append("AND rr.isApproved = :approved ");
        }
        if (rating != null && rating >= 1 && rating <= 5) {
            jpql.append("AND rr.rating = :rating ");
        }

        boolean hasKw = keyword != null && !keyword.trim().isEmpty();
        if (hasKw) {
            jpql.append("AND (LOWER(rr.comment) LIKE :kw ")
                    .append("OR LOWER(c.fullName) LIKE :kw ")
                    .append("OR LOWER(c.email) LIKE :kw ")
                    .append("OR LOWER(b.bookingCode) LIKE :kw) ");
        }

        TypedQuery<Long> q = em.createQuery(jpql.toString(), Long.class);
        q.setParameter("rid", restaurantId);

        if (approved != null) {
            q.setParameter("approved", approved);
        }
        if (rating != null && rating >= 1 && rating <= 5) {
            q.setParameter("rating", rating);
        }
        if (hasKw) {
            q.setParameter("kw", "%" + keyword.trim().toLowerCase() + "%");
        }

        Long n = q.getSingleResult();
        return (n == null) ? 0 : n;
    }

    @Override
    public List<RestaurantReviews> findForRestaurant(Long restaurantId, Boolean approved, Integer rating, String keyword,
            int offset, int limit, String sortKey) {
        if (restaurantId == null) {
            return List.of();
        }

        String orderBy = "rr.createdAt DESC";
        if ("oldest".equalsIgnoreCase(sortKey)) {
            orderBy = "rr.createdAt ASC";
        } else if ("highest".equalsIgnoreCase(sortKey)) {
            orderBy = "rr.rating DESC, rr.createdAt DESC";
        } else if ("lowest".equalsIgnoreCase(sortKey)) {
            orderBy = "rr.rating ASC, rr.createdAt DESC";
        }

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT rr FROM RestaurantReviews rr ")
                .append("JOIN FETCH rr.customerId c ")
                .append("JOIN FETCH rr.bookingId b ")
                .append("WHERE rr.restaurantId.restaurantId = :rid ")
                .append("AND rr.isDeleted = false ");

        if (approved != null) {
            jpql.append("AND rr.isApproved = :approved ");
        }
        if (rating != null && rating >= 1 && rating <= 5) {
            jpql.append("AND rr.rating = :rating ");
        }

        boolean hasKw = keyword != null && !keyword.trim().isEmpty();
        if (hasKw) {
            jpql.append("AND (LOWER(rr.comment) LIKE :kw ")
                    .append("OR LOWER(c.fullName) LIKE :kw ")
                    .append("OR LOWER(c.email) LIKE :kw ")
                    .append("OR LOWER(b.bookingCode) LIKE :kw) ");
        }

        jpql.append("ORDER BY ").append(orderBy);

        TypedQuery<RestaurantReviews> q = em.createQuery(jpql.toString(), RestaurantReviews.class);
        q.setParameter("rid", restaurantId);

        if (approved != null) {
            q.setParameter("approved", approved);
        }
        if (rating != null && rating >= 1 && rating <= 5) {
            q.setParameter("rating", rating);
        }
        if (hasKw) {
            q.setParameter("kw", "%" + keyword.trim().toLowerCase() + "%");
        }

        q.setFirstResult(Math.max(0, offset));
        q.setMaxResults(Math.max(1, limit));
        return q.getResultList();
    }

    @Override
    public long countForRestaurant(Long restaurantId, Boolean approved, Integer rating, String keyword,
            java.util.Date dateFrom, java.util.Date dateTo) {
        if (restaurantId == null) {
            return 0;
        }

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT COUNT(rr) FROM RestaurantReviews rr ")
                .append("LEFT JOIN rr.customerId c ")
                .append("LEFT JOIN rr.bookingId b ")
                .append("WHERE rr.restaurantId.restaurantId = :rid ")
                .append("AND rr.isDeleted = false ");

        if (approved != null) {
            jpql.append("AND rr.isApproved = :approved ");
        }
        if (rating != null && rating >= 1 && rating <= 5) {
            jpql.append("AND rr.rating = :rating ");
        }

        if (dateFrom != null) {
            jpql.append("AND rr.createdAt >= :fromDt ");
        }
        if (dateTo != null) {
            jpql.append("AND rr.createdAt <= :toDt ");
        }

        boolean hasKw = keyword != null && !keyword.trim().isEmpty();
        if (hasKw) {
            jpql.append("AND (LOWER(rr.comment) LIKE :kw ")
                    .append("OR LOWER(c.fullName) LIKE :kw ")
                    .append("OR LOWER(c.email) LIKE :kw ")
                    .append("OR LOWER(b.bookingCode) LIKE :kw) ");
        }

        var q = em.createQuery(jpql.toString(), Long.class);
        q.setParameter("rid", restaurantId);

        if (approved != null) {
            q.setParameter("approved", approved);
        }
        if (rating != null && rating >= 1 && rating <= 5) {
            q.setParameter("rating", rating);
        }
        if (dateFrom != null) {
            q.setParameter("fromDt", dateFrom);
        }
        if (dateTo != null) {
            q.setParameter("toDt", dateTo);
        }
        if (hasKw) {
            q.setParameter("kw", "%" + keyword.trim().toLowerCase() + "%");
        }

        Long n = q.getSingleResult();
        return (n == null) ? 0 : n;
    }

    @Override
    public List<RestaurantReviews> findForRestaurant(Long restaurantId, Boolean approved, Integer rating, String keyword,
            java.util.Date dateFrom, java.util.Date dateTo,
            int offset, int limit, String sortKey) {
        if (restaurantId == null) {
            return java.util.List.of();
        }

        String orderBy = "rr.createdAt DESC";
        if ("oldest".equalsIgnoreCase(sortKey)) {
            orderBy = "rr.createdAt ASC";
        } else if ("highest".equalsIgnoreCase(sortKey)) {
            orderBy = "rr.rating DESC, rr.createdAt DESC";
        } else if ("lowest".equalsIgnoreCase(sortKey)) {
            orderBy = "rr.rating ASC, rr.createdAt DESC";
        }

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT rr FROM RestaurantReviews rr ")
                .append("JOIN FETCH rr.customerId c ")
                .append("JOIN FETCH rr.bookingId b ")
                .append("WHERE rr.restaurantId.restaurantId = :rid ")
                .append("AND rr.isDeleted = false ");

        if (approved != null) {
            jpql.append("AND rr.isApproved = :approved ");
        }
        if (rating != null && rating >= 1 && rating <= 5) {
            jpql.append("AND rr.rating = :rating ");
        }

        if (dateFrom != null) {
            jpql.append("AND rr.createdAt >= :fromDt ");
        }
        if (dateTo != null) {
            jpql.append("AND rr.createdAt <= :toDt ");
        }

        boolean hasKw = keyword != null && !keyword.trim().isEmpty();
        if (hasKw) {
            jpql.append("AND (LOWER(rr.comment) LIKE :kw ")
                    .append("OR LOWER(c.fullName) LIKE :kw ")
                    .append("OR LOWER(c.email) LIKE :kw ")
                    .append("OR LOWER(b.bookingCode) LIKE :kw) ");
        }

        jpql.append("ORDER BY ").append(orderBy);

        var q = em.createQuery(jpql.toString(), RestaurantReviews.class);
        q.setParameter("rid", restaurantId);

        if (approved != null) {
            q.setParameter("approved", approved);
        }
        if (rating != null && rating >= 1 && rating <= 5) {
            q.setParameter("rating", rating);
        }
        if (dateFrom != null) {
            q.setParameter("fromDt", dateFrom);
        }
        if (dateTo != null) {
            q.setParameter("toDt", dateTo);
        }
        if (hasKw) {
            q.setParameter("kw", "%" + keyword.trim().toLowerCase() + "%");
        }

        q.setFirstResult(Math.max(0, offset));
        q.setMaxResults(Math.max(1, limit));
        return q.getResultList();
    }

    @Override
    public Double avgRatingForRestaurant(Long restaurantId, java.util.Date dateFrom, java.util.Date dateTo) {
        if (restaurantId == null) {
            return 0.0;
        }

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT AVG(rr.rating) FROM RestaurantReviews rr ")
                .append("WHERE rr.restaurantId.restaurantId = :rid ")
                .append("AND rr.isDeleted = false ");

        if (dateFrom != null) {
            jpql.append("AND rr.createdAt >= :fromDt ");
        }
        if (dateTo != null) {
            jpql.append("AND rr.createdAt <= :toDt ");
        }

        var q = em.createQuery(jpql.toString(), Double.class);
        q.setParameter("rid", restaurantId);
        if (dateFrom != null) {
            q.setParameter("fromDt", dateFrom);
        }
        if (dateTo != null) {
            q.setParameter("toDt", dateTo);
        }

        return q.getSingleResult();
    }

    @Override
    public List<Object[]> ratingBreakdownForRestaurant(Long restaurantId, java.util.Date dateFrom, java.util.Date dateTo) {
        if (restaurantId == null) {
            return java.util.List.of();
        }

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT rr.rating, COUNT(rr) FROM RestaurantReviews rr ")
                .append("WHERE rr.restaurantId.restaurantId = :rid ")
                .append("AND rr.isDeleted = false ");

        if (dateFrom != null) {
            jpql.append("AND rr.createdAt >= :fromDt ");
        }
        if (dateTo != null) {
            jpql.append("AND rr.createdAt <= :toDt ");
        }

        jpql.append("GROUP BY rr.rating ORDER BY rr.rating DESC");

        var q = em.createQuery(jpql.toString(), Object[].class);
        q.setParameter("rid", restaurantId);
        if (dateFrom != null) {
            q.setParameter("fromDt", dateFrom);
        }
        if (dateTo != null) {
            q.setParameter("toDt", dateTo);
        }

        return q.getResultList();
    }

}
