/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.PointTransactions;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface PointTransactionsFacadeLocal {

    void create(PointTransactions pointTransactions);

    void edit(PointTransactions pointTransactions);

    void remove(PointTransactions pointTransactions);

    PointTransactions find(Object id);

    List<PointTransactions> findAll();

    List<PointTransactions> findRange(int[] range);

    int count();
    
}
