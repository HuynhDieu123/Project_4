/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author Laptop
 */
@Entity
@Table(name = "Feedbacks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Feedbacks.findAll", query = "SELECT f FROM Feedbacks f"),
    @NamedQuery(name = "Feedbacks.findByFeedbackId", query = "SELECT f FROM Feedbacks f WHERE f.feedbackId = :feedbackId"),
    @NamedQuery(name = "Feedbacks.findByTitle", query = "SELECT f FROM Feedbacks f WHERE f.title = :title"),
    @NamedQuery(name = "Feedbacks.findByDescription", query = "SELECT f FROM Feedbacks f WHERE f.description = :description"),
    @NamedQuery(name = "Feedbacks.findByStatus", query = "SELECT f FROM Feedbacks f WHERE f.status = :status"),
    @NamedQuery(name = "Feedbacks.findByResolutionNote", query = "SELECT f FROM Feedbacks f WHERE f.resolutionNote = :resolutionNote"),
    @NamedQuery(name = "Feedbacks.findByCreatedAt", query = "SELECT f FROM Feedbacks f WHERE f.createdAt = :createdAt"),
    @NamedQuery(name = "Feedbacks.findByUpdatedAt", query = "SELECT f FROM Feedbacks f WHERE f.updatedAt = :updatedAt"),
    @NamedQuery(name = "Feedbacks.findByResolvedAt", query = "SELECT f FROM Feedbacks f WHERE f.resolvedAt = :resolvedAt")})
public class Feedbacks implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "FeedbackId")
    private Long feedbackId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 200)
    @Column(name = "Title")
    private String title;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 2147483647)
    @Column(name = "Description")
    private String description;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "Status")
    private String status;
    @Size(max = 2147483647)
    @Column(name = "ResolutionNote")
    private String resolutionNote;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Column(name = "UpdatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    @Column(name = "ResolvedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date resolvedAt;
    @JoinColumn(name = "BookingId", referencedColumnName = "BookingId")
    @ManyToOne
    private Bookings bookingId;
    @JoinColumn(name = "RestaurantId", referencedColumnName = "RestaurantId")
    @ManyToOne
    private Restaurants restaurantId;
    @JoinColumn(name = "ReporterUserId", referencedColumnName = "UserId")
    @ManyToOne(optional = false)
    private Users reporterUserId;
    @JoinColumn(name = "ReportedUserId", referencedColumnName = "UserId")
    @ManyToOne
    private Users reportedUserId;
    @JoinColumn(name = "AdminHandlerId", referencedColumnName = "UserId")
    @ManyToOne
    private Users adminHandlerId;

    public Feedbacks() {
    }

    public Feedbacks(Long feedbackId) {
        this.feedbackId = feedbackId;
    }

    public Feedbacks(Long feedbackId, String title, String description, String status, Date createdAt) {
        this.feedbackId = feedbackId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(Long feedbackId) {
        this.feedbackId = feedbackId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Date resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Bookings getBookingId() {
        return bookingId;
    }

    public void setBookingId(Bookings bookingId) {
        this.bookingId = bookingId;
    }

    public Restaurants getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Restaurants restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Users getReporterUserId() {
        return reporterUserId;
    }

    public void setReporterUserId(Users reporterUserId) {
        this.reporterUserId = reporterUserId;
    }

    public Users getReportedUserId() {
        return reportedUserId;
    }

    public void setReportedUserId(Users reportedUserId) {
        this.reportedUserId = reportedUserId;
    }

    public Users getAdminHandlerId() {
        return adminHandlerId;
    }

    public void setAdminHandlerId(Users adminHandlerId) {
        this.adminHandlerId = adminHandlerId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (feedbackId != null ? feedbackId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Feedbacks)) {
            return false;
        }
        Feedbacks other = (Feedbacks) object;
        if ((this.feedbackId == null && other.feedbackId != null) || (this.feedbackId != null && !this.feedbackId.equals(other.feedbackId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.Feedbacks[ feedbackId=" + feedbackId + " ]";
    }
    
}
