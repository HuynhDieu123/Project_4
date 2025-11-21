/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.MenuCombos;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface MenuCombosFacadeLocal {

    void create(MenuCombos menuCombos);

    void edit(MenuCombos menuCombos);

    void remove(MenuCombos menuCombos);

    MenuCombos find(Object id);

    List<MenuCombos> findAll();

    List<MenuCombos> findRange(int[] range);

    int count();
    
}
