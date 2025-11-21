/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.MenuItemImages;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface MenuItemImagesFacadeLocal {

    void create(MenuItemImages menuItemImages);

    void edit(MenuItemImages menuItemImages);

    void remove(MenuItemImages menuItemImages);

    MenuItemImages find(Object id);

    List<MenuItemImages> findAll();

    List<MenuItemImages> findRange(int[] range);

    int count();
    
}
