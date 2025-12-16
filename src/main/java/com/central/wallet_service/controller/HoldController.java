package com.central.wallet_service.controller;

import com.central.wallet_service.dto.*;
import com.central.wallet_service.dto.adapter.request.*;
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
import com.central.wallet_service.utils.ServiceUtils;

import java.time.OffsetDateTime;

import static com.central.wallet_service.utils.ServiceUtils.toHoldResponse;

@Slf4j
@RestController
public class HoldController implements HoldsApi {

    @Autowired
    private HoldService holdService;


    @Override
    public ResponseEntity<HoldResponse> adjustHold(String holdId, AdjustHoldRequest adjustHoldRequest) {
        AdjustHoldRequestDto requestDto = new RestAdjustHoldRequestAdapter(adjustHoldRequest);
        HoldResponseDto responseDto = holdService.adjustHold(holdId, requestDto);
        return ResponseEntity.ok(toHoldResponse(responseDto));
    }

    @Override
    public ResponseEntity<HoldResponse> captureHoldFunds(CaptureRequest captureRequest) {
        CaptureRequestDto requestDto = new RestCaptureRequestAdapter(captureRequest);
        HoldResponseDto responseDto = holdService.captureHoldFunds(requestDto);
        return ResponseEntity.ok(toHoldResponse(responseDto));
    }

    @Override
    public ResponseEntity<HoldResponse> extendHold(String holdId, ExtendHoldRequest extendHoldRequest) {
        ExtendHoldRequestDto requestDto = new RestExtendHoldRequestAdapter(extendHoldRequest);
        HoldResponseDto responseDto = holdService.extendHold(holdId, requestDto);
        return ResponseEntity.ok(toHoldResponse(responseDto));
    }

    @Override
    public ResponseEntity<HoldResponse> getHold(String holdId) {
        HoldResponseDto responseDto = holdService.getHold(holdId);
        return ResponseEntity.ok(toHoldResponse(responseDto));
    }

    @Override
    public ResponseEntity<ListHolds200Response> listHolds(String userCode, String status, String currency,
                                                          OffsetDateTime fromDate, OffsetDateTime toDate,
                                                          Integer page, Integer pageSize, String sortBy,
                                                          Boolean sortDesc) {
        int pageNumber = page != null ? page : 0;
        int size = pageSize != null ? pageSize : 20;
        String sortField = StringUtils.isNotBlank(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction = Boolean.TRUE.equals(sortDesc) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by(direction, sortField));

        Page<HoldResponseDto> holdsPage = holdService.listHolds(
                userCode, status, currency, fromDate, toDate, pageable);

        ListHolds200Response response = new ListHolds200Response();
        response.setItems(holdsPage.getContent().stream()
                .map(ServiceUtils::toHoldResponse)
                .toList());
        response.setPagination(new PaginationResponse()
                .currentPage(holdsPage.getNumber() + 1)
                .pageSize(holdsPage.getSize())
                .totalItems(holdsPage.getTotalElements())
                .totalPages(holdsPage.getTotalPages())
                .hasNext(holdsPage.hasNext())
                .hasPrevious(holdsPage.hasPrevious()));

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<HoldResponse> placeHold(HoldRequest holdRequest) {
        PlaceHoldRequestDto requestDto = new RestHoldRequestAdapter(holdRequest);
        HoldResponseDto responseDto = holdService.placeHold(requestDto);
        return ResponseEntity.ok(toHoldResponse(responseDto));
    }

    @Override
    public ResponseEntity<HoldResponse> releaseHold(String holdId, ReleaseHoldRequest releaseHoldRequest) {
        ReleaseHoldRequestDto requestDto = new RestReleaseHoldRequestAdapter(releaseHoldRequest);
        HoldResponseDto responseDto = holdService.releaseHold(holdId, requestDto);
        return ResponseEntity.ok(toHoldResponse(responseDto));
    }
}