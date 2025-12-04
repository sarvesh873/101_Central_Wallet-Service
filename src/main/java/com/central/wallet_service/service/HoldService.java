package com.central.wallet_service.service;

import com.central.wallet_service.model.WalletHold;
import org.openapitools.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
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
    HoldResponse placeHold(HoldRequest request);
    
    /**
     * Captures a hold and transfers funds from available to actual balance
     *
     * @param captureRequest Capture request details
     * @return Updated hold details
     */
    HoldResponse captureHoldFunds(CaptureRequest captureRequest);
    
    /**
     * Releases a hold and makes the funds available again
     *
     * @param holdId ID of the hold to release
     * @param request Release request details
     * @return Updated hold details
     */
    HoldResponse releaseHold(String holdId, ReleaseHoldRequest request);
    
    /**
     * Extends the expiration time of a hold
     *
     * @param holdId ID of the hold to extend
     * @param request Extension request details
     * @return Updated hold details
     */
    HoldResponse extendHold(String holdId, ExtendHoldRequest request);
    
    /**
     * Adjusts the amount of a hold
     *
     * @param holdId ID of the hold to adjust
     * @param request Adjustment request details
     * @return Updated hold details
     */
    HoldResponse adjustHold(String holdId, AdjustHoldRequest request);
    
    /**
     * Retrieves details of a specific hold
     *
     * @param holdId ID of the hold to retrieve
     * @return Hold details
     */
    HoldResponse getHold(String holdId);
    
    /**
     * Lists holds based on filter criteria
     *
     * @param userCode Filter by user code (optional)
     * @param status Filter by status (optional)
     * @param currency Filter by currency (optional)
     * @param fromDate Filter by start date (optional)
     * @param toDate Filter by end date (optional)
     * @param pageable Pagination information
     * @return Page of holds matching the criteria
     */
    Page<HoldResponse> listHolds(
        String userCode,
        String status,
        String currency,
        OffsetDateTime fromDate,
        OffsetDateTime toDate,
        Pageable pageable

    );


}
