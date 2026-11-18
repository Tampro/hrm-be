package vn.specom.hrm.iam.saas;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The account companies transfer to. TODO(assumption): one account for all plans, shown with each order — confirm
 * with the PO which account and whether the transfer content must follow a bank's format.
 */
@ConfigurationProperties(prefix = "hrm.saas.bank")
public record BankTransferProperties(String bankName, String accountNumber, String accountName) {
}
