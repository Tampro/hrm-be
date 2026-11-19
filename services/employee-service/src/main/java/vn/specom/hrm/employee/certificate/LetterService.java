package vn.specom.hrm.employee.certificate;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.specom.hrm.employee.client.IamClient;
import vn.specom.hrm.employee.common.EEmployeeError;
import vn.specom.hrm.employee.employee.Employee;
import vn.specom.hrm.employee.employee.EmployeeService;
import vn.specom.hrm.employee.org.OrgService;
import vn.specom.hrm.exception.ApiException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Joining letter, experience certificate and NOC of docs/analysis/06 §6.1 (Certifications tab), as PDF.
 * TODO(assumption): the wording is a neutral draft in vi/en until HR provides the company's templates; making
 * them editable (like email templates) is a later task.
 */
@Service
@RequiredArgsConstructor
public class LetterService {
    public static final String FONT = "/liberation/LiberationSans-Regular.ttf";
    public static final String FONT_BOLD = "/liberation/LiberationSans-Bold.ttf";
    private static final Set<String> LANGS = Set.of("vi", "en");

    public record Letter(String fileName, byte[] pdf) {
    }

    private final EmployeeService employees;
    private final OrgService orgs;
    private final IamClient iam;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Letter render(Long employeeId, String typeCode, String lang) {
        ELetterType type;
        try {
            type = ELetterType.valueOf(typeCode.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(EEmployeeError.EMP_025, typeCode);
        }
        // ko → en: the bundled font has no Hangul (see LetterTexts).
        String l = lang == null ? "vi" : "ko".equals(lang) ? "en" : LANGS.contains(lang) ? lang : "vi";
        Employee e = employees.require(employeeId);
        String company = iam.company(e.getCompanyId()).name();
        var names = orgs.names(e.getCompanyId(), ids(e.getBranchId()), ids(e.getDepartmentId()), ids(e.getDesignationId()));
        Map<String, String> v = Map.of(
                "company", company,
                "name", e.getFullName(),
                "no", e.getEmployeeNo(),
                "designation", names.designations().getOrDefault(e.getDesignationId(), "-"),
                "department", names.departments().getOrDefault(e.getDepartmentId(), "-"),
                "joined", date(e.getDateOfJoining(), l),
                "today", date(LocalDate.now(clock), l));
        String[] text = LetterTexts.of(type, l);
        return new Letter(type.name().toLowerCase(Locale.ROOT) + "-" + e.getEmployeeNo() + ".pdf", pdf(text, v));
    }

    static byte[] pdf(String[] text, Map<String, String> v) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 60, 60, 70, 70);
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font regular = new Font(font(FONT), 12);
            Font bold = new Font(font(FONT_BOLD), 16);
            Paragraph company = new Paragraph(v.get("company"), new Font(font(FONT_BOLD), 13));
            doc.add(company);
            Paragraph date = new Paragraph(fill(text[1], v), regular);
            date.setAlignment(Element.ALIGN_RIGHT);
            date.setSpacingAfter(24);
            doc.add(date);
            Paragraph title = new Paragraph(fill(text[0], v), bold);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(24);
            doc.add(title);
            for (int i = 2; i < text.length; i++) {
                Paragraph p = new Paragraph(fill(text[i], v), regular);
                p.setSpacingAfter(10);
                p.setAlignment(i == text.length - 1 ? Element.ALIGN_RIGHT : Element.ALIGN_JUSTIFIED);
                doc.add(p);
            }
            doc.close();
            return out.toByteArray();
        } catch (IOException | DocumentException e) {
            throw new IllegalStateException("PDF rendering failed", e);
        }
    }

    /** Embedded TTF: the image has no system fonts and the built-in PDF fonts have no Vietnamese glyphs. */
    public static BaseFont font(String path) throws IOException {
        try (InputStream in = LetterService.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("font missing: " + path);
            }
            return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, in.readAllBytes(), null);
        }
    }

    static String fill(String s, Map<String, String> v) {
        String out = s;
        for (var en : v.entrySet()) {
            out = out.replace("{" + en.getKey() + "}", en.getValue());
        }
        return out;
    }

    private static String date(LocalDate d, String lang) {
        if (d == null) {
            return "-";
        }
        return switch (lang) {
            case "en" -> d.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH));
            default -> d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        };
    }

    private static List<Long> ids(Long id) {
        return id == null ? List.of() : List.of(id);
    }
}
