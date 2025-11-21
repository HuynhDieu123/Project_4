/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.Sysdiagrams;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface SysdiagramsFacadeLocal {

    void create(Sysdiagrams sysdiagrams);

    void edit(Sysdiagrams sysdiagrams);

    void remove(Sysdiagrams sysdiagrams);

    Sysdiagrams find(Object id);

    List<Sysdiagrams> findAll();

    List<Sysdiagrams> findRange(int[] range);

    int count();
    
}
