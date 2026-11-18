package vn.specom.hrm.employee.certificate;

import java.util.Map;

/** Test access to the package-private PDF renderer. */
public final class LetterAccess {
    private LetterAccess() {
    }

    public static byte[] pdf(String[] text, Map<String, String> v) {
        return LetterService.pdf(text, v);
    }
}
