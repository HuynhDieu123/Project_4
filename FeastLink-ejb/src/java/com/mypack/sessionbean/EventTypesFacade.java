/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.EventTypes;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Stateless
public class EventTypesFacade extends AbstractFacade<EventTypes> implements EventTypesFacadeLocal {

    @PersistenceContext(unitName = "FeastLink-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public EventTypesFacade() {
        super(EventTypes.class);
    }
    @Override
    public EventTypes createByName(String name) {
        String n = normalize(name);

        if (n.isEmpty()) {
            throw new IllegalArgumentException("Event type name is required.");
        }
        if (existsByName(n)) {
            throw new IllegalArgumentException("Event type name already exists.");
        }

        EventTypes e = new EventTypes();
        e.setName(n);
        em.persist(e);
        em.flush(); // để có ID ngay nếu cần
        return e;
    }

    @Override
    public EventTypes updateName(Integer id, String name) {
        if (id == null) throw new IllegalArgumentException("Id is required.");

        EventTypes e = em.find(EventTypes.class, id);
        if (e == null) throw new IllegalArgumentException("Event type not found.");

        String n = normalize(name);
        if (n.isEmpty()) {
            throw new IllegalArgumentException("Event type name is required.");
        }
        if (existsByName(n, id)) {
            throw new IllegalArgumentException("Event type name already exists.");
        }

        e.setName(n);
        e = em.merge(e);
        return e;
    }

    @Override
    public void deleteById(Integer id) {
        if (id == null) throw new IllegalArgumentException("Id is required.");
        EventTypes e = em.find(EventTypes.class, id);
        if (e != null) {
            em.remove(e);
        }
    }

    @Override
    public boolean existsByName(String name) {
        String n = normalize(name);
        if (n.isEmpty()) return false;

        Long count = em.createQuery(
                "SELECT COUNT(e) FROM EventTypes e WHERE LOWER(e.name) = LOWER(:name)",
                Long.class
        ).setParameter("name", n)
         .getSingleResult();

        return count != null && count > 0;
    }

    @Override
    public boolean existsByName(String name, Integer excludeId) {
        String n = normalize(name);
        if (n.isEmpty()) return false;

        Long count = em.createQuery(
                "SELECT COUNT(e) FROM EventTypes e " +
                "WHERE LOWER(e.name) = LOWER(:name) AND e.eventTypeId <> :excludeId",
                Long.class
        ).setParameter("name", n)
         .setParameter("excludeId", excludeId == null ? -1 : excludeId)
         .getSingleResult();

        return count != null && count > 0;
    }

    @Override
    public List<EventTypes> search(String keyword) {
        String kw = (keyword == null) ? "" : keyword.trim();
        if (kw.isEmpty()) return findAll();

        return em.createNamedQuery("EventTypes.searchByKeyword", EventTypes.class)
                 .setParameter("kw", "%" + kw + "%")
                 .getResultList();
    }

    @Override
    public EventTypes findByNameIgnoreCase(String name) {
        String n = normalize(name);
        if (n.isEmpty()) return null;

        try {
            return em.createNamedQuery("EventTypes.findByNameIgnoreCase", EventTypes.class)
                     .setParameter("name", n)
                     .getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    private String normalize(String s) {
        return (s == null) ? "" : s.trim().replaceAll("\\s+", " ");
    }
}
