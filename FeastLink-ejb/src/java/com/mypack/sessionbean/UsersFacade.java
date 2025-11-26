/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.Users;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Stateless
public class UsersFacade extends AbstractFacade<Users> implements UsersFacadeLocal {

    @PersistenceContext(unitName = "FeastLink-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public UsersFacade() {
        super(Users.class);
    }
    
    @Override
    public long countUsers() {
        return (long) em.createQuery("SELECT COUNT(u) FROM Users u").getSingleResult();
    }
    @Override
    public int countFiltered(String keyword, String role, String status) {
        String sql = "SELECT COUNT(u) FROM Users u WHERE 1=1 ";

        if (!keyword.isEmpty()) sql += "AND (u.fullName LIKE :kw OR u.email LIKE :kw) ";
        if (!"ALL".equals(role)) sql += "AND u.role = :role ";
        if (!"ALL".equals(status)) sql += "AND u.status = :status ";

        var q = em.createQuery(sql, Long.class);

        if (!keyword.isEmpty()) q.setParameter("kw", "%" + keyword + "%");
        if (!"ALL".equals(role)) q.setParameter("role", role);
        if (!"ALL".equals(status)) q.setParameter("status", status);

        return q.getSingleResult().intValue();
    }


    @Override
    public List<Users> findFiltered(String keyword, String role,
                                   String status, int start, int size) {

        String sql = "SELECT u FROM Users u WHERE 1=1 ";

        if (!keyword.isEmpty()) sql += "AND (u.fullName LIKE :kw OR u.email LIKE :kw) ";
        if (!"ALL".equals(role)) sql += "AND u.role = :role ";
        if (!"ALL".equals(status)) sql += "AND u.status = :status ";

        sql += "ORDER BY u.createdAt DESC";

        var q = em.createQuery(sql, Users.class);

        if (!keyword.isEmpty()) q.setParameter("kw", "%" + keyword + "%");
        if (!"ALL".equals(role)) q.setParameter("role", role);
        if (!"ALL".equals(status)) q.setParameter("status", status);

        q.setFirstResult(start);
        q.setMaxResults(size);

        return q.getResultList();
    }
    @Override
public Users findByEmail(String email) {
    try {
        return em.createQuery("SELECT u FROM Users u WHERE u.email = :email", Users.class)
                 .setParameter("email", email)
                 .getSingleResult();
    } catch (Exception e) {
        return null;
    }
}

}
