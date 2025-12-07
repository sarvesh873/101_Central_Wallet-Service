package com.central.wallet_service.grpc;

import com.central.wallet.*;
import com.central.wallet.WalletServiceGrpc;
import com.central.wallet_service.exception.DuplicateTransactionException;
import com.central.wallet_service.exception.InsufficientFundsException;
import com.central.wallet_service.exception.WalletNotFoundException;
import com.central.wallet_service.service.WalletService;
import com.google.protobuf.Timestamp;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class GrpcWalletServiceImpl extends WalletServiceGrpc.WalletServiceImplBase {

    private final WalletService walletService;
    
    private static final String INVALID_REQUEST = "Invalid request parameters";
    private static final String INTERNAL_ERROR = "Internal server error";
    private static final String WALLET_NOT_FOUND = "Wallet not found";
    private static final String INSUFFICIENT_FUNDS = "Insufficient funds";
    private static final String DUPLICATE_TRANSACTION = "Duplicate transaction";
    private static final String INVALID_AMOUNT = "Amount must be greater than zero";

    @Override
    public void createWallet(WalletCreateRequestGRPC request, StreamObserver<WalletResponseGRPC> responseObserver) {
        try {
            // Validate request
            if (request == null || request.getUserCode().isBlank() || request.getCurrency().isBlank()) {
                handleError(responseObserver, Code.INVALID_ARGUMENT, INVALID_REQUEST);
                return;
            }

            // Create wallet using the service
            org.openapitools.model.WalletCreateRequest createRequest = new org.openapitools.model.WalletCreateRequest()
                .userCode(request.getUserCode())
                .currency(request.getCurrency());

            org.openapitools.model.WalletResponse response = walletService.createWallet(createRequest);

            // Convert to gRPC response
            WalletResponseGRPC.Builder builder = WalletResponseGRPC.newBuilder()
                    .setWalletId(response.getWalletId())
                    .setUserCode(response.getUserCode())
                    .setBalance(response.getBalance())
                    .setAvailableBalance(response.getAvailableBalance())
                    .setCurrency(response.getCurrency())
                    .setStatus(response.getStatus())
                    .setCreatedAt(convertToTimestamp(response.getCreatedAt()));

            // Safely set optional fields
            if (response.getUsername() != null) {
                builder.setUsername(response.getUsername());
            }
            if (response.getEmail() != null) {
                builder.setEmail(response.getEmail());
            }
            if (response.getPhoneNumber() != null) {
                builder.setPhoneNumber(response.getPhoneNumber());
            }

            WalletResponseGRPC grpcResponse = builder.build();

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
    public void getUserWallet(GetWalletRequestGRPC request, StreamObserver<WalletResponseGRPC> responseObserver) {
        try {
            // Validate request
            if (request == null || request.getUserCode().isBlank()) {
                handleError(responseObserver, Code.INVALID_ARGUMENT, INVALID_REQUEST);
                return;
            }

            // Get wallet using the service
            org.openapitools.model.WalletResponse response = walletService.getWalletByUserCode(request.getUserCode());

            // Convert to gRPC response
            WalletResponseGRPC grpcResponse = WalletResponseGRPC.newBuilder()
                .setWalletId(response.getWalletId())
                .setUserCode(response.getUserCode())
                .setBalance(response.getBalance())
                .setAvailableBalance(response.getAvailableBalance())
                .setCurrency(response.getCurrency())
                .setStatus(response.getStatus())
                .setUsername(response.getUsername())
                .setEmail(response.getEmail())
                .setPhoneNumber(response.getPhoneNumber())
                .setCreatedAt(convertToTimestamp(response.getCreatedAt()))
                .build();

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

            // Create transaction request
            org.openapitools.model.WalletTransactionRequest transactionRequest = 
                new org.openapitools.model.WalletTransactionRequest()
                    .amount(request.getAmount())
                    .currency(request.getCurrency());

            // Process deposit using the service
            org.openapitools.model.WalletTransactionResponse response = 
                walletService.depositFunds(request.getUserCode(), transactionRequest);

            // Convert to gRPC response
            WalletTransactionResponseGRPC grpcResponse = buildTransactionResponse(response);

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

            // Create transaction request
            org.openapitools.model.WalletTransactionRequest transactionRequest = 
                new org.openapitools.model.WalletTransactionRequest()
                    .amount(request.getAmount())
                    .currency(request.getCurrency());

            // Process withdrawal using the service
            org.openapitools.model.WalletTransactionResponse response = 
                walletService.withdrawFunds(request.getUserCode(), transactionRequest);

            // Convert to gRPC response
            WalletTransactionResponseGRPC grpcResponse = buildTransactionResponse(response);

            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();

        } catch (WalletNotFoundException e) {
            log.error("Wallet not found for withdrawal: {}", e.getMessage(), e);
            handleError(responseObserver, Code.NOT_FOUND, WALLET_NOT_FOUND);
        } catch (InsufficientFundsException e) {
            log.warn("Insufficient funds for withdrawal: {}", e.getMessage());
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
    private Timestamp convertToTimestamp(OffsetDateTime offsetDateTime) {
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
     * Builds a transaction response from the service response
     */
    private WalletTransactionResponseGRPC buildTransactionResponse(
            org.openapitools.model.WalletTransactionResponse response) {
        return WalletTransactionResponseGRPC.newBuilder()
            .setWalletId(response.getWalletId())
            .setTransactionType(WalletTransactionResponseGRPC.TransactionTypeGRPC.valueOf(
                response.getTransactionType().getValue()))
            .setProcessedAmount(response.getProcessedAmount())
            .setNewBalance(response.getNewBalance())
            .setNewAvailableBalance(response.getNewAvailableBalance())
            .setUserCode(response.getUserCode())
            .setStatus(WalletTransactionResponseGRPC.TransactionStatusGRPC.valueOf(
                response.getStatus().getValue()))
            .setUsername(response.getUsername())
            .setEmail(response.getEmail())
            .setPhoneNumber(response.getPhoneNumber())
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
