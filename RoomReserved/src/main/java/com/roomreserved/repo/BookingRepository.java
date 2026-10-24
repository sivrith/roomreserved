package com.roomreserved.repo;
import com.roomreserved.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
public interface BookingRepository extends JpaRepository<Booking,Long>{
 @Query("select coalesce(sum(b.quantity),0) from Booking b where b.room.id=:roomId and b.checkIn < :outDate and b.checkOut > :inDate and (b.status in ('CONFIRMED','CHECKED_IN') or (b.status='PENDING' and b.holdExpiresAt > :now))")
 long sumReservedQuantity(@Param("roomId") Long roomId,@Param("inDate") LocalDate inDate,@Param("outDate") LocalDate outDate,@Param("now") LocalDateTime now);
 default long countConflicts(Long roomId,LocalDate inDate,LocalDate outDate){return sumReservedQuantity(roomId,inDate,outDate,LocalDateTime.now());}
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select b from Booking b where b.id=:id")
 java.util.Optional<Booking> findByIdForUpdate(@Param("id") Long id);
 java.util.Optional<Booking> findByBookingReference(String bookingReference);
 java.util.Optional<Booking> findByPaymongoCheckoutSessionId(String sessionId);
 @Query("select b from Booking b where b.status='PENDING' and b.holdExpiresAt <= :now")
 List<Booking> findExpiredPendingHolds(@Param("now") LocalDateTime now);
 List<Booking> findByUserEmailOrderByCreatedAtDesc(String email);
 List<Booking> findAllByOrderByCreatedAtDesc();
 @Modifying @Query("update Booking b set b.status='EXPIRED' where b.status='PENDING' and b.holdExpiresAt <= :now")
 int expirePendingHolds(@Param("now") LocalDateTime now);
}
