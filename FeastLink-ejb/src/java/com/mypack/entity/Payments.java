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
import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @author Laptop
 */
@Entity
@Table(name = "Payments")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Payments.findAll", query = "SELECT p FROM Payments p"),
    @NamedQuery(name = "Payments.findByPaymentId", query = "SELECT p FROM Payments p WHERE p.paymentId = :paymentId"),
    @NamedQuery(name = "Payments.findByPaymentMethod", query = "SELECT p FROM Payments p WHERE p.paymentMethod = :paymentMethod"),
    @NamedQuery(name = "Payments.findByPaymentType", query = "SELECT p FROM Payments p WHERE p.paymentType = :paymentType"),
    @NamedQuery(name = "Payments.findByPaymentGateway", query = "SELECT p FROM Payments p WHERE p.paymentGateway = :paymentGateway"),
    @NamedQuery(name = "Payments.findByAmount", query = "SELECT p FROM Payments p WHERE p.amount = :amount"),
    @NamedQuery(name = "Payments.findByTransactionCode", query = "SELECT p FROM Payments p WHERE p.transactionCode = :transactionCode"),
    @NamedQuery(name = "Payments.findByStatus", query = "SELECT p FROM Payments p WHERE p.status = :status"),
    @NamedQuery(name = "Payments.findByPaidAt", query = "SELECT p FROM Payments p WHERE p.paidAt = :paidAt"),
    @NamedQuery(name = "Payments.findByRawResponse", query = "SELECT p FROM Payments p WHERE p.rawResponse = :rawResponse")})
public class Payments implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "PaymentId")
    private Long paymentId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "PaymentMethod")
    private String paymentMethod;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "PaymentType")
    private String paymentType;
    @Size(max = 30)
    @Column(name = "PaymentGateway")
    private String paymentGateway;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "Amount")
    private BigDecimal amount;
    @Size(max = 100)
    @Column(name = "TransactionCode")
    private String transactionCode;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "Status")
    private String status;
    @Column(name = "PaidAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date paidAt;
    @Size(max = 2147483647)
    @Column(name = "RawResponse")
    private String rawResponse;
    @JoinColumn(name = "BookingId", referencedColumnName = "BookingId")
    @ManyToOne(optional = false)
    private Bookings bookingId;
    @JoinColumn(name = "InvoiceId", referencedColumnName = "InvoiceId")
    @ManyToOne
    private Invoices invoiceId;

    public Payments() {
    }

    public Payments(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Payments(Long paymentId, String paymentMethod, String paymentType, BigDecimal amount, String status) {
        this.paymentId = paymentId;
        this.paymentMethod = paymentMethod;
        this.paymentType = paymentType;
        this.amount = amount;
        this.status = status;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getPaymentGateway() {
        return paymentGateway;
    }

    public void setPaymentGateway(String paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Date paidAt) {
        this.paidAt = paidAt;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public Bookings getBookingId() {
        return bookingId;
    }

    public void setBookingId(Bookings bookingId) {
        this.bookingId = bookingId;
    }

    public Invoices getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Invoices invoiceId) {
        this.invoiceId = invoiceId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (paymentId != null ? paymentId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Payments)) {
            return false;
        }
        Payments other = (Payments) object;
        if ((this.paymentId == null && other.paymentId != null) || (this.paymentId != null && !this.paymentId.equals(other.paymentId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.Payments[ paymentId=" + paymentId + " ]";
    }
    
}
