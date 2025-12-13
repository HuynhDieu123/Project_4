package com.mypack.sessionbean;

import com.mypack.entity.Payments;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

@Stateless
public class PaymentsFacade extends AbstractFacade<Payments> implements PaymentsFacadeLocal {

    @PersistenceContext(unitName = "FeastLink-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PaymentsFacade() {
        super(Payments.class);
    }

    @Override
    public Payments findByTransactionCode(String transactionCode) {
        try {
            return em.createNamedQuery("Payments.findByTransactionCode", Payments.class)
                    .setParameter("transactionCode", transactionCode)
                    .getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }
}
