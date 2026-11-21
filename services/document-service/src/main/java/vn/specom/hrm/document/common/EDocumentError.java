package vn.specom.hrm.document.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.specom.hrm.exception.ErrorCode;

/** Never renumber or reuse a code: the FE keys on it. */
@Getter
@RequiredArgsConstructor
public enum EDocumentError implements ErrorCode {
    DOC_001("Choose the company to act on."),
    DOC_002("The document category does not exist."),
    DOC_003("This name already exists."),
    DOC_004("It is still in use and cannot be deleted."),
    DOC_005("The contract type does not exist."),
    DOC_006("The employee service is unavailable. Please try again.");

    private final String message;
}
