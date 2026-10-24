package com.roomreserved.service;

import com.roomreserved.model.AppUser;
import com.roomreserved.model.Booking;
import com.roomreserved.model.PromoCode;
import com.roomreserved.model.Room;
import com.roomreserved.repo.BookingRepository;
import com.roomreserved.repo.PromoRepository;
import com.roomreserved.repo.RoomRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
public class BookingService {
 private final RoomRepository rooms;
 private final BookingRepository bookings;
 private final PromoRepository promos;
 @Value("${roomreserved.pricing.tax-percent:0}") private BigDecimal taxPercent;
 @Value("${roomreserved.pricing.service-fee-percent:0}") private BigDecimal serviceFeePercent;
 @Value("${roomreserved.booking-holds.minutes:15}") private long holdMinutes;
 public BookingService(RoomRepository rooms, BookingRepository bookings, PromoRepository promos){this.rooms=rooms;this.bookings=bookings;this.promos=promos;}

 /**
  * Creates a temporary inventory hold. A hold is deliberately NOT a confirmed or paid booking.
  * The room row is locked before checking overlapping inventory so concurrent requests serialize.
  */
@Transactional
public void expireHoldIfStillPending(Long bookingId) {
  bookings.findByIdForUpdate(bookingId).ifPresent(booking -> {
   if ("PENDING".equals(booking.getStatus()) && booking.getHoldExpiresAt() != null
       && !booking.getHoldExpiresAt().isAfter(LocalDateTime.now())) {
    booking.setStatus("EXPIRED");
    bookings.save(booking);
   }
  });
 }

 public Booking createHold(Long roomId, LocalDate checkIn, LocalDate checkOut, String promoCode,
                           AppUser user, int quantity, int adults, int children) {
  if(checkIn==null||checkOut==null||!checkIn.isAfter(LocalDate.now().minusDays(1))||!checkOut.isAfter(checkIn))
   throw new IllegalArgumentException("Choose valid future check-in and check-out dates.");
  if(quantity<1||quantity>10||adults<1||children<0||adults+children>30)
   throw new IllegalArgumentException("Guest or room quantity is invalid.");
  Room room=rooms.findByIdForUpdate(roomId).orElseThrow(()->new IllegalArgumentException("Room not found."));
  if(!room.isActive()) throw new IllegalArgumentException("This room is not currently bookable.");
  if(adults+children>room.getCapacity()*quantity) throw new IllegalArgumentException("Guest capacity exceeded for the selected room quantity.");
  LocalDateTime now=LocalDateTime.now();
  long reserved=bookings.sumReservedQuantity(roomId,checkIn,checkOut,now);
  if(reserved+quantity>room.getInventoryCount()) throw new IllegalArgumentException("The requested room inventory is no longer available for the entire stay.");

  long numberOfNights=ChronoUnit.DAYS.between(checkIn,checkOut);
  BigDecimal subtotal=room.getPricePerNight().multiply(BigDecimal.valueOf(numberOfNights)).multiply(BigDecimal.valueOf(quantity));
  BigDecimal discount=BigDecimal.ZERO; String appliedCode=null;
  if(promoCode!=null&&!promoCode.isBlank()){
   PromoCode promo=promos.findByCodeIgnoreCaseAndActiveTrue(promoCode.trim()).orElseThrow(()->new IllegalArgumentException("Promo code is invalid or inactive."));
   if(promo.getExpiresAt()!=null&&promo.getExpiresAt().isBefore(LocalDate.now())) throw new IllegalArgumentException("Promo code has expired.");
   discount=subtotal.multiply(promo.getDiscountPercent()).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);
   appliedCode=promo.getCode();
  }
  BigDecimal taxable=subtotal.subtract(discount).max(BigDecimal.ZERO);
  BigDecimal tax=percent(taxable,taxPercent);
  BigDecimal fee=percent(taxable,serviceFeePercent);
  BigDecimal total=taxable.add(tax).add(fee).setScale(2,RoundingMode.HALF_UP);
  Booking booking=new Booking(); booking.setUser(user); booking.setRoom(room); booking.setCheckIn(checkIn); booking.setCheckOut(checkOut);
  booking.setQuantity(quantity); booking.setAdults(adults); booking.setChildren(children); booking.setRoomSubtotal(subtotal.setScale(2,RoundingMode.HALF_UP));
  booking.setDiscountAmount(discount.setScale(2,RoundingMode.HALF_UP)); booking.setTaxAmount(tax); booking.setServiceFee(fee); booking.setTotalPrice(total);
  booking.setPromoCode(appliedCode); booking.setStatus("PENDING"); booking.setPaymentStatus("NOT_PAID"); booking.setHoldExpiresAt(now.plusMinutes(Math.max(1,holdMinutes)));
  return bookings.saveAndFlush(booking);
 }
 private BigDecimal percent(BigDecimal base, BigDecimal percent){return base.multiply(percent.max(BigDecimal.ZERO)).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);}
}
