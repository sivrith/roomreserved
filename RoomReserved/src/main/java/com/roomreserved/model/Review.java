package com.roomreserved.model;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity
public class Review {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false) private AppUser user;
 @ManyToOne(optional=false) private Room room;
 @Column(nullable=false) private int stars;
 @Column(length=1200) private String comment;
 private LocalDateTime createdAt=LocalDateTime.now();
 public Long getId(){return id;} public AppUser getUser(){return user;} public void setUser(AppUser v){user=v;}
 public Room getRoom(){return room;} public void setRoom(Room v){room=v;}
 public int getStars(){return stars;} public void setStars(int v){stars=v;}
 public String getComment(){return comment;} public void setComment(String v){comment=v;}
 public LocalDateTime getCreatedAt(){return createdAt;}
}
