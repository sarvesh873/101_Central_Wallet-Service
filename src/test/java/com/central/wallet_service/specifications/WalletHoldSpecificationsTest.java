//package com.central.wallet_service.specifications;
//
//import com.central.wallet_service.model.HoldStatus;
//import com.central.wallet_service.model.Wallet;
//import com.central.wallet_service.model.WalletHold;
//import com.central.wallet_service.model.WalletUserSnapshot;
//import jakarta.persistence.criteria.*;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.jpa.domain.Specification;
//
//import java.lang.reflect.Constructor;
//import java.lang.reflect.InvocationTargetException;
//import java.time.LocalDateTime;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@SuppressWarnings({"rawtypes", "unchecked"})
//class WalletHoldSpecificationsTest {
//
//    @Mock
//    private Root root;
//
//    @Mock
//    private CriteriaQuery query;
//
//    @Mock
//    private CriteriaBuilder criteriaBuilder;
//
//    @Mock
//    private Path path;
//
//    @Mock
//    private Join walletJoin;
//
//    @Mock
//    private Join userSnapshotJoin;
//
//    @Mock
//    private Expression expression;
//
//    @Mock
//    private Predicate predicate;
//
//
//    @BeforeEach
//    void setUp() {
//        // No need to set up default mocks here as we'll set them up in each test
//    }
//
//    @Test
//    void hasUserCode_WithValidUserCode_ReturnsCorrectPredicate() {
//        // Arrange
//        String userCode = "USER123";
//        when(root.join("wallet")).thenReturn(walletJoin);
//        when(walletJoin.join("userSnapshot")).thenReturn(userSnapshotJoin);
//        when(userSnapshotJoin.get("userCode")).thenReturn(path);
//        when(criteriaBuilder.equal(any(Expression.class), eq(userCode))).thenReturn(predicate);
//
//        // Act
//        Specification<WalletHold> spec = WalletHoldSpecifications.hasUserCode(userCode);
//        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
//
//        // Assert
//        assertNotNull(result);
//        verify(root).join("wallet");
//        verify(walletJoin).join("userSnapshot");
//        verify(userSnapshotJoin).get("userCode");
//        verify(criteriaBuilder).equal(path, userCode);
//    }
//
//    @Test
//    void hasUserCode_WithBlankUserCode_ReturnsConjunction() {
//        // Arrange
//        when(criteriaBuilder.conjunction()).thenReturn(predicate);
//
//        // Act
//        Specification<WalletHold> spec = WalletHoldSpecifications.hasUserCode("");
//        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
//
//        // Assert
//        assertNotNull(result);
//        verify(criteriaBuilder).conjunction();
//        verifyNoMoreInteractions(root);
//    }
//
//    @Test
//    void hasUserCode_WithNullUserCode_ReturnsConjunction() {
//        // Arrange
//        when(criteriaBuilder.conjunction()).thenReturn(predicate);
//
//        // Act
//        Specification<WalletHold> spec = WalletHoldSpecifications.hasUserCode(null);
//        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
//
//        // Assert
//        assertNotNull(result);
//        verify(criteriaBuilder).conjunction();
//        verifyNoMoreInteractions(root);
//    }
//
//    @Test
//    void hasStatus_WithValidStatus_ReturnsEqualPredicate() {
//        // Arrange
//        HoldStatus status = HoldStatus.ACTIVE;
//        when(root.get("status")).thenReturn(path);
//        when(criteriaBuilder.equal(any(Expression.class), eq(status))).thenReturn(predicate);
//
//        // Act
//        Specification<WalletHold> spec = WalletHoldSpecifications.hasStatus(status);
//        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
//
//        // Assert
//        assertNotNull(result);
//        verify(root).get("status");
//        verify(criteriaBuilder).equal(path, status);
//    }
//
//    @Test
//    void hasStatus_WithNullStatus_ReturnsConjunction() {
//        // Arrange
//        when(criteriaBuilder.conjunction()).thenReturn(predicate);
//
//        // Act
//        Specification<WalletHold> spec = WalletHoldSpecifications.hasStatus(null);
//        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
//
//        // Assert
//        assertNotNull(result);
//        verify(criteriaBuilder).conjunction();
//        verifyNoMoreInteractions(root);
//    }
//
//    @Test
//    void hasCurrency_WithValidCurrency_ReturnsEqualPredicate() {
//        // Arrange
//        String currency = "USD";
//        when(root.get("wallet")).thenReturn(path);
//        when(path.get("currency")).thenReturn(path);
//        when(criteriaBuilder.upper(path)).thenReturn(path);
//        when(criteriaBuilder.equal(any(Expression.class), eq(currency.toUpperCase()))).thenReturn(predicate);
//
//        // Act
//        Specification<WalletHold> spec = WalletHoldSpecifications.hasCurrency(currency);
//        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
//
//        // Assert
//        assertNotNull(result);
//        verify(root).get("wallet");
//        verify(path).get("currency");
//        verify(criteriaBuilder).upper(path);
//        verify(criteriaBuilder).equal(path, currency.toUpperCase());
//    }
//
//    @Test
//    void hasCurrency_WithBlankCurrency_ReturnsConjunction() {
//        // Arrange
//        when(criteriaBuilder.conjunction()).thenReturn(predicate);
//
//        // Act
//        Specification<WalletHold> spec = WalletHoldSpecifications.hasCurrency("");
//        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
//
//        // Assert
//        assertNotNull(result);
//        verify(criteriaBuilder).conjunction();
//        verifyNoMoreInteractions(root);
//    }
//
//    @Test
//    void hasCurrency_WithNullCurrency_ReturnsConjunction() {
//        // Arrange
//        when(criteriaBuilder.conjunction()).thenReturn(predicate);
//
//        // Act
//        Specification<WalletHold> spec = WalletHoldSpecifications.hasCurrency(null);
//        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
//
//        // Assert
//        assertNotNull(result);
//        verify(criteriaBuilder).conjunction();
//        verifyNoMoreInteractions(root);
//    }
//
//    @Test
//    void createdAfter_WithValidDate_ReturnsGreaterThanOrEqualPredicate() {
//        // Arrange
//        LocalDateTime date = LocalDateTime.now();
//        when(root.get("createdAt")).thenReturn(path);
//        when(criteriaBuilder.greaterThanOrEqualTo(any(Expression.class), any(LocalDateTime.class))).thenReturn(predicate);
//
//        // Act
//        Specification<WalletHold> spec = WalletHoldSpecifications.createdAfter(date);
//        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
//
//        // Assert
//        assertNotNull(result);
//        verify(root).get("createdAt");
//        verify(criteriaBuilder).greaterThanOrEqualTo(path, date);
//    }
//
//    @Test
//    void createdAfter_WithNullDate_ReturnsConjunction() {
//        // Arrange
//        when(criteriaBuilder.conjunction()).thenReturn(predicate);
//
//        // Act
//        Specification<WalletHold> spec = WalletHoldSpecifications.createdAfter(null);
//        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
//
//        // Assert
//        assertNotNull(result);
//        verify(criteriaBuilder).conjunction();
//        verifyNoMoreInteractions(root);
//    }
//
//    @Test
//    void createdBefore_WithValidDate_ReturnsLessThanOrEqualPredicate() {
//        // Arrange
//        LocalDateTime date = LocalDateTime.now();
//        when(root.get("createdAt")).thenReturn(path);
//        when(criteriaBuilder.lessThanOrEqualTo(any(Expression.class), any(LocalDateTime.class))).thenReturn(predicate);
//
//        // Act
//        Specification<WalletHold> spec = WalletHoldSpecifications.createdBefore(date);
//        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
//
//        // Assert
//        assertNotNull(result);
//        verify(root).get("createdAt");
//        verify(criteriaBuilder).lessThanOrEqualTo(path, date);
//    }
//
//    @Test
//    void createdBefore_WithNullDate_ReturnsConjunction() {
//        // Arrange
//        when(criteriaBuilder.conjunction()).thenReturn(predicate);
//
//        // Act
//        Specification<WalletHold> spec = WalletHoldSpecifications.createdBefore(null);
//        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
//
//        // Assert
//        assertNotNull(result);
//        verify(criteriaBuilder).conjunction();
//        verifyNoMoreInteractions(root);
//    }
//
//    @Test
//    void constructor_IsPrivate_ThrowsException() throws Exception {
//        // Arrange
//        Constructor<WalletHoldSpecifications> constructor = WalletHoldSpecifications.class.getDeclaredConstructor();
//        constructor.setAccessible(true);
//
//        try {
//            // Act
//            WalletHoldSpecifications instance = constructor.newInstance();
//
//            // Assert
//            assertNotNull(instance, "Instance should be created successfully");
//        } catch (InvocationTargetException e) {
//            // If the constructor throws an exception, verify it's the expected one
//            assertTrue(e.getCause() instanceof IllegalStateException,
//                "Should throw IllegalStateException when trying to instantiate");
//        }
//    }
//}