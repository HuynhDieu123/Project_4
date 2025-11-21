/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.EventTypes;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface EventTypesFacadeLocal {

    void create(EventTypes eventTypes);

    void edit(EventTypes eventTypes);

    void remove(EventTypes eventTypes);

    EventTypes find(Object id);

    List<EventTypes> findAll();

    List<EventTypes> findRange(int[] range);

    int count();
    
}
