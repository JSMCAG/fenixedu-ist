/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST QUC.
 *
 * FenixEdu IST QUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST QUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST QUC.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.quc.domain;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.student.Registration;

public abstract class StudentCycleInquiryTemplate extends StudentCycleInquiryTemplate_Base {

    public StudentCycleInquiryTemplate() {
        super();
    }

    public static StudentCycleInquiryTemplate getStudentCycleInquiryTemplate(Registration registration) {
        CycleType cycleType = registration.getCycleType(ExecutionYear.readCurrentExecutionYear());
        if (cycleType == null) {
            cycleType = registration.getDegree().getDegreeType().getLastOrderedCycleType();
        }
        switch (cycleType) {
        case FIRST_CYCLE:
            return Student1rstCycleInquiryTemplate.getCurrentTemplate();
        case SECOND_CYCLE:
            return Student2ndCycleInquiryTemplate.getCurrentTemplate();
        default:
            return StudentOtherCycleInquiryTemplate.getCurrentTemplate();
        }
    }

    public static StudentCycleInquiryTemplate getStudentCycleInquiryTemplate(PhdIndividualProgramProcess phdProcess) {
        return StudentOtherCycleInquiryTemplate.getCurrentTemplate();
    }
}
