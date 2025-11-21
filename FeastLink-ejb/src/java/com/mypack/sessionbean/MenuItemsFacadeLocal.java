/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.MenuItems;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface MenuItemsFacadeLocal {

    void create(MenuItems menuItems);

    void edit(MenuItems menuItems);

    void remove(MenuItems menuItems);

    MenuItems find(Object id);

    List<MenuItems> findAll();

    List<MenuItems> findRange(int[] range);

    int count();
    
}
