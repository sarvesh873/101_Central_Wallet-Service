package com.central.wallet_service.grpc;

import com.central.wallet.*;
import com.central.wallet_service.dto.*;
import com.central.wallet_service.dto.adapter.request.*;
import com.central.wallet_service.exception.*;
import com.central.wallet_service.service.HoldService;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.openapitools.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.stream.Collectors;
import com.central.wallet_service.utils.ServiceUtils;
import static com.central.wallet_service.utils.ServiceUtils.*;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class GrpcHoldServiceImpl extends HoldServiceGrpc.HoldServiceImplBase {

    private final HoldService holdService;
    
    private static final String INVALID_REQUEST = "Invalid request parameters";
    private static final String INTERNAL_ERROR = "Internal server error";
    private static final String HOLD_NOT_FOUND = "Hold not found";
    private static final String WALLET_NOT_FOUND = "Wallet not found";
    private static final String DUPLICATE_TRANSACTION = "Duplicate transaction";
    private static final String INVALID_AMOUNT = "Amount must be greater than zero";
    private static final String HOLD_EXPIRED = "Hold has expired";
    private static final String HOLD_NOT_ACTIVE = "Hold is not in active state";
    private static final String HOLD_ALREADY_CAPTURED = "Hold has already been captured";

    @Override
    public void placeHold(PlaceHoldRequestGRPC request, StreamObserver<HoldResponseGRPC> responseObserver) {
        try {
            // Validate request
            if (request == null || request.getUserCode().isBlank() || request.getAmount() <= 0) {
                handleError(responseObserver, Code.INVALID_ARGUMENT, 
                    request == null || request.getAmount() <= 0 ? INVALID_AMOUNT : INVALID_REQUEST);
                return;
            }

            // Create hold request
            PlaceHoldRequestDto requestDto = new GrpcPlaceHoldRequestAdapter(request);
            HoldResponseDto responseDto = holdService.placeHold(requestDto);

            // Convert and send response
            responseObserver.onNext(convertToHoldResponseGRPC(responseDto));
            responseObserver.onCompleted();

        } catch (WalletNotFoundException e) {
            log.error("Wallet not found for hold: {}", e.getMessage(), e);
            handleError(responseObserver, Code.NOT_FOUND, WALLET_NOT_FOUND);
        } catch (DataIntegrityViolationException e) {
            log.error("Duplicate hold detected: {}", e.getMessage(), e);
            handleError(responseObserver, Code.ALREADY_EXISTS, DUPLICATE_TRANSACTION);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Invalid hold request: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INVALID_ARGUMENT, e.getMessage());
        } catch (InsufficientFundsException e) {
            log.error("Insufficient funds for adjustment: {}", e.getMessage(), e);
            handleError(responseObserver, Code.FAILED_PRECONDITION, "Insufficient funds");
        }catch (Exception e) {
            log.error("Error placing hold: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INTERNAL, INTERNAL_ERROR);
        }
    }

    @Override
    public void captureHold(CaptureHoldRequestGRPC request, StreamObserver<HoldResponseGRPC> responseObserver) {
        try {
            if (request == null || request.getHoldId().isBlank()) {
                handleError(responseObserver, Code.INVALID_ARGUMENT, INVALID_REQUEST);
                return;
            }

            CaptureRequestDto requestDto = new GrpcCaptureRequestAdapter(request);
            HoldResponseDto responseDto = holdService.captureHoldFunds(requestDto);

            responseObserver.onNext(convertToHoldResponseGRPC(responseDto));
            responseObserver.onCompleted();

        } catch (HoldNotFoundException e) {
            log.error("Hold not found for capture: {}", e.getMessage(), e);
            handleError(responseObserver, Code.NOT_FOUND, HOLD_NOT_FOUND);
        } catch (IllegalStateException e) {
            log.error("Invalid hold state for capture: {}", e.getMessage(), e);
            handleError(responseObserver, Code.FAILED_PRECONDITION, 
                e.getMessage().contains("expired") ? HOLD_EXPIRED : HOLD_NOT_ACTIVE);
        } catch (InsufficientFundsException e) {
            log.error("Insufficient funds for adjustment: {}", e.getMessage(), e);
            handleError(responseObserver, Code.FAILED_PRECONDITION, "Insufficient funds");
        } catch (Exception e) {
            log.error("Error capturing hold: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INTERNAL, INTERNAL_ERROR);
        }
    }

    @Override
    public void releaseHold(ReleaseHoldRequestGRPC request, StreamObserver<HoldResponseGRPC> responseObserver) {
        try {
            if (request == null || request.getHoldId().isBlank()) {
                handleError(responseObserver, Code.INVALID_ARGUMENT, INVALID_REQUEST);
                return;
            }

            ReleaseHoldRequestDto requestDto = new GrpcReleaseHoldRequestAdapter(request);
            HoldResponseDto responseDto = holdService.releaseHold(request.getHoldId(), requestDto);

            responseObserver.onNext(convertToHoldResponseGRPC(responseDto));
            responseObserver.onCompleted();

        }
        catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("already been captured")) {
                handleError(responseObserver, Code.FAILED_PRECONDITION, HOLD_ALREADY_CAPTURED);
            } else {
                handleError(responseObserver, Code.FAILED_PRECONDITION, e.getMessage());
            }
        }catch (HoldNotFoundException e) {
            log.error("Hold not found for release: {}", e.getMessage(), e);
            handleError(responseObserver, Code.NOT_FOUND, HOLD_NOT_FOUND);
        } catch (Exception e) {
            log.error("Error releasing hold: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INTERNAL, INTERNAL_ERROR);
        }
    }

    @Override
    public void getHold(GetHoldRequestGRPC request, StreamObserver<HoldResponseGRPC> responseObserver) {
        try {
            if (request == null || request.getHoldId().isBlank()) {
                handleError(responseObserver, Code.INVALID_ARGUMENT, INVALID_REQUEST);
                return;
            }

            HoldResponseDto responseDto = holdService.getHold(request.getHoldId());
            responseObserver.onNext(convertToHoldResponseGRPC(responseDto));
            responseObserver.onCompleted();

        } catch (HoldNotFoundException e) {
            log.error("Hold not found: {}", e.getMessage(), e);
            handleError(responseObserver, Code.NOT_FOUND, HOLD_NOT_FOUND);
        } catch (Exception e) {
            log.error("Error retrieving hold: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INTERNAL, INTERNAL_ERROR);
        }
    }

    @Override
    public void listHolds(ListHoldsRequestGRPC request, StreamObserver<ListHoldsResponseGRPC> responseObserver) {
        try {
            if (request == null || request.getUserCode().isBlank()) {
                handleError(responseObserver, Code.INVALID_ARGUMENT, INVALID_REQUEST);
                return;
            }

            // Convert pagination
            int page = request.hasPagination() ? request.getPagination().getPage() : 0;
            int size = request.hasPagination() ? request.getPagination().getPageSize() : 20;
            String sortBy = request.hasPagination() ? request.getPagination().getSortBy() : "createdAt";
            boolean sortDesc = request.hasPagination() && request.getPagination().getSortDescending();

            Pageable pageable = PageRequest.of(
                page,
                size,
                sortDesc ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending()
            );

            // Call service
            Page<HoldResponseDto> holdsPage = holdService.listHolds(
                request.getUserCode(),
                request.getStatus(),
                request.getCurrency(),
                request.hasFromDate() ? toOffsetDateTime(request.getFromDate()) : null,
                request.hasToDate() ? toOffsetDateTime(request.getToDate()) : null,
                pageable
            );

            // Convert and send response
            ListHoldsResponseGRPC response = ListHoldsResponseGRPC.newBuilder()
                .addAllItems(holdsPage.getContent().stream()
                    .map(ServiceUtils::convertToHoldResponseGRPC)
                    .collect(Collectors.toList()))
                .setPagination(PaginationResponseGRPC.newBuilder()
                    .setCurrentPage(holdsPage.getNumber() + 1)
                    .setPageSize(holdsPage.getSize())
                    .setTotalItems(holdsPage.getTotalElements())
                    .setTotalPages(holdsPage.getTotalPages())
                    .setHasNext(holdsPage.hasNext())
                    .setHasPrevious(holdsPage.hasPrevious())
                    .build())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error listing holds: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INTERNAL, INTERNAL_ERROR);
        }
    }

    @Override
    public void extendHold(ExtendHoldRequestGRPC request, StreamObserver<HoldResponseGRPC> responseObserver) {
        try {
            if (request == null || request.getHoldId().isBlank() || !request.hasNewExpiresAt()) {
                handleError(responseObserver, Code.INVALID_ARGUMENT, INVALID_REQUEST);
                return;
            }

            ExtendHoldRequestDto requestDto = new GrpcExtendHoldRequestAdapter(request);
            HoldResponseDto responseDto = holdService.extendHold(request.getHoldId(), requestDto);
            responseObserver.onNext(convertToHoldResponseGRPC(responseDto));
            responseObserver.onCompleted();

        } catch (HoldNotFoundException e) {
            log.error("Hold not found for extension: {}", e.getMessage(), e);
            handleError(responseObserver, Code.NOT_FOUND, HOLD_NOT_FOUND);
        } catch (Exception e) {
            log.error("Error extending hold: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INTERNAL, INTERNAL_ERROR);
        }
    }

    @Override
    public void adjustHold(AdjustHoldRequestGRPC request, StreamObserver<HoldResponseGRPC> responseObserver) {
        try {
            if (request == null || request.getHoldId().isBlank() || request.getNewAmount() <= 0) {
                handleError(responseObserver, Code.INVALID_ARGUMENT, 
                    request == null || request.getNewAmount() <= 0 ? INVALID_AMOUNT : INVALID_REQUEST);
                return;
            }

            AdjustHoldRequestDto requestDto = new GrpcAdjustHoldRequestAdapter(request);
            HoldResponseDto responseDto = holdService.adjustHold(request.getHoldId(), requestDto);

            responseObserver.onNext(convertToHoldResponseGRPC(responseDto));
            responseObserver.onCompleted();

        } catch (HoldNotFoundException e) {
            log.error("Hold not found for adjustment: {}", e.getMessage(), e);
            handleError(responseObserver, Code.NOT_FOUND, HOLD_NOT_FOUND);
        } catch (IllegalStateException e) {
            log.error("Invalid hold state for adjustment: {}", e.getMessage(), e);
            handleError(responseObserver, Code.FAILED_PRECONDITION, e.getMessage());
        } catch (InsufficientFundsException e) {
            log.error("Insufficient funds for adjustment: {}", e.getMessage(), e);
            handleError(responseObserver, Code.FAILED_PRECONDITION, "Insufficient funds");
        } catch (Exception e) {
            log.error("Error adjusting hold: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INTERNAL, INTERNAL_ERROR);
        }
    }

    private <T> void handleError(StreamObserver<T> responseObserver, Code code, String message) {
        Status status = Status.newBuilder()
            .setCode(code.getNumber())
            .setMessage(message)
            .build();
        responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
}
