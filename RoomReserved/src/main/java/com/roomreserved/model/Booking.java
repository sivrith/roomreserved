package com.roomreserved.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(indexes={@Index(columnList="checkIn,checkOut"),@Index(columnList="room_id"),@Index(columnList="status,holdExpiresAt")})
public class Booking {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=32) private String bookingReference;
 @ManyToOne(optional=false) private AppUser user;
 @ManyToOne(optional=false) private Room room;
 @Column(nullable=false) private LocalDate checkIn;
 @Column(nullable=false) private LocalDate checkOut;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal roomSubtotal=BigDecimal.ZERO;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal taxAmount=BigDecimal.ZERO;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal serviceFee=BigDecimal.ZERO;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal discountAmount=BigDecimal.ZERO;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal totalPrice;
 @Column(nullable=false,length=24) private String status="PENDING";
 @Column(nullable=false,length=24) private String paymentStatus="NOT_PAID";
 @Column(length=24) private String paymentProvider;
 @Column(unique=true,length=100) private String paymongoCheckoutSessionId;
 @Column(length=1000) private String paymongoCheckoutUrl;
 @Column(length=100) private String paymongoPaymentIntentId;
 private LocalDateTime paymentUpdatedAt;
 @Column(nullable=false) private int quantity=1;
 @Column(nullable=false) private int adults=2;
 @Column(nullable=false) private int children=0;
 private String promoCode;
 private LocalDateTime holdExpiresAt;
 private LocalDateTime createdAt=LocalDateTime.now();
 @PrePersist void ensureReference(){if(bookingReference==null||bookingReference.isBlank())bookingReference="RR-"+UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase();if(createdAt==null)createdAt=LocalDateTime.now();}
 public Long getId(){return id;} public String getBookingReference(){return bookingReference;} public void setBookingReference(String v){bookingReference=v;}
 public AppUser getUser(){return user;} public void setUser(AppUser v){user=v;}
 public Room getRoom(){return room;} public void setRoom(Room v){room=v;}
 public LocalDate getCheckIn(){return checkIn;} public void setCheckIn(LocalDate v){checkIn=v;}
 public LocalDate getCheckOut(){return checkOut;} public void setCheckOut(LocalDate v){checkOut=v;}
 public BigDecimal getRoomSubtotal(){return roomSubtotal;} public void setRoomSubtotal(BigDecimal v){roomSubtotal=v;}
 public BigDecimal getTaxAmount(){return taxAmount;} public void setTaxAmount(BigDecimal v){taxAmount=v;}
 public BigDecimal getServiceFee(){return serviceFee;} public void setServiceFee(BigDecimal v){serviceFee=v;}
 public BigDecimal getDiscountAmount(){return discountAmount;} public void setDiscountAmount(BigDecimal v){discountAmount=v;}
 public BigDecimal getTotalPrice(){return totalPrice;} public void setTotalPrice(BigDecimal v){totalPrice=v;}
 public String getStatus(){return status;} public void setStatus(String v){status=v;}
 public String getPaymentStatus(){return paymentStatus;} public void setPaymentStatus(String v){paymentStatus=v;}
 public String getPaymentProvider(){return paymentProvider;} public void setPaymentProvider(String v){paymentProvider=v;}
 public String getPaymongoCheckoutSessionId(){return paymongoCheckoutSessionId;} public void setPaymongoCheckoutSessionId(String v){paymongoCheckoutSessionId=v;}
 public String getPaymongoCheckoutUrl(){return paymongoCheckoutUrl;} public void setPaymongoCheckoutUrl(String v){paymongoCheckoutUrl=v;}
 public String getPaymongoPaymentIntentId(){return paymongoPaymentIntentId;} public void setPaymongoPaymentIntentId(String v){paymongoPaymentIntentId=v;}
 public LocalDateTime getPaymentUpdatedAt(){return paymentUpdatedAt;} public void setPaymentUpdatedAt(LocalDateTime v){paymentUpdatedAt=v;}
 public int getQuantity(){return quantity;} public void setQuantity(int v){quantity=v;}
 public int getAdults(){return adults;} public void setAdults(int v){adults=v;}
 public int getChildren(){return children;} public void setChildren(int v){children=v;}
 public String getPromoCode(){return promoCode;} public void setPromoCode(String v){promoCode=v;}
 public LocalDateTime getHoldExpiresAt(){return holdExpiresAt;} public void setHoldExpiresAt(LocalDateTime v){holdExpiresAt=v;}
 public LocalDateTime getCreatedAt(){return createdAt;}
}
