/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.Cities;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 *
 * @author Laptop
 */
@Stateless
public class CitiesFacade extends AbstractFacade<Cities> implements CitiesFacadeLocal {

    @PersistenceContext(unitName = "FeastLink-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public CitiesFacade() {
        super(Cities.class);
    }
    
}
