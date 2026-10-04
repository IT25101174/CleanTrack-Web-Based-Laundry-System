package com.cleantrack.laundry_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_name", nullable = false, length = 100, unique = true)
    private String supplierName;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(length = 30)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 200)
    private String address;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Supplier() {
    }

    public Supplier(String supplierName, String contactPerson, String phone, String email, String address) {
        this.supplierName = supplierName;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.email = email;
        this.address = address;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}