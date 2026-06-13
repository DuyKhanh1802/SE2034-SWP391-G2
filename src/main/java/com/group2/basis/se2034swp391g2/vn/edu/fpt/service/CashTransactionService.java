package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.*;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.CashTransaction;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Payment;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.CashTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CashTransactionService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

    private final CashTransactionRepository cashTransactionRepository;

    @Transactional(readOnly = true)
    public List<CashTransaction> getRecentTransactions(int limit) {
        return cashTransactionRepository.findByOrderByCreatedAtDesc(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public CashTransaction getTransaction(Long id) {
        return cashTransactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dòng tiền."));
    }

    @Transactional(readOnly = true)
    public List<CashTransaction> searchTransactions(String type, String keyword) {
        CashTransactionType selectedType = parseType(type);
        return cashTransactionRepository.search(selectedType, keyword == null ? "" : keyword.trim());
    }

    @Transactional
    public CashTransaction createManualTransaction(CashTransactionType type,
                                                   CashTransactionCategory category,
                                                   BigDecimal amount,
                                                   String description,
                                                   User createdBy) {
        validateAmount(amount);

        CashTransaction transaction = CashTransaction.builder()
                .code(generateCode())
                .documentCode(generateDocumentCode(type))
                .type(type)
                .category(category)
                .amount(normalizeMoney(amount))
                .description(description)
                .sourceType(CashTransactionSourceType.MANUAL)
                .createdBy(createdBy)
                .build();

        return cashTransactionRepository.save(transaction);
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
        if (receiptId != null && cashTransactionRepository.existsBySourceTypeAndSourceId(
                CashTransactionSourceType.INVENTORY_RECEIPT, receiptId)) {
            return cashTransactionRepository.findBySourceTypeAndSourceId(
                    CashTransactionSourceType.INVENTORY_RECEIPT, receiptId).orElseThrow();
        }

        CashTransaction transaction = CashTransaction.builder()
                .code(generateCode())
                .documentCode(generateDocumentCode(CashTransactionType.EXPENSE))
                .type(CashTransactionType.EXPENSE)
                .category(CashTransactionCategory.INVENTORY_PURCHASE)
                .amount(normalizeMoney(amount))
                .description(description)
                .sourceType(CashTransactionSourceType.INVENTORY_RECEIPT)
                .sourceId(receiptId)
                .createdBy(createdBy)
                .build();

        return cashTransactionRepository.save(transaction);
    }

    @Transactional
    public CashTransaction createFromPayment(Payment payment) {
        if (payment == null || payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalArgumentException("Payment must be successful to create cash transaction.");
        }
        if (payment.getId() != null && cashTransactionRepository.existsBySourceTypeAndSourceId(
                CashTransactionSourceType.PAYMENT, payment.getId())) {
            return cashTransactionRepository.findBySourceTypeAndSourceId(
                    CashTransactionSourceType.PAYMENT, payment.getId()).orElseThrow();
        }

        CashTransactionType type = payment.getPaymentType() == PaymentType.REFUND
                ? CashTransactionType.EXPENSE
                : CashTransactionType.INCOME;

        CashTransactionCategory category = switch (payment.getPaymentType()) {
            case DEPOSIT -> CashTransactionCategory.DEPOSIT;
            case REFUND -> CashTransactionCategory.REFUND;
            case CHECKOUT -> CashTransactionCategory.BOOKING_PAYMENT;
        };

        CashTransaction transaction = CashTransaction.builder()
                .code(generateCode())
                .documentCode(generateDocumentCode(type))
                .type(type)
                .category(category)
                .amount(normalizeMoney(payment.getAmount()))
                .description("Payment " + payment.getPaymentType() + " - " + payment.getTransactionRef())
                .sourceType(CashTransactionSourceType.PAYMENT)
                .sourceId(payment.getId())
                .createdBy(payment.getProcessedBy())
                .createdAt(payment.getPaidAt())
                .build();

        return cashTransactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalIncome() {
        return cashTransactionRepository.sumByType(CashTransactionType.INCOME);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalExpense() {
        return cashTransactionRepository.sumByType(CashTransactionType.EXPENSE);
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

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0.");
        }
    }

    private BigDecimal normalizeMoney(BigDecimal amount) {
        return amount.setScale(0, java.math.RoundingMode.HALF_UP);
    }

    private String generateCode() {
        String datePart = LocalDate.now(APP_ZONE).format(CODE_DATE_FORMATTER);
        String code;

        do {
            int randomNumber = (int) (Math.random() * 9000) + 1000;
            code = "CT-" + datePart + "-" + randomNumber;
        } while (cashTransactionRepository.existsByCode(code));

        return code;
    }

    private String generateDocumentCode(CashTransactionType type) {
        String prefix = type == CashTransactionType.INCOME ? "PT" : "PC";
        String datePart = LocalDate.now(APP_ZONE).format(CODE_DATE_FORMATTER);
        String documentCode;

        do {
            int randomNumber = (int) (Math.random() * 9000) + 1000;
            documentCode = prefix + "-" + datePart + "-" + randomNumber;
        } while (cashTransactionRepository.existsByDocumentCode(documentCode));

        return documentCode;
    }
}
