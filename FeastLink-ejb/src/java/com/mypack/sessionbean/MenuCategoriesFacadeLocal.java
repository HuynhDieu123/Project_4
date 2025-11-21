/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.MenuCategories;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface MenuCategoriesFacadeLocal {

    void create(MenuCategories menuCategories);

    void edit(MenuCategories menuCategories);

    void remove(MenuCategories menuCategories);

    MenuCategories find(Object id);

    List<MenuCategories> findAll();

    List<MenuCategories> findRange(int[] range);

    int count();
    
}
