/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypack.sessionbean;

import com.mypack.entity.UserVouchers;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author tuanc
 */
@Local
public interface UserVouchersFacadeLocal {

    void create(UserVouchers userVouchers);

    void edit(UserVouchers userVouchers);

    void remove(UserVouchers userVouchers);

    UserVouchers find(Object id);

    List<UserVouchers> findAll();

    List<UserVouchers> findRange(int[] range);

    int count();
    
}
