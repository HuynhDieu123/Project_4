/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
package com.restaurant.bean;

import com.mypack.entity.Users;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;

@Named("authBean")
@SessionScoped
public class AuthBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private Users currentUser;

    /** Lấy currentUser từ session map (đã set lúc login) */
    public Users getCurrentUser() {
        if (currentUser == null) {
            Object obj = FacesContext.getCurrentInstance()
                    .getExternalContext()
                    .getSessionMap()
                    .get("currentUser");
            if (obj instanceof Users) {
                currentUser = (Users) obj;
            }
        }
        return currentUser;
    }

    public String getDisplayName() {
        Users u = getCurrentUser();
        if (u != null && u.getFullName() != null && !u.getFullName().isBlank()) {
            return u.getFullName();
        }
        return "Manager";
    }

    public String getDisplayRole() {
        Users u = getCurrentUser();
        if (u != null && u.getRole() != null) {
            return u.getRole();
        }
        return "";
    }

    /** Logout: clear session và điều hướng về login.xhtml */
    public String logout() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        ctx.getExternalContext().invalidateSession();
        // login.xhtml ở root: /faces/login.xhtml
        return "/login?faces-redirect=true";
    }
}

