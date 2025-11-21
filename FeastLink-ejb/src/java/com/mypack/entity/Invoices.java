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
@Table(name = "Invoices")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Invoices.findAll", query = "SELECT i FROM Invoices i"),
    @NamedQuery(name = "Invoices.findByInvoiceId", query = "SELECT i FROM Invoices i WHERE i.invoiceId = :invoiceId"),
    @NamedQuery(name = "Invoices.findByInvoiceNumber", query = "SELECT i FROM Invoices i WHERE i.invoiceNumber = :invoiceNumber"),
    @NamedQuery(name = "Invoices.findByIssueDate", query = "SELECT i FROM Invoices i WHERE i.issueDate = :issueDate"),
    @NamedQuery(name = "Invoices.findBySubtotal", query = "SELECT i FROM Invoices i WHERE i.subtotal = :subtotal"),
    @NamedQuery(name = "Invoices.findByTaxAmount", query = "SELECT i FROM Invoices i WHERE i.taxAmount = :taxAmount"),
    @NamedQuery(name = "Invoices.findByTotalAmount", query = "SELECT i FROM Invoices i WHERE i.totalAmount = :totalAmount"),
    @NamedQuery(name = "Invoices.findByStatus", query = "SELECT i FROM Invoices i WHERE i.status = :status"),
    @NamedQuery(name = "Invoices.findByPdfUrl", query = "SELECT i FROM Invoices i WHERE i.pdfUrl = :pdfUrl")})
public class Invoices implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "InvoiceId")
    private Long invoiceId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "InvoiceNumber")
    private String invoiceNumber;
    @Basic(optional = false)
    @NotNull
    @Column(name = "IssueDate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date issueDate;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "Subtotal")
    private BigDecimal subtotal;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TaxAmount")
    private BigDecimal taxAmount;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TotalAmount")
    private BigDecimal totalAmount;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "Status")
    private String status;
    @Size(max = 255)
    @Column(name = "PdfUrl")
    private String pdfUrl;
    @OneToMany(mappedBy = "invoiceId")
    private Collection<Payments> paymentsCollection;
    @JoinColumn(name = "BookingId", referencedColumnName = "BookingId")
    @OneToOne(optional = false)
    private Bookings bookingId;

    public Invoices() {
    }

    public Invoices(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Invoices(Long invoiceId, String invoiceNumber, Date issueDate, BigDecimal subtotal, BigDecimal taxAmount, BigDecimal totalAmount, String status) {
        this.invoiceId = invoiceId;
        this.invoiceNumber = invoiceNumber;
        this.issueDate = issueDate;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Date getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(Date issueDate) {
        this.issueDate = issueDate;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    @XmlTransient
    public Collection<Payments> getPaymentsCollection() {
        return paymentsCollection;
    }

    public void setPaymentsCollection(Collection<Payments> paymentsCollection) {
        this.paymentsCollection = paymentsCollection;
    }

    public Bookings getBookingId() {
        return bookingId;
    }

    public void setBookingId(Bookings bookingId) {
        this.bookingId = bookingId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (invoiceId != null ? invoiceId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Invoices)) {
            return false;
        }
        Invoices other = (Invoices) object;
        if ((this.invoiceId == null && other.invoiceId != null) || (this.invoiceId != null && !this.invoiceId.equals(other.invoiceId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.Invoices[ invoiceId=" + invoiceId + " ]";
    }
    
}
