package com.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "persons")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // nvarchar(50) nullable
    @Column(name = "name", length = 50)
    private String name;

    // nvarchar(50) nullable
    @Column(name = "last_name", length = 50)
    private String lastName;

    // nvarchar(50) nullable (BUT constrained by CK_persons depending on isLegal)
    @Column(name = "company_name", length = 50)
    private String companyName;

    // nvarchar(4000)
    @Column(name = "address", length = 4000)
    private String address;

    // varchar(50)
    @Column(name = "tel", length = 50)
    private String tel;

    // bit NOT NULL
    @Column(name = "is_legal", nullable = false)
    private boolean isLegal;

    // nvarchar(4000)
    @Column(name = "dsc", length = 4000)
    private String dsc;

    public Person() {}

    // getters/setters

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getTel() { return tel; }
    public void setTel(String tel) { this.tel = tel; }

    public boolean isLegal() { return isLegal; }
    public void setLegal(boolean legal) { isLegal = legal; }

    public String getDsc() { return dsc; }
    public void setDsc(String dsc) { this.dsc = dsc; }
}
