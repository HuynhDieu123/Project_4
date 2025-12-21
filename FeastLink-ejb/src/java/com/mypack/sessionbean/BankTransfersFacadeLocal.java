/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.BankTransfers;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface BankTransfersFacadeLocal {

    void create(BankTransfers bankTransfers);

    void edit(BankTransfers bankTransfers);

    void remove(BankTransfers bankTransfers);

    BankTransfers find(Object id);

    List<BankTransfers> findAll();

    List<BankTransfers> findRange(int[] range);

    int count();
    
}
