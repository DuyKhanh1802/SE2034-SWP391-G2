package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.*;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.CashTransaction;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.InventoryReceipt;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Payment;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.CashTransactionCreateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.CashTransactionRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.CashTransactionListResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.CashTransactionResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.CashTransactionRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.InventoryReceiptRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CashTransactionService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<CashTransactionCategory> PAYMENT_CATEGORIES = List.of(
            CashTransactionCategory.DEPOSIT,
            CashTransactionCategory.BOOKING_PAYMENT,
            CashTransactionCategory.REFUND
    );

    private final CashTransactionRepository cashTransactionRepository;
    private final InventoryReceiptRepository inventoryReceiptRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public List<CashTransaction> getRecentTransactions(int limit) {
        return cashTransactionRepository.findByOrderByCreatedAtDesc(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public CashTransaction getTransaction(Long id) {
        return cashTransactionRepository.findDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dòng tiền."));
    }

    @Transactional(readOnly = true)
    public InventoryReceipt getInventoryReceiptForTransaction(CashTransaction transaction) {
        if (transaction == null
                || transaction.getCategory() != CashTransactionCategory.INVENTORY_PURCHASE
                || transaction.getSourceId() == null) {
            return null;
        }

        return inventoryReceiptRepository.findDetailById(transaction.getSourceId()).orElse(null);
    }

    @Transactional(readOnly = true)
    public CashTransactionListResponse getCashTransactionListResponse(CashTransactionRequest request) {
        if (request == null) {
            request = new CashTransactionRequest();
        }

        int page = request.getPage();
        int size = request.getSize();

        if (page < 0) {
            page = 0;
        }

        if (size <= 0) {
            size = 10;
        }

        // Chuyển entity sang response để màn list chỉ nhận dữ liệu cần hiển thị.
        List<CashTransaction> filteredTransactions = searchTransactions(
                request.getType(),
                request.getCategory(),
                request.getPaymentMethod(),
                request.getFromDate(),
                request.getToDate(),
                request.getKeyword()
        );
        Map<Long, String> paymentMethodMap = getPaymentMethodMap(filteredTransactions);

        List<CashTransactionResponse> allTransactions = filteredTransactions
                .stream()
                .map(transaction -> toResponse(transaction, paymentMethodMap))
                .toList();

        int totalTransactions = allTransactions.size();
        int totalPages = (int) Math.ceil((double) totalTransactions / size);

        if (totalPages > 0 && page >= totalPages) {
            page = totalPages - 1;
        }

        List<CashTransactionResponse> pagedTransactions = getPagedTransactions(allTransactions, page, size);

        return new CashTransactionListResponse(
                pagedTransactions,
                totalTransactions,
                page,
                totalPages,
                size,
                page > 0,
                page < totalPages - 1
        );
    }

    @Transactional(readOnly = true)
    public List<CashTransaction> searchTransactions(String type, String keyword) {
        CashTransactionType selectedType = parseType(type);
        return cashTransactionRepository.search(
                selectedType,
                null,
                null,
                PAYMENT_CATEGORIES,
                null,
                null,
                keyword == null ? "" : keyword.trim()
        );
    }

    @Transactional(readOnly = true)
    public List<CashTransaction> searchTransactions(String type,
                                                    String category,
                                                    String paymentMethod,
                                                    LocalDate fromDate,
                                                    LocalDate toDate,
                                                    String keyword) {
        // Parse filter từ String trên form sang enum để repository query dễ hơn.
        CashTransactionType selectedType = parseType(type);
        CashTransactionCategory selectedCategory = parseCategory(category);
        PaymentMethod selectedPaymentMethod = parsePaymentMethod(paymentMethod);
        Instant fromDateTime = fromDate == null ? null : fromDate.atStartOfDay(APP_ZONE).toInstant();
        Instant toDateTime = toDate == null ? null : toDate.plusDays(1).atStartOfDay(APP_ZONE).toInstant();

        return cashTransactionRepository.search(
                selectedType,
                selectedCategory,
                selectedPaymentMethod,
                PAYMENT_CATEGORIES,
                fromDateTime,
                toDateTime,
                keyword == null ? "" : keyword.trim()
        );
    }

    @Transactional
    public CashTransaction createManualTransaction(CashTransactionCreateRequest request, User createdBy) {
        validateManualTransactionRequest(request);

        CashTransactionCategory category = request.getType() == CashTransactionType.INCOME
                ? CashTransactionCategory.MANUAL_INCOME
                : CashTransactionCategory.MANUAL_EXPENSE;

        return createManualTransaction(
                request.getType(),
                category,
                request.getAmount(),
                request.getPaymentMethod(),
                request.getDescription().trim(),
                createdBy
        );
    }

    @Transactional
    public CashTransaction createManualTransaction(CashTransactionType type,
                                                   CashTransactionCategory category,
                                                   BigDecimal amount,
                                                   String description,
                                                   User createdBy) {
        return createManualTransaction(type, category, amount, null, description, createdBy);
    }

    @Transactional
    public CashTransaction createManualTransaction(CashTransactionType type,
                                                   CashTransactionCategory category,
                                                   BigDecimal amount,
                                                   PaymentMethod paymentMethod,
                                                   String description,
                                                   User createdBy) {
        validateAmount(amount);

        // Tạo phiếu thủ công: phiếu thu lưu số dương, phiếu chi lưu số âm.
        CashTransaction transaction = CashTransaction.builder()
                .documentCode(generateDocumentCode(type))
                .type(type)
                .category(category)
                .amount(normalizeMoneyByType(amount, type))
                .paymentMethod(paymentMethod)
                .description(description)
                .createdBy(createdBy)
                .status(CashTransactionStatus.COMPLETED)
                .build();

        return cashTransactionRepository.save(transaction);
    }

    @Transactional
    public void cancelManualVoucher(Long transactionId, User manager, String reason) {
        CashTransaction originalTransaction = getTransaction(transactionId);
        validateCancellationReason(reason);

        if (!canCancelManualVoucher(originalTransaction)) {
            throw new IllegalArgumentException("Phiếu này không đủ điều kiện để hủy.");
        }

        originalTransaction.setStatus(CashTransactionStatus.CANCELLED);
        originalTransaction.setCancelledBy(manager);
        originalTransaction.setCancelledAt(Instant.now());
        originalTransaction.setCancellationReason(reason.trim());

        // Tạo giao dịch đảo chiều để tổng tác động tài chính của hai dòng bằng 0.
        BigDecimal reversalAmount = originalTransaction.getAmount().negate();
        CashTransactionType reversalType = reversalAmount.compareTo(BigDecimal.ZERO) >= 0
                ? CashTransactionType.INCOME
                : CashTransactionType.EXPENSE;

        CashTransaction reversalTransaction = CashTransaction.builder()
                .documentCode(generateDocumentCode(reversalType))
                .type(reversalType)
                .category(CashTransactionCategory.REVERSAL)
                .amount(reversalAmount)
                .paymentMethod(originalTransaction.getPaymentMethod())
                .description("Đảo chiều hủy chứng từ " + originalTransaction.getDocumentCode())
                .sourceId(originalTransaction.getId())
                .originalTransaction(originalTransaction)
                .createdBy(manager)
                .status(CashTransactionStatus.COMPLETED)
                .build();

        CashTransaction savedReversalTransaction = cashTransactionRepository.save(reversalTransaction);
        originalTransaction.setReversalTransaction(savedReversalTransaction);
        cashTransactionRepository.save(originalTransaction);
    }

    @Transactional(readOnly = true)
    public boolean canCancelManualVoucher(CashTransaction transaction) {
        if (transaction == null) {
            return false;
        }

        // Chỉ cho hủy phiếu thu/chi thủ công đã hoàn tất và chưa từng bị đảo chiều.
        CashTransactionStatus status = resolveStatus(transaction);
        boolean isManualVoucher = transaction.getCategory() == CashTransactionCategory.MANUAL_INCOME
                || transaction.getCategory() == CashTransactionCategory.MANUAL_EXPENSE;

        return isManualVoucher
                && status == CashTransactionStatus.COMPLETED
                && transaction.getOriginalTransaction() == null
                && transaction.getReversalTransaction() == null;
    }

    @Transactional
    public CashTransaction createCapitalInjection(BigDecimal amount,
                                                  String description,
                                                  User createdBy) {
        return createManualTransaction(
                CashTransactionType.INCOME,
                CashTransactionCategory.CAPITAL_INJECTION,
                amount,
                description == null || description.isBlank() ? "Rot von khach san" : description,
                createdBy
        );
    }

    @Transactional
    public CashTransaction createInventoryPurchase(BigDecimal amount,
                                                   String description,
                                                   Long receiptId,
                                                   User createdBy) {
        validateAmount(amount);
        if (receiptId != null && cashTransactionRepository.existsByCategoryAndSourceId(
                CashTransactionCategory.INVENTORY_PURCHASE, receiptId)) {
            return cashTransactionRepository.findByCategoryAndSourceId(
                    CashTransactionCategory.INVENTORY_PURCHASE, receiptId).orElseThrow();
        }

        CashTransaction transaction = CashTransaction.builder()
                .documentCode(generateDocumentCode(CashTransactionType.EXPENSE))
                .type(CashTransactionType.EXPENSE)
                .category(CashTransactionCategory.INVENTORY_PURCHASE)
                .amount(normalizeMoneyByType(amount, CashTransactionType.EXPENSE))
                .description(description)
                .sourceId(receiptId)
                .createdBy(createdBy)
                .status(CashTransactionStatus.COMPLETED)
                .build();

        return cashTransactionRepository.save(transaction);
    }

    @Transactional
    public CashTransaction createFromPayment(Payment payment) {
        if (payment == null || payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalArgumentException("Payment must be successful to create cash transaction.");
        }

        if (payment.getId() != null && cashTransactionRepository.existsBySourceIdAndCategoryIn(
                payment.getId(), PAYMENT_CATEGORIES)) {
            return cashTransactionRepository.findFirstBySourceIdAndCategoryIn(
                    payment.getId(), PAYMENT_CATEGORIES).orElseThrow();
        }

        CashTransactionType type = payment.getPaymentType() == PaymentType.REFUND
                ? CashTransactionType.EXPENSE
                : CashTransactionType.INCOME;

        CashTransactionCategory category = switch (payment.getPaymentType()) {
            case DEPOSIT -> CashTransactionCategory.DEPOSIT;
            case REFUND -> CashTransactionCategory.REFUND;
            case BALANCE, FULL -> CashTransactionCategory.BOOKING_PAYMENT;
        };

        CashTransaction transaction = CashTransaction.builder()
                .documentCode(generateDocumentCode(type))
                .type(type)
                .category(category)
                .amount(normalizeMoneyByType(payment.getAmount(), type))
                .paymentMethod(payment.getMethod())
                .description(buildPaymentDescription(payment))
                .sourceId(payment.getId())
                .createdBy(payment.getProcessedBy())
                .createdAt(payment.getPaidAt())
                .status(CashTransactionStatus.COMPLETED)
                .build();

        return cashTransactionRepository.save(transaction);
    }
    private String buildPaymentDescription(Payment payment) {
        String bookingCode = "";

        if (payment.getBooking() != null && payment.getBooking().getBookingReference() != null) {
            bookingCode = payment.getBooking().getBookingReference();
        }

        return switch (payment.getPaymentType()) {
            case DEPOSIT -> "Thu tiền đặt cọc cho booking " + bookingCode;
            case BALANCE -> "Thu phần tiền còn lại cho booking " + bookingCode;
            case FULL -> "Thu toàn bộ tiền booking " + bookingCode;
            case REFUND -> "Hoàn tiền cho booking " + bookingCode;
        };
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalIncome() {
        return cashTransactionRepository.sumByType(CashTransactionType.INCOME);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalExpense() {
        return cashTransactionRepository.sumByType(CashTransactionType.EXPENSE).abs();
    }

    @Transactional(readOnly = true)
    public BigDecimal getIncomeForDay(LocalDate date) {
        return cashTransactionRepository.sumByTypeBetween(
                CashTransactionType.INCOME,
                date.atStartOfDay(APP_ZONE).toInstant(),
                date.plusDays(1).atStartOfDay(APP_ZONE).toInstant()
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal getIncomeForMonth(LocalDate date) {
        LocalDate start = date.withDayOfMonth(1);
        return cashTransactionRepository.sumByTypeBetween(
                CashTransactionType.INCOME,
                start.atStartOfDay(APP_ZONE).toInstant(),
                start.plusMonths(1).atStartOfDay(APP_ZONE).toInstant()
        );
    }

    private CashTransactionType parseType(String type) {
        if (type == null || type.isBlank() || "ALL".equalsIgnoreCase(type)) {
            return null;
        }
        return CashTransactionType.valueOf(type.trim().toUpperCase());
    }

    private CashTransactionCategory parseCategory(String category) {
        if (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category)) {
            return null;
        }
        return CashTransactionCategory.valueOf(category.trim().toUpperCase());
    }

    private PaymentMethod parsePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank() || "ALL".equalsIgnoreCase(paymentMethod)) {
            return null;
        }
        return PaymentMethod.valueOf(paymentMethod.trim().toUpperCase());
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0.");
        }
    }

    private void validateManualTransactionRequest(CashTransactionCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Vui lòng nhập thông tin phiếu.");
        }
        if (request.getType() == null) {
            throw new IllegalArgumentException("Vui lòng chọn loại phiếu.");
        }
        if (request.getPaymentMethod() == null) {
            throw new IllegalArgumentException("Vui lòng chọn phương thức thanh toán.");
        }
        validateAmount(request.getAmount());
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập nội dung phiếu.");
        }
    }

    private BigDecimal normalizeMoney(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeMoneyByType(BigDecimal amount, CashTransactionType type) {
        // Người dùng nhập số dương, hệ thống tự đổi phiếu chi thành số âm.
        BigDecimal normalizedAmount = normalizeMoney(amount).abs();
        if (type == CashTransactionType.EXPENSE) {
            return normalizedAmount.negate();
        }
        return normalizedAmount;
    }

    private void validateCancellationReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do hủy phiếu.");
        }
    }

    private CashTransactionStatus resolveStatus(CashTransaction transaction) {
        return transaction.getStatus() == null ? CashTransactionStatus.COMPLETED : transaction.getStatus();
    }

    private List<CashTransactionResponse> getPagedTransactions(List<CashTransactionResponse> transactions,
                                                               int page,
                                                               int size) {
        if (transactions.isEmpty()) {
            return List.of();
        }

        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, transactions.size());

        return transactions.subList(startIndex, endIndex);
    }

    private Map<Long, String> getPaymentMethodMap(List<CashTransaction> transactions) {
        List<Long> paymentIds = transactions.stream()
                .filter(transaction -> PAYMENT_CATEGORIES.contains(transaction.getCategory()))
                .map(CashTransaction::getSourceId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (paymentIds.isEmpty()) {
            return Map.of();
        }

        // Lay phuong thuc thanh toan tu bang payments cho cac dong tien sinh tu payment.
        return paymentRepository.findAllById(paymentIds)
                .stream()
                .collect(Collectors.toMap(
                        Payment::getId,
                        payment -> payment.getMethod().getLabel()
                ));
    }

    private CashTransactionResponse toResponse(CashTransaction transaction, Map<Long, String> paymentMethodMap) {
        String paymentMethodDisplayName = "Chưa có";
        if (transaction.getPaymentMethod() != null) {
            paymentMethodDisplayName = transaction.getPaymentMethod().getLabel();
        } else if (PAYMENT_CATEGORIES.contains(transaction.getCategory()) && transaction.getSourceId() != null) {
            paymentMethodDisplayName = paymentMethodMap.getOrDefault(transaction.getSourceId(), "Chưa có");
        }

        return CashTransactionResponse.builder()
                .id(transaction.getId())
                .documentCode(transaction.getDocumentCode())
                .createdAt(transaction.getCreatedAt())
                .type(transaction.getType().name())
                .typeDisplayName(transaction.getType().getDisplayName())
                .category(transaction.getCategory().name())
                .categoryDisplayName(transaction.getCategory().getDisplayName())
                .amount(transaction.getAmount())
                .paymentMethodDisplayName(paymentMethodDisplayName)
                .statusDisplayName(resolveStatus(transaction).getDisplayName())
                .build();
    }

    private String generateDocumentCode(CashTransactionType type) {
        String prefix = type == CashTransactionType.INCOME ? "PT" : "PC";

        // Mã chứng từ chạy chung một dãy số cho cả phiếu thu và phiếu chi.
        int nextNumber = cashTransactionRepository.findSimpleDocumentCodes()
                .stream()
                .filter(this::isSimpleDocumentCode)
                .mapToInt(this::getDocumentNumber)
                .max()
                .orElse(0) + 1;

        return prefix + "-" + String.format("%04d", nextNumber);
    }

    private boolean isSimpleDocumentCode(String documentCode) {
        if (documentCode == null
                || (!documentCode.startsWith("PT-") && !documentCode.startsWith("PC-"))) {
            return false;
        }

        String numberPart = documentCode.substring(3);
        return numberPart.length() == 4 && numberPart.chars().allMatch(Character::isDigit);
    }

    private int getDocumentNumber(String documentCode) {
        return Integer.parseInt(documentCode.substring(3));
    }
}
