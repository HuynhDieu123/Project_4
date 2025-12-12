/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.Cuisines;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface CuisinesFacadeLocal {

    void create(Cuisines cuisines);

    void edit(Cuisines cuisines);

    void remove(Cuisines cuisines);

    Cuisines find(Object id);

    List<Cuisines> findAll();

    List<Cuisines> findRange(int[] range);
    
    List<Cuisines> searchByName(String keyword);
    int count();
    
}
