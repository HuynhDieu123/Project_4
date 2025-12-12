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
}
