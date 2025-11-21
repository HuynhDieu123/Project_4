/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;

/**
 *
 * @author Laptop
 */
@Entity
@Table(name = "Bookings")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Bookings.findAll", query = "SELECT b FROM Bookings b"),
    @NamedQuery(name = "Bookings.findByBookingId", query = "SELECT b FROM Bookings b WHERE b.bookingId = :bookingId"),
    @NamedQuery(name = "Bookings.findByBookingCode", query = "SELECT b FROM Bookings b WHERE b.bookingCode = :bookingCode"),
    @NamedQuery(name = "Bookings.findByEventDate", query = "SELECT b FROM Bookings b WHERE b.eventDate = :eventDate"),
    @NamedQuery(name = "Bookings.findByStartTime", query = "SELECT b FROM Bookings b WHERE b.startTime = :startTime"),
    @NamedQuery(name = "Bookings.findByEndTime", query = "SELECT b FROM Bookings b WHERE b.endTime = :endTime"),
    @NamedQuery(name = "Bookings.findByGuestCount", query = "SELECT b FROM Bookings b WHERE b.guestCount = :guestCount"),
    @NamedQuery(name = "Bookings.findByLocationType", query = "SELECT b FROM Bookings b WHERE b.locationType = :locationType"),
    @NamedQuery(name = "Bookings.findByOutsideAddress", query = "SELECT b FROM Bookings b WHERE b.outsideAddress = :outsideAddress"),
    @NamedQuery(name = "Bookings.findByNote", query = "SELECT b FROM Bookings b WHERE b.note = :note"),
    @NamedQuery(name = "Bookings.findByTotalAmount", query = "SELECT b FROM Bookings b WHERE b.totalAmount = :totalAmount"),
    @NamedQuery(name = "Bookings.findByDepositAmount", query = "SELECT b FROM Bookings b WHERE b.depositAmount = :depositAmount"),
    @NamedQuery(name = "Bookings.findByRemainingAmount", query = "SELECT b FROM Bookings b WHERE b.remainingAmount = :remainingAmount"),
    @NamedQuery(name = "Bookings.findByBookingStatus", query = "SELECT b FROM Bookings b WHERE b.bookingStatus = :bookingStatus"),
    @NamedQuery(name = "Bookings.findByPaymentStatus", query = "SELECT b FROM Bookings b WHERE b.paymentStatus = :paymentStatus"),
    @NamedQuery(name = "Bookings.findByCancelReason", query = "SELECT b FROM Bookings b WHERE b.cancelReason = :cancelReason"),
    @NamedQuery(name = "Bookings.findByCancelTime", query = "SELECT b FROM Bookings b WHERE b.cancelTime = :cancelTime"),
    @NamedQuery(name = "Bookings.findByRejectReason", query = "SELECT b FROM Bookings b WHERE b.rejectReason = :rejectReason"),
    @NamedQuery(name = "Bookings.findByCreatedAt", query = "SELECT b FROM Bookings b WHERE b.createdAt = :createdAt"),
    @NamedQuery(name = "Bookings.findByUpdatedAt", query = "SELECT b FROM Bookings b WHERE b.updatedAt = :updatedAt")})
public class Bookings implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "BookingId")
    private Long bookingId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 30)
    @Column(name = "BookingCode")
    private String bookingCode;
    @Basic(optional = false)
    @NotNull
    @Column(name = "EventDate")
    @Temporal(TemporalType.DATE)
    private Date eventDate;
    @Column(name = "StartTime")
    @Temporal(TemporalType.TIME)
    private Date startTime;
    @Column(name = "EndTime")
    @Temporal(TemporalType.TIME)
    private Date endTime;
    @Basic(optional = false)
    @NotNull
    @Column(name = "GuestCount")
    private int guestCount;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "LocationType")
    private String locationType;
    @Size(max = 255)
    @Column(name = "OutsideAddress")
    private String outsideAddress;
    @Size(max = 2147483647)
    @Column(name = "Note")
    private String note;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Column(name = "TotalAmount")
    private BigDecimal totalAmount;
    @Column(name = "DepositAmount")
    private BigDecimal depositAmount;
    @Column(name = "RemainingAmount")
    private BigDecimal remainingAmount;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "BookingStatus")
    private String bookingStatus;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "PaymentStatus")
    private String paymentStatus;
    @Size(max = 2147483647)
    @Column(name = "CancelReason")
    private String cancelReason;
    @Column(name = "CancelTime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date cancelTime;
    @Size(max = 2147483647)
    @Column(name = "RejectReason")
    private String rejectReason;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Column(name = "UpdatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "bookingId")
    private Collection<RestaurantReviews> restaurantReviewsCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "bookingId")
    private Collection<BookingStatusHistory> bookingStatusHistoryCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "bookingId")
    private Collection<Payments> paymentsCollection;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "bookingId")
    private Invoices invoices;
    @OneToMany(mappedBy = "bookingId")
    private Collection<Messages> messagesCollection;
    @OneToMany(mappedBy = "bookingId")
    private Collection<Feedbacks> feedbacksCollection;
    @JoinColumn(name = "EventTypeId", referencedColumnName = "EventTypeId")
    @ManyToOne
    private EventTypes eventTypeId;
    @JoinColumn(name = "RestaurantId", referencedColumnName = "RestaurantId")
    @ManyToOne(optional = false)
    private Restaurants restaurantId;
    @JoinColumn(name = "ServiceTypeId", referencedColumnName = "ServiceTypeId")
    @ManyToOne
    private ServiceTypes serviceTypeId;
    @JoinColumn(name = "CustomerId", referencedColumnName = "UserId")
    @ManyToOne(optional = false)
    private Users customerId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "bookings")
    private Collection<BookingCombos> bookingCombosCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "bookings")
    private Collection<BookingMenuItems> bookingMenuItemsCollection;

    public Bookings() {
    }

    public Bookings(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Bookings(Long bookingId, String bookingCode, Date eventDate, int guestCount, String locationType, String bookingStatus, String paymentStatus, Date createdAt) {
        this.bookingId = bookingId;
        this.bookingCode = bookingCode;
        this.eventDate = eventDate;
        this.guestCount = guestCount;
        this.locationType = locationType;
        this.bookingStatus = bookingStatus;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public int getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(int guestCount) {
        this.guestCount = guestCount;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public String getOutsideAddress() {
        return outsideAddress;
    }

    public void setOutsideAddress(String outsideAddress) {
        this.outsideAddress = outsideAddress;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public Date getCancelTime() {
        return cancelTime;
    }

    public void setCancelTime(Date cancelTime) {
        this.cancelTime = cancelTime;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
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

    @XmlTransient
    public Collection<RestaurantReviews> getRestaurantReviewsCollection() {
        return restaurantReviewsCollection;
    }

    public void setRestaurantReviewsCollection(Collection<RestaurantReviews> restaurantReviewsCollection) {
        this.restaurantReviewsCollection = restaurantReviewsCollection;
    }

    @XmlTransient
    public Collection<BookingStatusHistory> getBookingStatusHistoryCollection() {
        return bookingStatusHistoryCollection;
    }

    public void setBookingStatusHistoryCollection(Collection<BookingStatusHistory> bookingStatusHistoryCollection) {
        this.bookingStatusHistoryCollection = bookingStatusHistoryCollection;
    }

    @XmlTransient
    public Collection<Payments> getPaymentsCollection() {
        return paymentsCollection;
    }

    public void setPaymentsCollection(Collection<Payments> paymentsCollection) {
        this.paymentsCollection = paymentsCollection;
    }

    public Invoices getInvoices() {
        return invoices;
    }

    public void setInvoices(Invoices invoices) {
        this.invoices = invoices;
    }

    @XmlTransient
    public Collection<Messages> getMessagesCollection() {
        return messagesCollection;
    }

    public void setMessagesCollection(Collection<Messages> messagesCollection) {
        this.messagesCollection = messagesCollection;
    }

    @XmlTransient
    public Collection<Feedbacks> getFeedbacksCollection() {
        return feedbacksCollection;
    }

    public void setFeedbacksCollection(Collection<Feedbacks> feedbacksCollection) {
        this.feedbacksCollection = feedbacksCollection;
    }

    public EventTypes getEventTypeId() {
        return eventTypeId;
    }

    public void setEventTypeId(EventTypes eventTypeId) {
        this.eventTypeId = eventTypeId;
    }

    public Restaurants getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Restaurants restaurantId) {
        this.restaurantId = restaurantId;
    }

    public ServiceTypes getServiceTypeId() {
        return serviceTypeId;
    }

    public void setServiceTypeId(ServiceTypes serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
    }

    public Users getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Users customerId) {
        this.customerId = customerId;
    }

    @XmlTransient
    public Collection<BookingCombos> getBookingCombosCollection() {
        return bookingCombosCollection;
    }

    public void setBookingCombosCollection(Collection<BookingCombos> bookingCombosCollection) {
        this.bookingCombosCollection = bookingCombosCollection;
    }

    @XmlTransient
    public Collection<BookingMenuItems> getBookingMenuItemsCollection() {
        return bookingMenuItemsCollection;
    }

    public void setBookingMenuItemsCollection(Collection<BookingMenuItems> bookingMenuItemsCollection) {
        this.bookingMenuItemsCollection = bookingMenuItemsCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (bookingId != null ? bookingId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Bookings)) {
            return false;
        }
        Bookings other = (Bookings) object;
        if ((this.bookingId == null && other.bookingId != null) || (this.bookingId != null && !this.bookingId.equals(other.bookingId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.Bookings[ bookingId=" + bookingId + " ]";
    }
    
}
