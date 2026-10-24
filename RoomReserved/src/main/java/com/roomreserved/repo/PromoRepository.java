package com.roomreserved.repo;
import com.roomreserved.model.PromoCode; import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface PromoRepository extends JpaRepository<PromoCode,Long>{Optional<PromoCode> findByCodeIgnoreCaseAndActiveTrue(String code);}
