/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 *
 * @author Laptop
 */
@Embeddable
public class ComboItemsPK implements Serializable {

    @Basic(optional = false)
    @NotNull
    @Column(name = "ComboId")
    private long comboId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "MenuItemId")
    private long menuItemId;

    public ComboItemsPK() {
    }

    public ComboItemsPK(long comboId, long menuItemId) {
        this.comboId = comboId;
        this.menuItemId = menuItemId;
    }

    public long getComboId() {
        return comboId;
    }

    public void setComboId(long comboId) {
        this.comboId = comboId;
    }

    public long getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(long menuItemId) {
        this.menuItemId = menuItemId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) comboId;
        hash += (int) menuItemId;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ComboItemsPK)) {
            return false;
        }
        ComboItemsPK other = (ComboItemsPK) object;
        if (this.comboId != other.comboId) {
            return false;
        }
        if (this.menuItemId != other.menuItemId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.ComboItemsPK[ comboId=" + comboId + ", menuItemId=" + menuItemId + " ]";
    }
    
}
