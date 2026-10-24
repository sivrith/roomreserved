package com.roomreserved.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity
public class Room {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private String name;
 @Column(length=1500) private String description;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal pricePerNight;
 private String imageUrl; private String amenities; private int capacity=2; private int inventoryCount=1; private boolean active=true;
 public Long getId(){return id;} public String getName(){return name;} public void setName(String v){name=v;}
 public String getDescription(){return description;} public void setDescription(String v){description=v;}
 public BigDecimal getPricePerNight(){return pricePerNight;} public void setPricePerNight(BigDecimal v){pricePerNight=v;}
 public String getImageUrl(){return imageUrl;} public void setImageUrl(String v){imageUrl=v;}
 public String getAmenities(){return amenities;} public void setAmenities(String v){amenities=v;}
 public int getCapacity(){return capacity;} public void setCapacity(int v){capacity=v;}
 public int getInventoryCount(){return inventoryCount;} public void setInventoryCount(int v){inventoryCount=v;}
 public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
}
