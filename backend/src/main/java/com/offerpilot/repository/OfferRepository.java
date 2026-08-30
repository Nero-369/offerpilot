package com.offerpilot.repository;
import com.offerpilot.domain.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface OfferRepository extends JpaRepository<Offer, UUID> {}
