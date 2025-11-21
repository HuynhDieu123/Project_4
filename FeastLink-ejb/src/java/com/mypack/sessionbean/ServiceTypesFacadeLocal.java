/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.ServiceTypes;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface ServiceTypesFacadeLocal {

    void create(ServiceTypes serviceTypes);

    void edit(ServiceTypes serviceTypes);

    void remove(ServiceTypes serviceTypes);

    ServiceTypes find(Object id);

    List<ServiceTypes> findAll();

    List<ServiceTypes> findRange(int[] range);

    int count();
    
}
