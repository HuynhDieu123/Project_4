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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

/**
 *
 * @author Laptop
 */
@Entity
@Table(name = "Messages")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Messages.findAll", query = "SELECT m FROM Messages m"),
    @NamedQuery(name = "Messages.findByMessageId", query = "SELECT m FROM Messages m WHERE m.messageId = :messageId"),
    @NamedQuery(name = "Messages.findBySubject", query = "SELECT m FROM Messages m WHERE m.subject = :subject"),
    @NamedQuery(name = "Messages.findByContent", query = "SELECT m FROM Messages m WHERE m.content = :content"),
    @NamedQuery(name = "Messages.findByIsArchivedBySender", query = "SELECT m FROM Messages m WHERE m.isArchivedBySender = :isArchivedBySender"),
    @NamedQuery(name = "Messages.findByIsArchivedByReceiver", query = "SELECT m FROM Messages m WHERE m.isArchivedByReceiver = :isArchivedByReceiver"),
    @NamedQuery(name = "Messages.findByCreatedAt", query = "SELECT m FROM Messages m WHERE m.createdAt = :createdAt")})
public class Messages implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "MessageId")
    private Long messageId;
    @Size(max = 200)
    @Column(name = "Subject")
    private String subject;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 2147483647)
    @Column(name = "Content")
    private String content;
    @Basic(optional = false)
    @NotNull
    @Column(name = "IsArchivedBySender")
    private boolean isArchivedBySender;
    @Basic(optional = false)
    @NotNull
    @Column(name = "IsArchivedByReceiver")
    private boolean isArchivedByReceiver;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @JoinColumn(name = "BookingId", referencedColumnName = "BookingId")
    @ManyToOne
    private Bookings bookingId;
    @OneToMany(mappedBy = "parentMessageId")
    private Collection<Messages> messagesCollection;
    @JoinColumn(name = "ParentMessageId", referencedColumnName = "MessageId")
    @ManyToOne
    private Messages parentMessageId;
    @JoinColumn(name = "RestaurantId", referencedColumnName = "RestaurantId")
    @ManyToOne
    private Restaurants restaurantId;
    @JoinColumn(name = "FromUserId", referencedColumnName = "UserId")
    @ManyToOne(optional = false)
    private Users fromUserId;
    @JoinColumn(name = "ToUserId", referencedColumnName = "UserId")
    @ManyToOne(optional = false)
    private Users toUserId;

    public Messages() {
    }

    public Messages(Long messageId) {
        this.messageId = messageId;
    }

    public Messages(Long messageId, String content, boolean isArchivedBySender, boolean isArchivedByReceiver, Date createdAt) {
        this.messageId = messageId;
        this.content = content;
        this.isArchivedBySender = isArchivedBySender;
        this.isArchivedByReceiver = isArchivedByReceiver;
        this.createdAt = createdAt;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean getIsArchivedBySender() {
        return isArchivedBySender;
    }

    public void setIsArchivedBySender(boolean isArchivedBySender) {
        this.isArchivedBySender = isArchivedBySender;
    }

    public boolean getIsArchivedByReceiver() {
        return isArchivedByReceiver;
    }

    public void setIsArchivedByReceiver(boolean isArchivedByReceiver) {
        this.isArchivedByReceiver = isArchivedByReceiver;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Bookings getBookingId() {
        return bookingId;
    }

    public void setBookingId(Bookings bookingId) {
        this.bookingId = bookingId;
    }

    @XmlTransient
    public Collection<Messages> getMessagesCollection() {
        return messagesCollection;
    }

    public void setMessagesCollection(Collection<Messages> messagesCollection) {
        this.messagesCollection = messagesCollection;
    }

    public Messages getParentMessageId() {
        return parentMessageId;
    }

    public void setParentMessageId(Messages parentMessageId) {
        this.parentMessageId = parentMessageId;
    }

    public Restaurants getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Restaurants restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Users getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(Users fromUserId) {
        this.fromUserId = fromUserId;
    }

    public Users getToUserId() {
        return toUserId;
    }

    public void setToUserId(Users toUserId) {
        this.toUserId = toUserId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (messageId != null ? messageId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Messages)) {
            return false;
        }
        Messages other = (Messages) object;
        if ((this.messageId == null && other.messageId != null) || (this.messageId != null && !this.messageId.equals(other.messageId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.Messages[ messageId=" + messageId + " ]";
    }
    
}
