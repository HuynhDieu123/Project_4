package com.restaurant.bean;

import com.mypack.entity.EventTypes;
import com.mypack.sessionbean.EventTypesFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;

@Named("eventTypesPublicBean")
@ViewScoped
public class EventTypesPublicBean implements Serializable {

    @EJB
    private EventTypesFacadeLocal eventTypesFacade;

    private List<EventTypes> eventTypes;

    @PostConstruct
    public void init() {
        // Lấy tất cả loại tiệc từ DB
        eventTypes = eventTypesFacade.findAll();
    }

    public List<EventTypes> getEventTypes() {
        return eventTypes;
    }
}
