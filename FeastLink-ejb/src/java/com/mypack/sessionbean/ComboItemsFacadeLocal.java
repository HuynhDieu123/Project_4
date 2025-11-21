/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.ComboItems;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface ComboItemsFacadeLocal {

    void create(ComboItems comboItems);

    void edit(ComboItems comboItems);

    void remove(ComboItems comboItems);

    ComboItems find(Object id);

    List<ComboItems> findAll();

    List<ComboItems> findRange(int[] range);

    int count();
    
}
