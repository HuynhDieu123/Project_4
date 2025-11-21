/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.Messages;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Local
public interface MessagesFacadeLocal {

    void create(Messages messages);

    void edit(Messages messages);

    void remove(Messages messages);

    Messages find(Object id);

    List<Messages> findAll();

    List<Messages> findRange(int[] range);

    int count();
    
}
