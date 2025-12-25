/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.Cuisines;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Stateless
public class CuisinesFacade extends AbstractFacade<Cuisines> implements CuisinesFacadeLocal {

    @PersistenceContext(unitName = "FeastLink-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public CuisinesFacade() {
        super(Cuisines.class);
    }
    

    @Override
    public List<Cuisines> searchByName(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }

        String jpql = "SELECT c FROM Cuisines c WHERE LOWER(c.name) LIKE :kw ORDER BY c.name";
        TypedQuery<Cuisines> q = em.createQuery(jpql, Cuisines.class);
        q.setParameter("kw", "%" + keyword.trim().toLowerCase() + "%");
        return q.getResultList();
    }
    @Override
public boolean existsByName(String name, Integer excludeId) {
    if (name == null) return false;

    String n = name.trim().toLowerCase();

    String jpql = "SELECT COUNT(c) FROM Cuisines c " +
                  "WHERE LOWER(TRIM(c.name)) = :n";

    if (excludeId != null) {
        jpql += " AND c.cuisineId <> :id";
    }

    var q = getEntityManager().createQuery(jpql, Long.class);
    q.setParameter("n", n);

    if (excludeId != null) {
        q.setParameter("id", excludeId);
    }

    Long count = q.getSingleResult();
    return count != null && count > 0;
}


}
