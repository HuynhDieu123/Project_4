/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.Invoices;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface InvoicesFacadeLocal {

    void create(Invoices invoices);

    void edit(Invoices invoices);

    void remove(Invoices invoices);

    Invoices find(Object id);

    List<Invoices> findAll();

    List<Invoices> findRange(int[] range);

    int count();
    
}
