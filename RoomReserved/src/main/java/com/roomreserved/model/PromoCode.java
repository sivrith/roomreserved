package com.roomreserved.model;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.LocalDate;
@Entity
public class PromoCode {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true) private String code;
 @Column(nullable=false,precision=5,scale=2) private BigDecimal discountPercent;
 private LocalDate expiresAt; private boolean active=true;
 public Long getId(){return id;} public String getCode(){return code;} public void setCode(String v){code=v;}
 public BigDecimal getDiscountPercent(){return discountPercent;} public void setDiscountPercent(BigDecimal v){discountPercent=v;}
 public LocalDate getExpiresAt(){return expiresAt;} public void setExpiresAt(LocalDate v){expiresAt=v;}
 public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
}
