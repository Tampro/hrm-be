package vn.specom.hrm.iam.constant;

/** SUSPENDED blocks login and every token of the company's users (checked on each request). */
public enum ECompanyStatus {
    ACTIVE, SUSPENDED
}
