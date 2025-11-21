/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.Cities;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface CitiesFacadeLocal {

    void create(Cities cities);

    void edit(Cities cities);

    void remove(Cities cities);

    Cities find(Object id);

    List<Cities> findAll();

    List<Cities> findRange(int[] range);

    int count();
    
}
