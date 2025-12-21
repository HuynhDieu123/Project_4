/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.WalletTransactions;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface WalletTransactionsFacadeLocal {

    void create(WalletTransactions walletTransactions);

    void edit(WalletTransactions walletTransactions);

    void remove(WalletTransactions walletTransactions);

    WalletTransactions find(Object id);

    List<WalletTransactions> findAll();

    List<WalletTransactions> findRange(int[] range);

    int count();
    
}
