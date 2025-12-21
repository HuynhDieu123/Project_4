/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.VirtualAccounts;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface VirtualAccountsFacadeLocal {

    void create(VirtualAccounts virtualAccounts);

    void edit(VirtualAccounts virtualAccounts);

    void remove(VirtualAccounts virtualAccounts);

    VirtualAccounts find(Object id);

    List<VirtualAccounts> findAll();

    List<VirtualAccounts> findRange(int[] range);

    int count();
    
}
