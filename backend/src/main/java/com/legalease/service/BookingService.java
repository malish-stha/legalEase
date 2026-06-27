package com.legalease.service;

import com.legalease.entity.Booking;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingService {
    Booking createBooking(String clerkUserId, UUID lawyerId, LocalDate date, String startTime, String endTime, String userExplanation, UUID associatedDocId);
    List<Booking> getUserBookings(String clerkUserId);
    List<Booking> getLawyerBookings(UUID lawyerId);
    Booking getBookingById(UUID id, String clerkUserId);
    Booking updateBookingStatus(UUID id, String status);
}
