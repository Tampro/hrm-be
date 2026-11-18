package vn.specom.hrm.employee.certificate;

/**
 * Wording of the letters: [0] title, [1] place/date line, [2..n-1] body, [n] signature line.
 * TODO(assumption): draft texts, to be replaced by the company's own templates (confirm with HR/BA).
 * No Korean yet: the bundled font (Liberation) has no Hangul glyphs; ko falls back to en until a CJK font is added.
 */
final class LetterTexts {
    private LetterTexts() {
    }

    static String[] of(ELetterType type, String lang) {
        return switch (lang) {
            case "en" -> en(type);
            default -> vi(type);
        };
    }

    private static String[] vi(ELetterType type) {
        return switch (type) {
            case JOINING_LETTER -> new String[]{"THƯ MỜI NHẬN VIỆC", "Ngày {today}",
                    "Kính gửi Ông/Bà {name},",
                    "{company} trân trọng xác nhận Ông/Bà được tiếp nhận vào làm việc ở vị trí {designation}, "
                            + "bộ phận {department}, kể từ ngày {joined}.",
                    "Mã nhân viên của Ông/Bà là {no}. Các điều khoản chi tiết được quy định trong hợp đồng lao động.",
                    "Chúc Ông/Bà thành công trong công việc mới.",
                    "Đại diện {company}"};
            case EXPERIENCE_CERTIFICATE -> new String[]{"GIẤY XÁC NHẬN KINH NGHIỆM LÀM VIỆC", "Ngày {today}",
                    "{company} xác nhận Ông/Bà {name} (mã nhân viên {no}) đã làm việc tại công ty ở vị trí "
                            + "{designation}, bộ phận {department}, từ ngày {joined} đến ngày {today}.",
                    "Giấy xác nhận này được cấp theo đề nghị của người lao động.",
                    "Đại diện {company}"};
            case NOC -> new String[]{"GIẤY XÁC NHẬN KHÔNG PHẢN ĐỐI", "Ngày {today}",
                    "{company} xác nhận Ông/Bà {name} (mã nhân viên {no}), vị trí {designation}, bộ phận "
                            + "{department}, làm việc tại công ty từ ngày {joined}.",
                    "Công ty không phản đối việc Ông/Bà sử dụng giấy này cho mục đích hợp pháp của mình.",
                    "Đại diện {company}"};
        };
    }

    private static String[] en(ELetterType type) {
        return switch (type) {
            case JOINING_LETTER -> new String[]{"JOINING LETTER", "{today}",
                    "Dear {name},",
                    "{company} is pleased to confirm your appointment as {designation} in the {department} "
                            + "department, effective {joined}.",
                    "Your employee ID is {no}. The detailed terms are set out in your employment contract.",
                    "We wish you every success in your new role.",
                    "On behalf of {company}"};
            case EXPERIENCE_CERTIFICATE -> new String[]{"EXPERIENCE CERTIFICATE", "{today}",
                    "This is to certify that {name} (employee ID {no}) has worked with {company} as {designation} "
                            + "in the {department} department from {joined} to {today}.",
                    "This certificate is issued at the request of the employee.",
                    "On behalf of {company}"};
            case NOC -> new String[]{"NO OBJECTION CERTIFICATE", "{today}",
                    "This is to certify that {name} (employee ID {no}), {designation} in the {department} "
                            + "department, has been employed by {company} since {joined}.",
                    "The company has no objection to this certificate being used for any lawful purpose.",
                    "On behalf of {company}"};
        };
    }
}
