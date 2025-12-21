/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.Wallets;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface WalletsFacadeLocal {

    void create(Wallets wallets);

    void edit(Wallets wallets);

    void remove(Wallets wallets);

    Wallets find(Object id);

    List<Wallets> findAll();

    List<Wallets> findRange(int[] range);

    int count();
    
}
