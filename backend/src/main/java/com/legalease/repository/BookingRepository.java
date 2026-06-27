package com.legalease.repository;

import com.legalease.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Booking> findByLawyerIdOrderByBookingDateDesc(UUID lawyerId);
}
