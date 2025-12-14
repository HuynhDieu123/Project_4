/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.PointSettings;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author tuanc
 */
@Local
public interface PointSettingsFacadeLocal {

    void create(PointSettings pointSettings);

    void edit(PointSettings pointSettings);

    void remove(PointSettings pointSettings);

    PointSettings find(Object id);

    List<PointSettings> findAll();

    List<PointSettings> findRange(int[] range);

    int count();
    
}
