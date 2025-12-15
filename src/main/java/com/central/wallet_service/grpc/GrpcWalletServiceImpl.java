package com.central.wallet_service.grpc;

import com.central.wallet.*;
import com.central.wallet.WalletServiceGrpc;
import com.central.wallet_service.dto.WalletCreateRequestDto;
import com.central.wallet_service.dto.WalletResponseDto;
import com.central.wallet_service.dto.WalletTransactionRequestDto;
import com.central.wallet_service.dto.WalletTransactionResponseDto;
import com.central.wallet_service.dto.adapter.request.GrpcWalletRequestAdapter;
import com.central.wallet_service.dto.adapter.request.GrpcTransactionRequestAdapter;
import com.central.wallet_service.dto.adapter.response.GrpcTransactionResponseAdapter;
import com.central.wallet_service.dto.adapter.response.GrpcWalletResponseAdapter;
import com.central.wallet_service.exception.DuplicateTransactionException;
import com.central.wallet_service.exception.InsufficientFundsException;
import com.central.wallet_service.exception.WalletNotFoundException;
import com.central.wallet_service.service.WalletService;
import com.google.protobuf.Timestamp;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@GrpcService
@RequiredArgsConstructor
@Transactional
public class GrpcWalletServiceImpl extends WalletServiceGrpc.WalletServiceImplBase {

    private final WalletService walletService;
    
    private static final String INVALID_REQUEST = "Invalid request parameters";
    private static final String INTERNAL_ERROR = "Internal server error";
    private static final String WALLET_NOT_FOUND = "Wallet not found";
    private static final String INSUFFICIENT_FUNDS = "Insufficient funds";
    private static final String DUPLICATE_TRANSACTION = "Duplicate transaction";
    private static final String INVALID_AMOUNT = "Amount must be greater than zero";

    @Override
    @Transactional
    public void createWallet(WalletCreateRequestGRPC request, StreamObserver<WalletResponseGRPC> responseObserver) {
        try {
            // Validate request
            if (request == null || request.getUserCode().isBlank() || request.getCurrency().isBlank()) {
                handleError(responseObserver, Code.INVALID_ARGUMENT, INVALID_REQUEST);
                return;
            }

            // Convert gRPC request to DTO using adapter
            WalletCreateRequestDto createRequest = new GrpcWalletRequestAdapter(request);
            
            // Call service
            WalletResponseDto response = walletService.createWallet(createRequest);
            
            // Convert DTO to gRPC response using adapter
            WalletResponseGRPC grpcResponse = new GrpcWalletResponseAdapter(response).toGrpcResponse();

            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();

        } catch (DataIntegrityViolationException e) {
            log.error("Error creating wallet - data integrity violation: {}", e.getMessage(), e);
            handleError(responseObserver, Code.ALREADY_EXISTS, "Wallet already exists for this user");
        } catch (IllegalArgumentException e) {
            log.error("Invalid request to create wallet: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INVALID_ARGUMENT, e.getMessage());
        } catch (Exception e) {
            log.error("Error creating wallet: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INTERNAL, INTERNAL_ERROR);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getUserWallet(GetWalletRequestGRPC request, StreamObserver<WalletResponseGRPC> responseObserver) {
        try {
            // Validate request
            if (request == null || request.getUserCode().isBlank()) {
                handleError(responseObserver, Code.INVALID_ARGUMENT, INVALID_REQUEST);
                return;
            }

            // Get wallet using the service
            WalletResponseDto response = walletService.getWalletByUserCode(request.getUserCode());
            
            // Convert DTO to gRPC response using adapter
            WalletResponseGRPC grpcResponse = new GrpcWalletResponseAdapter(response).toGrpcResponse();

            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();

        } catch (WalletNotFoundException e) {
            log.warn("Wallet not found for user: {}", request != null ? request.getUserCode() : "null");
            handleError(responseObserver, Code.NOT_FOUND, WALLET_NOT_FOUND);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request to get wallet: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INVALID_ARGUMENT, e.getMessage());
        } catch (Exception e) {
            log.error("Error retrieving wallet: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INTERNAL, INTERNAL_ERROR);
        }
    }

    @Override
    public void depositFunds(WalletTransactionRequestGRPC request, StreamObserver<WalletTransactionResponseGRPC> responseObserver) {
        try {
            // Validate request
            if (request == null || request.getUserCode().isBlank() || request.getAmount() <= 0) {
                handleError(responseObserver, Code.INVALID_ARGUMENT, 
                    request == null || request.getAmount() <= 0 ? INVALID_AMOUNT : INVALID_REQUEST);
                return;
            }

            // Convert gRPC request to DTO using adapter
            WalletTransactionRequestDto requestDto = new GrpcTransactionRequestAdapter(request);

            // Process deposit using the service
            WalletTransactionResponseDto response = walletService.depositFunds(request.getUserCode(), requestDto);

            // Convert DTO to gRPC response using adapter
            WalletTransactionResponseGRPC grpcResponse = new GrpcTransactionResponseAdapter(response).toGrpcResponse();

            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();

        } catch (WalletNotFoundException e) {
            log.error("Wallet not found for deposit: {}", e.getMessage(), e);
            handleError(responseObserver, Code.NOT_FOUND, WALLET_NOT_FOUND);
        } catch (DuplicateTransactionException e) {
            log.error("Duplicate transaction detected: {}", e.getMessage(), e);
            handleError(responseObserver, Code.ALREADY_EXISTS, DUPLICATE_TRANSACTION);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Invalid deposit request: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INVALID_ARGUMENT, e.getMessage());
        } catch (Exception e) {
            log.error("Error processing deposit: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INTERNAL, INTERNAL_ERROR);
        }
    }

    @Override
    public void withdrawFunds(WalletTransactionRequestGRPC request, StreamObserver<WalletTransactionResponseGRPC> responseObserver) {
        try {
            // Validate request
            if (request == null || request.getUserCode().isBlank() || request.getAmount() <= 0) {
                handleError(responseObserver, Code.INVALID_ARGUMENT, 
                    request == null || request.getAmount() <= 0 ? INVALID_AMOUNT : INVALID_REQUEST);
                return;
            }

            // Convert gRPC request to DTO using adapter
            WalletTransactionRequestDto requestDto = new GrpcTransactionRequestAdapter(request);

            // Process withdrawal using the service
            WalletTransactionResponseDto response = walletService.withdrawFunds(request.getUserCode(), requestDto);

            // Convert DTO to gRPC response using adapter
            WalletTransactionResponseGRPC grpcResponse = new GrpcTransactionResponseAdapter(response).toGrpcResponse();

            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();

        } catch (WalletNotFoundException e) {
            log.error("Wallet not found for withdrawal: {}", e.getMessage(), e);
            handleError(responseObserver, Code.NOT_FOUND, WALLET_NOT_FOUND);
        } catch (InsufficientFundsException e) {
            log.error("Insufficient funds for withdrawal: {}", e.getMessage(), e);
            handleError(responseObserver, Code.FAILED_PRECONDITION, INSUFFICIENT_FUNDS);
        } catch (DuplicateTransactionException e) {
            log.error("Duplicate transaction detected: {}", e.getMessage(), e);
            handleError(responseObserver, Code.ALREADY_EXISTS, DUPLICATE_TRANSACTION);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Invalid withdrawal request: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INVALID_ARGUMENT, e.getMessage());
        } catch (Exception e) {
            log.error("Error processing withdrawal: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INTERNAL, INTERNAL_ERROR);
        }
    }

    // Helper Methods

    /**
     * Converts OffsetDateTime to protobuf Timestamp
     */
    private static Timestamp convertToTimestamp(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return Timestamp.getDefaultInstance();
        }
        Instant instant = offsetDateTime.toInstant();
        return Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();
    }

    /**
     * Handles gRPC error responses
     */
    private <T> void handleError(StreamObserver<T> responseObserver, Code code, String message) {
        Status status = Status.newBuilder()
            .setCode(code.getNumber())
            .setMessage(message)
            .build();
        responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }

}
