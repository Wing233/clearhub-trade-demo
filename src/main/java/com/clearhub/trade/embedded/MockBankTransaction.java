package com.clearhub.trade.embedded;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "mock_bank_transaction",
        uniqueConstraints = @UniqueConstraint(name = "uk_mock_bank_request", columnNames = "request_id"))
public class MockBankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "txn_no", nullable = false, length = 40)
    private String txnNo;

    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Column(name = "firm_id", nullable = false)
    private Long firmId;

    @Column(name = "account_no", nullable = false, length = 40)
    private String accountNo;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 20, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "ref_no", length = 64)
    private String refNo;

    @Column(length = 255)
    private String remark;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
