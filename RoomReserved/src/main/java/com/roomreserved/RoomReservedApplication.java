package com.roomreserved;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class RoomReservedApplication {
 public static void main(String[] args) { SpringApplication.run(RoomReservedApplication.class, args); }
}
