package com.customer.bean;

import com.mypack.entity.Bookings;
import com.mypack.entity.Users;
import com.mypack.sessionbean.BookingsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import jakarta.faces.context.FacesContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Named("customerBookingsBean")
@RequestScoped
public class CustomerBookingsBean implements Serializable {

    @EJB
    private BookingsFacadeLocal bookingsFacade;

    private List<Bookings> myBookings;

    @PostConstruct
    public void init() {
        myBookings = new ArrayList<>();

        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return;
        }

        Map<String, Object> sessionMap = ctx.getExternalContext().getSessionMap();
        Object userObj = sessionMap.get("currentUser");

        // Nếu chưa login thì để danh sách rỗng
        if (!(userObj instanceof Users)) {
            return;
        }

        Users currentUser = (Users) userObj;
        Long currentUserId = currentUser.getUserId();
        if (currentUserId == null) {
            return;
        }

        // Lấy tất cả booking rồi filter theo CustomerId
        List<Bookings> all = bookingsFacade.findAll();
        if (all == null) {
            return;
        }

        for (Bookings b : all) {
            if (b == null || b.getCustomerId() == null) {
                continue;
            }
            if (currentUserId.equals(b.getCustomerId().getUserId())) {
                myBookings.add(b);
            }
        }

        // Sắp xếp: EventDate / CreatedAt DESC (mới nhất ở trên)
        Collections.sort(myBookings, new Comparator<Bookings>() {
            @Override
            public int compare(Bookings o1, Bookings o2) {
                Date d1 = o1 != null
                        ? (o1.getEventDate() != null ? o1.getEventDate() : o1.getCreatedAt())
                        : null;
                Date d2 = o2 != null
                        ? (o2.getEventDate() != null ? o2.getEventDate() : o2.getCreatedAt())
                        : null;

                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;

                // DESC
                return d2.compareTo(d1);
            }
        });
    }

    public List<Bookings> getMyBookings() {
        return myBookings;
    }
}
