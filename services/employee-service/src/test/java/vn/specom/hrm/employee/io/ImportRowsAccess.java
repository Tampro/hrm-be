package vn.specom.hrm.employee.io;

import org.apache.poi.ss.usermodel.Row;
import vn.specom.hrm.employee.employee.EmployeeDtos.EmployeeCreateForm;
import vn.specom.hrm.employee.org.OrgService;

/** Test access to the package-private row parser. */
public final class ImportRowsAccess {
    private ImportRowsAccess() {
    }

    public static EmployeeCreateForm toForm(Row row, OrgService.Tree tree, int excelRow) {
        return EmployeeExcelService.toForm(row, tree, excelRow);
    }
}
