/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.ServiceTypes;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Stateless
public class ServiceTypesFacade extends AbstractFacade<ServiceTypes> implements ServiceTypesFacadeLocal {

    @PersistenceContext(unitName = "FeastLink-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ServiceTypesFacade() {
        super(ServiceTypes.class);
    }
    
    @Override
    public List<ServiceTypes> findByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        return em.createQuery(
                "SELECT s FROM ServiceTypes s " +
                "WHERE LOWER(s.name) LIKE :kw", ServiceTypes.class)
                 .setParameter("kw", "%" + keyword.toLowerCase() + "%")
                 .getResultList();
    }
    
        @Override
    public boolean existsByName(String name) {
        Long count = em.createQuery(
                "SELECT COUNT(s) FROM ServiceTypes s " +
                "WHERE LOWER(s.name) = :name", Long.class)
                .setParameter("name", name.toLowerCase().trim())
                .getSingleResult();
        return count > 0;
    }

    @Override
    public boolean existsByNameExceptId(String name, Integer id) {
        Long count = em.createQuery(
                "SELECT COUNT(s) FROM ServiceTypes s " +
                "WHERE LOWER(s.name) = :name AND s.serviceTypeId <> :id", Long.class)
                .setParameter("name", name.toLowerCase().trim())
                .setParameter("id", id)
                .getSingleResult();
        return count > 0;
    }
    
}
