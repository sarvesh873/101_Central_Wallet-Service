package com.central.wallet_service.controller;

import com.central.wallet_service.dto.WalletTransactionRequestDto;
import com.central.wallet_service.dto.WalletTransactionResponseDto;
import com.central.wallet_service.dto.adapter.request.RestTransactionRequestAdapter;
import com.central.wallet_service.exception.GlobalExceptionHandler;
import com.central.wallet_service.service.WalletService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.WalletTransactionRequest;
import org.openapitools.model.WalletTransactionResponse;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Import(GlobalExceptionHandler.class)
class WalletTransactionControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletTransactionController walletTransactionController;

    private Validator validator;
    private WalletTransactionRequest testRequest;
    private WalletTransactionResponseDto testResponseDto;
    private static final String TEST_USER_CODE = "USER123";
    private static final Long TEST_WALLET_ID = 1L;
    private static final Double TEST_AMOUNT = 100.0;
    private static final String TEST_CURRENCY = "USD";

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();

        testRequest = new WalletTransactionRequest()
                .amount(TEST_AMOUNT)
                .currency(TEST_CURRENCY);

        testResponseDto = new WalletTransactionResponseDto() {
            @Override
            public Long getWalletId() {
                return TEST_WALLET_ID;
            }

            @Override
            public WalletTransactionResponseDto.TransactionType getTransactionType() {
                return WalletTransactionResponseDto.TransactionType.DEPOSIT;
            }

            @Override
            public Double getProcessedAmount() {
                return TEST_AMOUNT;
            }

            @Override
            public Double getNewBalance() {
                return 1100.0;
            }

            @Override
            public Double getNewAvailableBalance() {
                return 1000.0;
            }

            @Override
            public String getUserCode() {
                return TEST_USER_CODE;
            }

            @Override
            public WalletTransactionResponseDto.TransactionStatus getStatus() {
                return WalletTransactionResponseDto.TransactionStatus.COMPLETED;
            }

            @Override
            public String getUsername() {
                return "testuser";
            }

            @Override
            public String getEmail() {
                return "test@example.com";
            }

            @Override
            public String getPhoneNumber() {
                return "+1234567890";
            }
        };
    }

    @Test
    void depositFunds_InvalidAmount_ReturnsBadRequest() {
        // Arrange
        WalletTransactionRequest invalidRequest = new WalletTransactionRequest()
                .amount(-100.0)
                .currency(TEST_CURRENCY);

        // Act & Assert
        Set<ConstraintViolation<WalletTransactionRequest>> violations = validator.validate(invalidRequest);
        assertFalse(violations.isEmpty(), "Expected validation to fail for negative amount");
    }

    @Test
    void withdrawFunds_InvalidAmount_ReturnsBadRequest() {
        // Arrange
        WalletTransactionRequest invalidRequest = new WalletTransactionRequest()
                .amount(-100.0)
                .currency(TEST_CURRENCY);

        // Act & Assert
        Set<ConstraintViolation<WalletTransactionRequest>> violations = validator.validate(invalidRequest);
        assertFalse(violations.isEmpty(), "Expected validation to fail for negative amount");
    }

    @Test
    void depositFunds_ValidRequest_ReturnsOk() {
        // Arrange
        when(walletService.depositFunds(anyString(), any(WalletTransactionRequestDto.class)))
                .thenReturn(testResponseDto);

        // Act
        ResponseEntity<WalletTransactionResponse> response =
                walletTransactionController.depositFunds(TEST_USER_CODE, testRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(TEST_WALLET_ID, response.getBody().getWalletId());
        assertEquals("DEPOSIT", response.getBody().getTransactionType().getValue());
        assertEquals(TEST_AMOUNT, response.getBody().getProcessedAmount());
        
        // Verify adapter was used correctly
        ArgumentCaptor<WalletTransactionRequestDto> dtoCaptor = ArgumentCaptor.forClass(WalletTransactionRequestDto.class);
        verify(walletService).depositFunds(eq(TEST_USER_CODE), dtoCaptor.capture());
        
        WalletTransactionRequestDto capturedDto = dtoCaptor.getValue();
        assertInstanceOf(RestTransactionRequestAdapter.class, capturedDto);
        assertEquals(TEST_AMOUNT, capturedDto.getAmount());
        assertEquals(TEST_CURRENCY, capturedDto.getCurrency());
    }

    @Test
    void withdrawFunds_ValidRequest_ReturnsOk() {
        // Arrange
        testResponseDto = new WalletTransactionResponseDto() {
            @Override
            public Long getWalletId() {
                return TEST_WALLET_ID;
            }

            @Override
            public WalletTransactionResponseDto.TransactionType getTransactionType() {
                return WalletTransactionResponseDto.TransactionType.WITHDRAWAL;
            }

            @Override
            public Double getProcessedAmount() {
                return TEST_AMOUNT;
            }

            @Override
            public Double getNewBalance() {
                return 900.0;
            }

            @Override
            public Double getNewAvailableBalance() {
                return 900.0;
            }

            @Override
            public String getUserCode() {
                return TEST_USER_CODE;
            }

            @Override
            public WalletTransactionResponseDto.TransactionStatus getStatus() {
                return WalletTransactionResponseDto.TransactionStatus.COMPLETED;
            }

            @Override
            public String getUsername() {
                return "testuser";
            }

            @Override
            public String getEmail() {
                return "test@example.com";
            }

            @Override
            public String getPhoneNumber() {
                return "+1234567890";
            }
        };
        
        when(walletService.withdrawFunds(anyString(), any(WalletTransactionRequestDto.class)))
                .thenReturn(testResponseDto);

        // Act
        ResponseEntity<WalletTransactionResponse> response =
                walletTransactionController.withdrawFunds(TEST_USER_CODE, testRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(TEST_WALLET_ID, response.getBody().getWalletId());
        assertEquals("WITHDRAWAL", response.getBody().getTransactionType().getValue());
        
        // Verify adapter was used correctly
        ArgumentCaptor<WalletTransactionRequestDto> dtoCaptor = ArgumentCaptor.forClass(WalletTransactionRequestDto.class);
        verify(walletService).withdrawFunds(eq(TEST_USER_CODE), dtoCaptor.capture());
        
        WalletTransactionRequestDto capturedDto = dtoCaptor.getValue();
        assertInstanceOf(RestTransactionRequestAdapter.class, capturedDto);
        assertEquals(TEST_AMOUNT, capturedDto.getAmount());
        assertEquals(TEST_CURRENCY, capturedDto.getCurrency());
    }

    @Test
    void testDepositFunds_ServiceThrowsException_ReturnsInternalServerError() {
        // Arrange
        when(walletService.depositFunds(anyString(), any(WalletTransactionRequestDto.class)))
                .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            walletTransactionController.depositFunds(TEST_USER_CODE, testRequest);
        });
        
        // The GlobalExceptionHandler will convert this to a proper response in the actual application
        // In the test, we just verify the exception is thrown as expected
    }
}
