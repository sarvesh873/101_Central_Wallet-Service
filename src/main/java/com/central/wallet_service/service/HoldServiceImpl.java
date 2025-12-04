package com.central.wallet_service.service;

import com.central.wallet_service.constants.WalletConstants;
import com.central.wallet_service.exception.WalletException;
import com.central.wallet_service.model.HoldStatus;
import com.central.wallet_service.model.Wallet;
import com.central.wallet_service.model.WalletHold;
import com.central.wallet_service.repository.WalletHoldRepository;
import com.central.wallet_service.repository.WalletRepository;
import com.central.wallet_service.utils.ServiceUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import com.central.wallet_service.specifications.WalletHoldSpecifications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.central.wallet_service.utils.ServiceUtils.constructHoldResponse;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HoldServiceImpl implements HoldService {

    @Autowired
    private final WalletHoldRepository holdRepository;

    @Autowired
    private final WalletRepository walletRepository;


    @Override
    @Transactional
    public HoldResponse placeHold(HoldRequest request) {
        try {
            validateHoldRequest(request);
            
            Wallet wallet = walletRepository.findByUserCode(request.getUserCode())
                .orElseThrow(() -> WalletException.notFound(
                    String.format(WalletConstants.WALLET_NOT_FOUND, request.getUserCode())));

            // Validate wallet status and available balance
            if (wallet.getWalletStatus() != HoldStatus.ACTIVE) {
                throw WalletException.badRequest(
                    String.format(WalletConstants.WALLET_INACTIVE, request.getUserCode()));
            }

            Double amount = request.getAmount();
            Double walletBalance = wallet.getAvailableBalance();
            if (walletBalance < amount) {
                throw WalletException.insufficientFunds(WalletConstants.INSUFFICIENT_FUNDS);
            }
            Double newBalanceAfterHoldPlaced = walletBalance - amount;

            // Create and save the hold
            WalletHold hold = WalletHold.builder()
                .wallet(wallet)
                .capturedAmount(amount)
                .transaction_id(request.getTransactionId())
                .description(request.getDescription())
                .status(HoldStatus.ACTIVE)
                    .holdId("HOLD-" + System.currentTimeMillis())
                    .capturedAmount(amount)
                .originalAmount(walletBalance)
                 .remainingAmount(newBalanceAfterHoldPlaced)
                .build();

            WalletHold savedHold = holdRepository.save(hold);
            
            // Update wallet's available balance
            wallet.setAvailableBalance(newBalanceAfterHoldPlaced);
            walletRepository.save(wallet);

            log.info(WalletConstants.LOG_HOLD_PLACED, 
                amount, wallet.getCurrency(), wallet.getId());


                
            return constructHoldResponse(savedHold);
            
        } catch (WalletException ex) {
            log.error("Error placing hold: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error placing hold: {}", ex.getMessage(), ex);
            throw WalletException.internalServerError(WalletConstants.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public HoldResponse captureHoldFunds(CaptureRequest request) {
        try {
            validateCaptureRequest(request);
            
            WalletHold hold = holdRepository.findByHoldId(request.getHoldId())
                .orElseThrow(() -> WalletException.notFound(
                    String.format(WalletConstants.HOLD_NOT_FOUND, request.getHoldId())));
            
            // Validate hold status
            if (hold.getStatus() != HoldStatus.ACTIVE) {
                throw WalletException.badRequest(
                    String.format(WalletConstants.HOLD_ALREADY_PROCESSED, hold.getStatus().name().toLowerCase()));
            }
            
            // Validate hold expiration
            if (hold.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw WalletException.badRequest(WalletConstants.HOLD_EXPIRED);
            }

            Wallet wallet = hold.getWallet();
            
            // Update hold status
            hold.setStatus(HoldStatus.CAPTURED);
            hold.setUpdatedAt(LocalDateTime.now());
            wallet.setBalance(wallet.getBalance()-hold.getCapturedAmount());
            WalletHold updatedHold = holdRepository.save(hold);
            
            log.info(WalletConstants.LOG_HOLD_CAPTURED, 
                hold.getCapturedAmount(), wallet.getCurrency(), hold.getId());
                
            return constructHoldResponse(updatedHold);
            
        } catch (WalletException ex) {
            log.error("Error capturing hold: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error capturing hold: {}", ex.getMessage(), ex);
            throw WalletException.internalServerError(WalletConstants.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public HoldResponse releaseHold(String holdId, ReleaseHoldRequest request) {
        try {
            WalletHold hold = getValidHoldForRelease(holdId, request);
            
            // Update hold status
            hold.setStatus(HoldStatus.RELEASED);
            hold.setUpdatedAt(LocalDateTime.now());
            hold.setDescription(request.getReason());
            hold.setMetadata((JsonNode) request.getMetadata());
            WalletHold releasedHold = holdRepository.save(hold);
            
            // Return funds to available balance
            Wallet wallet = hold.getWallet();
            wallet.setAvailableBalance(wallet.getAvailableBalance() + hold.getCapturedAmount());
            walletRepository.save(wallet);
            
            log.info(WalletConstants.LOG_HOLD_RELEASED, holdId);
            return constructHoldResponse(releasedHold);
            
        } catch (WalletException ex) {
            log.error("Error releasing hold: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error releasing hold: {}", ex.getMessage(), ex);
            throw WalletException.internalServerError(WalletConstants.INTERNAL_SERVER_ERROR);
        }
    }

    // Helper methods will be implemented in the next chunk
    private void validateHoldRequest(HoldRequest request) {
        if (request == null) {
            throw WalletException.badRequest("Hold request cannot be null");
        }
        
        if (StringUtils.isBlank(request.getUserCode())) {
            throw WalletException.badRequest("User code is required");
        }
        
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw WalletException.badRequest("Amount must be greater than zero");
        }
    }
    
    private void validateCaptureRequest(CaptureRequest request) {
        if (request == null || StringUtils.isBlank(request.getHoldId())) {
            throw WalletException.badRequest("Hold ID is required");
        }
    }
    
    private WalletHold getValidHoldForRelease(String holdId, ReleaseHoldRequest request) {
        if (StringUtils.isBlank(holdId)) {
            throw WalletException.badRequest("Hold ID is required");
        }
        
        if (request == null || StringUtils.isBlank(request.getReason())) {
            throw WalletException.badRequest("Release reason is required");
        }
        
        WalletHold hold = holdRepository.findByHoldId(holdId)
            .orElseThrow(() -> WalletException.notFound(
                String.format(WalletConstants.HOLD_NOT_FOUND, holdId)));
        
        // Validate hold status
        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw WalletException.badRequest(
                String.format(WalletConstants.HOLD_ALREADY_PROCESSED, hold.getStatus().name().toLowerCase()));
        }
        
        return hold;
    }

    @Override
    @Transactional
    public HoldResponse extendHold(String holdId, ExtendHoldRequest request) {
        try {
            if (StringUtils.isBlank(holdId)) {
                throw WalletException.badRequest(WalletConstants.INVALID_REQUEST);
            }
            
            if (request == null || request.getNewExpiresAt() == null) {
                throw WalletException.badRequest("New expiry time is required");
            }
            
            WalletHold hold = holdRepository.findByHoldId(holdId)
                .orElseThrow(() -> WalletException.notFound(
                    String.format(WalletConstants.HOLD_NOT_FOUND, holdId)));
            
            // Validate hold status
            if (hold.getStatus() == HoldStatus.EXPIRED || hold.getStatus() == HoldStatus.RELEASED) {
                throw WalletException.badRequest(
                    String.format(WalletConstants.HOLD_ALREADY_PROCESSED, hold.getStatus().name().toLowerCase()));
            }

            LocalDateTime newExpiry = ServiceUtils.toLocalDateTime(request.getNewExpiresAt());
            if (newExpiry.isBefore(LocalDateTime.now())) {
                throw WalletException.badRequest("New expiry time must be in the future");
            }

            // Update hold
            hold.setExpiresAt(newExpiry);
            hold.setUpdatedAt(LocalDateTime.now());
            hold.setDescription(request.getReason());
            WalletHold updatedHold = holdRepository.save(hold);

            log.info(WalletConstants.LOG_HOLD_EXTENDED, holdId, newExpiry);
            return constructHoldResponse(updatedHold);

        } catch (WalletException ex) {
            log.error("Error extending hold: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error extending hold: {}", ex.getMessage(), ex);
            throw WalletException.internalServerError(WalletConstants.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public HoldResponse adjustHold(String holdId, AdjustHoldRequest request) {
        try {
            if (StringUtils.isBlank(holdId)) {
                throw WalletException.badRequest(WalletConstants.INVALID_REQUEST);
            }
            
            if (request == null || request.getNewAmount() == null) {
                throw WalletException.badRequest(WalletConstants.INVALID_AMOUNT);
            }
            
            WalletHold hold = holdRepository.findByHoldId(holdId)
                .orElseThrow(() -> WalletException.notFound(
                    String.format(WalletConstants.HOLD_NOT_FOUND, holdId)));
            
            // Validate hold status
            if (hold.getStatus() == HoldStatus.EXPIRED || hold.getStatus() == HoldStatus.RELEASED) {
                throw WalletException.badRequest(
                    String.format(WalletConstants.HOLD_ALREADY_PROCESSED, hold.getStatus().name().toLowerCase()));
            }
            
            Double newAmount = request.getNewAmount();
            if (newAmount <= 0) {
                throw WalletException.badRequest(WalletConstants.INVALID_AMOUNT);
            }
            
            Wallet wallet = hold.getWallet();
            double amountDifference = newAmount - hold.getCapturedAmount();
            
            // If increasing the hold amount, check available balance
            if (amountDifference > 0 && wallet.getAvailableBalance() < amountDifference) {
                throw WalletException.insufficientFunds(WalletConstants.INSUFFICIENT_FUNDS);
            }
            
            // Update hold amount and wallet's available balance
            hold.setCapturedAmount(newAmount);
            wallet.setAvailableBalance(wallet.getAvailableBalance() - amountDifference);
            hold.setUpdatedAt(LocalDateTime.now());
            hold.setDescription(request.getReason());
            WalletHold updatedHold = holdRepository.save(hold);
            walletRepository.save(wallet);
            
            log.info(WalletConstants.LOG_HOLD_ADJUSTED, holdId, newAmount);
            return constructHoldResponse(updatedHold);
            
        } catch (WalletException ex) {
            log.error("Error adjusting hold: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error adjusting hold: {}", ex.getMessage(), ex);
            throw WalletException.internalServerError(WalletConstants.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public HoldResponse getHold(String holdId) {
        try {
            if (StringUtils.isBlank(holdId)) {
                log.warn("{} - Hold ID is empty", WalletConstants.INVALID_REQUEST);
                throw WalletException.badRequest(WalletConstants.INVALID_REQUEST);
            }
            
            WalletHold hold = holdRepository.findByHoldId(holdId)
                .orElseThrow(() -> {
                    log.warn("{} - Hold ID: {}", 
                        String.format(WalletConstants.HOLD_NOT_FOUND, holdId), holdId);
                    return WalletException.notFound(
                        String.format(WalletConstants.HOLD_NOT_FOUND, holdId));
                });
                
            log.debug("Retrieved hold with ID: {}", holdId);
            return constructHoldResponse(hold);
            
        } catch (WalletException ex) {
            log.error("{}: {}", WalletConstants.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("{} while fetching hold {}: {}", 
                WalletConstants.INTERNAL_SERVER_ERROR, holdId, ex.getMessage(), ex);
            throw WalletException.internalServerError(WalletConstants.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Page<HoldResponse> listHolds(
            String userCode,
            String status,
            String currency,
            OffsetDateTime fromDate,
            OffsetDateTime toDate,
            Pageable pageable) {

        try {
            // Validate date range
            ServiceUtils.validateDateRange(fromDate, toDate);

            // Build specification based on filters
            Specification<WalletHold> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

            // Add user code filter if provided
            if (StringUtils.isNotBlank(userCode)) {
                log.debug("Filtering holds by user code: {}", userCode);
                spec = spec.and(WalletHoldSpecifications.hasUserCode(userCode));
            }

            // Add status filter if provided
            if (StringUtils.isNotBlank(status)) {
                try {
                    HoldStatus holdStatus = HoldStatus.valueOf(status.toUpperCase());
                    log.debug("Filtering holds by status: {}", holdStatus);
                    spec = spec.and(WalletHoldSpecifications.hasStatus(holdStatus));
                } catch (IllegalArgumentException ex) {
                    log.warn("Invalid status value provided: {}", status);
                    throw WalletException.badRequest(
                            String.format(WalletConstants.INVALID_STATUS, status));
                }
            }

            // Add currency filter if provided
            if (StringUtils.isNotBlank(currency)) {
                log.debug("Filtering holds by currency: {}", currency);
                spec = spec.and(WalletHoldSpecifications.hasCurrency(currency));
            }

            // Add date range filters if provided
            LocalDateTime fromDateTime = fromDate != null ? ServiceUtils.toLocalDateTime(fromDate) : null;
            LocalDateTime toDateTime = toDate != null ?
                    ServiceUtils.atEndOfDay(ServiceUtils.toLocalDateTime(toDate)) : null;

            if (fromDateTime != null) {
                log.debug("Filtering holds from date: {}", fromDateTime);
                spec = spec.and(WalletHoldSpecifications.createdAfter(fromDateTime));
            }

            if (toDateTime != null) {
                log.debug("Filtering holds to date: {}", toDateTime);
                spec = spec.and(WalletHoldSpecifications.createdBefore(toDateTime));
            }

            // Apply pagination and sorting with bounds checking
            Sort sort = pageable.getSortOr(Sort.by(Sort.Direction.DESC, "createdAt"));
            int pageSize = pageable.getPageSize();
            int requestedPage = pageable.getPageNumber();
            
            // First, get the total count to calculate total pages
            long totalElements = holdRepository.count((Specification<WalletHold>) spec);
            int totalPages = (int) Math.ceil((double) totalElements / pageSize);
            
            // Adjust page number if it's out of bounds
            int adjustedPage = requestedPage;
            if (totalPages > 0 && requestedPage >= totalPages) {
                adjustedPage = totalPages - 1; // Go to last page
            } else if (requestedPage < 0) {
                adjustedPage = 0; // Go to first page
            }
            
            // Create pageable with adjusted page number
            Pageable adjustedPageable = PageRequest.of(adjustedPage, pageSize, sort);
            
            // Get the page of results
            Page<WalletHold> holdsPage = holdRepository.findAll(spec, adjustedPageable);

            // Log the query details
            log.debug("Executing query: {}", holdsPage.toString());
            log.debug("Found {} items", holdsPage.getTotalElements());

            // Map the results to HoldResponse objects
            List<HoldResponse> responses = new ArrayList<>();
            for (WalletHold hold : holdsPage.getContent()) {
                try {
                    HoldResponse response = constructHoldResponse(hold);
                    if (response != null) {
                        responses.add(response);
                    }
                } catch (Exception e) {
                    log.error("Error mapping hold with ID {}: {}", hold.getId(), e.getMessage(), e);
                }
            }

            // Create a new page with the mapped responses
            return new PageImpl<>(responses, adjustedPageable, holdsPage.getTotalElements());

        } catch (WalletException ex) {
            log.error("Error listing holds: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error listing holds: {}", ex.getMessage(), ex);
            throw WalletException.internalServerError(WalletConstants.INTERNAL_SERVER_ERROR);
        }
    }

    public boolean validateHold(Long walletId, Double amount, String currency) {
        try {
            if (walletId == null || amount == null || StringUtils.isBlank(currency)) {
                log.warn(WalletConstants.LOG_HOLD_VALIDATION_FAILED, "Missing required parameters");
                return false;
            }
            
            Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> WalletException.notFound(
                    String.format(WalletConstants.WALLET_NOT_FOUND, walletId)));
                
            if (wallet.getWalletStatus() != HoldStatus.ACTIVE) {
                log.warn(WalletConstants.LOG_HOLD_VALIDATION_FAILED, "Wallet not active");
                return false;
            }
            
            if (!wallet.getCurrency().equals(currency)) {
                log.warn(WalletConstants.LOG_HOLD_VALIDATION_FAILED, "Currency mismatch");
                return false;
            }
            
            // Check if there's enough available balance
            boolean hasSufficientFunds = wallet.getAvailableBalance() >= amount;
            if (!hasSufficientFunds) {
                log.warn(WalletConstants.LOG_HOLD_VALIDATION_FAILED, "Insufficient funds");
            }
            return hasSufficientFunds;
            
        } catch (Exception ex) {
            log.error("Error validating hold: {}", ex.getMessage(), ex);
            return false;
        }
    }

    public BigDecimal getTotalHeldAmount(Long walletId) {
        try {
            Double totalHeld = holdRepository.sumPendingHoldsByWalletId(walletId);
            return BigDecimal.valueOf(totalHeld != null ? totalHeld : 0.0);
        } catch (Exception ex) {
            log.error("Error calculating total held amount: {}", ex.getMessage(), ex);
            return BigDecimal.ZERO;
        }
    }

}
