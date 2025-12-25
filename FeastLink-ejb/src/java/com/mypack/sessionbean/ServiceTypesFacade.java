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
    public List<ServiceTypes> findAll() {
        return em.createQuery("SELECT s FROM ServiceTypes s", ServiceTypes.class).getResultList();
    }

    @Override
    public List<ServiceTypes> findAllOrderByName() {
        return em.createQuery("SELECT s FROM ServiceTypes s ORDER BY s.name", ServiceTypes.class)
                .getResultList();
    }

    @Override
    public List<ServiceTypes> searchByName(String keyword) {
        return em.createQuery(
                "SELECT s FROM ServiceTypes s WHERE LOWER(s.name) LIKE :kw ORDER BY s.name",
                ServiceTypes.class
        ).setParameter("kw", "%" + keyword.toLowerCase() + "%")
                .getResultList();
    }

    @Override
    public boolean existsByName(String name) {
        Long c = em.createQuery(
                "SELECT COUNT(s) FROM ServiceTypes s WHERE LOWER(s.name) = :n", Long.class
        ).setParameter("n", name.toLowerCase())
                .getSingleResult();
        return c != null && c > 0;
    }

    @Override
    public boolean existsByNameExceptId(String name, Integer excludeId) {
        Long c = em.createQuery(
                "SELECT COUNT(s) FROM ServiceTypes s WHERE LOWER(s.name) = :n AND s.serviceTypeId <> :id",
                Long.class
        ).setParameter("n", name.toLowerCase())
                .setParameter("id", excludeId)
                .getSingleResult();
        return c != null && c > 0;
    }

    @Override
    public ServiceTypes findByNameIgnoreCase(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        List<ServiceTypes> list = em.createQuery(
                "SELECT s FROM ServiceTypes s WHERE LOWER(TRIM(s.name)) = :n",
                ServiceTypes.class
        )
                .setParameter("n", name.trim().toLowerCase())
                .setMaxResults(1)
                .getResultList();

        return list.isEmpty() ? null : list.get(0);
    }

}
