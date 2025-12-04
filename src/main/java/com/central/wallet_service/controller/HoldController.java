package com.central.wallet_service.controller;

import com.central.wallet_service.service.HoldService;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.api.HoldsApi;
import org.openapitools.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;

@Slf4j
@RestController
public class HoldController implements HoldsApi {

    @Autowired
    private HoldService holdService;


    @Override
    public ResponseEntity<HoldResponse> adjustHold(String holdId, AdjustHoldRequest adjustHoldRequest) {
        HoldResponse response = holdService.adjustHold(holdId, adjustHoldRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<HoldResponse> captureHoldFunds(CaptureRequest captureRequest) {
        HoldResponse response = holdService.captureHoldFunds(captureRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<HoldResponse> extendHold(String holdId, ExtendHoldRequest extendHoldRequest) {
        HoldResponse response = holdService.extendHold(holdId, extendHoldRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<HoldResponse> getHold(String holdId) {
        HoldResponse response = holdService.getHold(holdId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ListHolds200Response> listHolds(String userCode, String status, String currency, OffsetDateTime fromDate, OffsetDateTime toDate, Integer page, Integer pageSize, String sortBy, Boolean sortDesc) {

        // Set default values if not provided
        int pageNumber = page != null ? page : 0;
        int size = pageSize != null ? pageSize : 20;
        String sortField = StringUtils.isNotBlank(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction = Boolean.TRUE.equals(sortDesc) ? Sort.Direction.DESC : Sort.Direction.ASC;

        // Create Pageable for pagination
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by(direction, sortField));

        // Call service method
        Page<HoldResponse> holdsPage = holdService.listHolds(
                userCode,
                status,
                currency,
                fromDate,
                toDate,
                pageable
        );

        // Debug logging
        log.debug("Total elements: {}, Content size: {}", 
            holdsPage.getTotalElements(), 
            holdsPage.getContent() != null ? holdsPage.getContent().size() : 0);
            
        if (holdsPage.getContent() != null && !holdsPage.getContent().isEmpty()) {
            log.debug("First hold in page: {}", holdsPage.getContent().get(0));
        }

        // Create response
        ListHolds200Response response = new ListHolds200Response();
        response.setItems(holdsPage.getContent());
        response.setPagination(new PaginationResponse()
            .currentPage(holdsPage.getNumber() + 1) // Page numbers are 1-based in the response
            .pageSize(holdsPage.getSize())
            .totalItems(holdsPage.getTotalElements())
            .totalPages(holdsPage.getTotalPages())
            .hasNext(holdsPage.hasNext())
            .hasPrevious(holdsPage.hasPrevious())
        );
        
        log.debug("Response items size: {}", response.getItems() != null ? response.getItems().size() : 0);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<HoldResponse> placeHold(HoldRequest holdRequest) {
        HoldResponse response = holdService.placeHold(holdRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<HoldResponse> releaseHold(String holdId, ReleaseHoldRequest releaseHoldRequest) {
        HoldResponse response = holdService.releaseHold(holdId, releaseHoldRequest);
        return ResponseEntity.ok(response);
    }
}