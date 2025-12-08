/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.PointWallets;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface PointWalletsFacadeLocal {

    void create(PointWallets pointWallets);

    void edit(PointWallets pointWallets);

    void remove(PointWallets pointWallets);

    PointWallets find(Object id);

    List<PointWallets> findAll();

    List<PointWallets> findRange(int[] range);

    int count();
    
}
