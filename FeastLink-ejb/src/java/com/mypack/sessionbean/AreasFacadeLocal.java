/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.Areas;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface AreasFacadeLocal {

    void create(Areas areas);

    void edit(Areas areas);

    void remove(Areas areas);

    Areas find(Object id);

    List<Areas> findAll();

    List<Areas> findRange(int[] range);

    int count();
    
}
