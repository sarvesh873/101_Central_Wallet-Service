package com.central.wallet_service.service;

import com.central.wallet_service.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;

/**
 * Service interface for managing wallet holds.
 * Handles operations like placing, capturing, releasing, and adjusting holds.
 */
public interface HoldService {
    
    /**
     * Places a hold on a wallet's funds
     *
     * @param request Hold request details
     * @return Created hold details
     */
    HoldResponseDto placeHold(PlaceHoldRequestDto request);
    
    /**
     * Captures a hold and transfers funds from available to actual balance
     *
     * @param captureRequest Capture request details
     * @return Updated hold details
     */
    HoldResponseDto captureHoldFunds(CaptureRequestDto captureRequest);
    
    /**
     * Releases a hold and makes the funds available again
     *
     * @param holdId ID of the hold to release
     * @param request Release request details
     * @return Updated hold details
     */
    HoldResponseDto releaseHold(String holdId, ReleaseHoldRequestDto request);
    
    /**
     * Extends the expiration time of a hold
     *
     * @param holdId ID of the hold to extend
     * @param request Extension request details
     * @return Updated hold details
     */
    HoldResponseDto extendHold(String holdId, ExtendHoldRequestDto request);
    
    /**
     * Adjusts the amount of a hold
     *
     * @param holdId ID of the hold to adjust
     * @param request Adjustment request details
     * @return Updated hold details
     */
    HoldResponseDto adjustHold(String holdId, AdjustHoldRequestDto request);
    
    /**
     * Retrieves details of a specific hold
     *
     * @param holdId ID of the hold to retrieve
     * @return Hold details
     */
    HoldResponseDto getHold(String holdId);
    
    /**
     * Lists holds based on filter criteria
     *
     * @param userCode Filter by user code (optional)
     * @param status   Filter by status (optional)
     * @param currency Filter by currency (optional)
     * @param fromDate Filter by start date (optional)
     * @param toDate   Filter by end date (optional)
     * @param pageable Pagination information
     * @return Page of holds matching the criteria
     */
    Page<HoldResponseDto> listHolds(
        String userCode,
        String status,
        String currency,
        OffsetDateTime fromDate,
        OffsetDateTime toDate,
        Pageable pageable

    );


}
