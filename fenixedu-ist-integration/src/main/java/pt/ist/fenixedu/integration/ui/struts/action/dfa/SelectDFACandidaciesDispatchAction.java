/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Integration.
 *
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 *
 */
package pt.ist.fenixedu.integration.ui.struts.action.dfa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.candidacy.CandidacySituationType;
import org.fenixedu.academic.domain.candidacy.DFACandidacy;
import org.fenixedu.academic.dto.administrativeOffice.candidacy.DFACandidacyBean;
import org.fenixedu.academic.dto.administrativeOffice.candidacy.SelectDFACandidacyBean;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.bennu.portal.servlet.PortalLayoutInjector;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;

import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.ist.fenixedu.integration.service.services.dfa.SelectCandidacies;

/**
 * @author - Shezad Anavarali (shezad@ist.utl.pt)
 *
 */

@StrutsFunctionality(app = DFAApplication.class, path = "select-candidacies",
        titleKey = "link.masterDegree.administrativeOffice.dfaCandidacy.selectCandidacies")
@Mapping(path = "/selectDFACandidacies", module = "masterDegreeAdministrativeOffice",
        input = "/candidacy/listCandidaciesForSelection.jsp")
@Forwards({
        @Forward(name = "listCandidacies", path = "/masterDegreeAdministrativeOffice/candidacy/listCandidaciesForSelection.jsp"),
        @Forward(name = "confirmCandidaciesForSelection",
                path = "/masterDegreeAdministrativeOffice/candidacy/confirmCandidaciesForSelection.jsp"),
        @Forward(name = "showCandidaciesForSelectionSuccess",
                path = "/masterDegreeAdministrativeOffice/candidacy/showCandidaciesForSelectionSuccess.jsp"),
        @Forward(name = "printAcceptanceDispatch",
                path = "/masterDegreeAdministrativeOffice/candidacy/acceptanceDispatchTemplate.jsp") })
public class SelectDFACandidaciesDispatchAction extends FenixDispatchAction {

    @EntryPoint
    public ActionForward prepareListCandidacies(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        DFACandidacyBean candidacyBean = new DFACandidacyBean();
        request.setAttribute("candidacyBean", candidacyBean);
        return mapping.findForward("listCandidacies");
    }

    public ActionForward chooseDegreePostBack(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {

        DFACandidacyBean candidacyBean = (DFACandidacyBean) RenderUtils.getViewState().getMetaObject().getObject();
        candidacyBean.setDegreeCurricularPlan(null);
        candidacyBean.setExecutionYear(null);
        RenderUtils.invalidateViewState();
        request.setAttribute("candidacyBean", candidacyBean);

        return mapping.getInputForward();
    }

    public ActionForward chooseDegreeCurricularPlanPostBack(ActionMapping mapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response) {

        DFACandidacyBean candidacyBean = (DFACandidacyBean) RenderUtils.getViewState().getMetaObject().getObject();
        RenderUtils.invalidateViewState();
        request.setAttribute("candidacyBean", candidacyBean);

        return mapping.getInputForward();
    }

    public ActionForward chooseExecutionDegreeInvalid(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        request.setAttribute("candidacyBean", RenderUtils.getViewState().getMetaObject().getObject());
        return mapping.getInputForward();
    }

    public ActionForward listCandidacies(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        DFACandidacyBean dfaCandidacyBean = (DFACandidacyBean) RenderUtils.getViewState().getMetaObject().getObject();

        List<SelectDFACandidacyBean> candidacies = new ArrayList<SelectDFACandidacyBean>();
        for (DFACandidacy candidacy : dfaCandidacyBean.getExecutionDegree().getDfaCandidacies()) {
            CandidacySituationType candidacySituationType = candidacy.getActiveCandidacySituation().getCandidacySituationType();
            if (candidacySituationType.equals(CandidacySituationType.STAND_BY_CONFIRMED_DATA)
                    || candidacySituationType.equals(CandidacySituationType.SUBSTITUTE)
                    || candidacySituationType.equals(CandidacySituationType.ADMITTED)
                    || candidacySituationType.equals(CandidacySituationType.NOT_ADMITTED)) {

                candidacies.add(new SelectDFACandidacyBean(candidacy));
            }
        }

        request.setAttribute("candidacies", candidacies);
        request.setAttribute("candidacyBean", dfaCandidacyBean);
        return mapping.findForward("listCandidacies");
    }

    public ActionForward selectCandidacies(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        List<SelectDFACandidacyBean> candidaciesListForSelection =
                (List<SelectDFACandidacyBean>) RenderUtils.getViewState().getMetaObject().getObject();

        List<SelectDFACandidacyBean> admittedCandidacies = new ArrayList<SelectDFACandidacyBean>();
        List<SelectDFACandidacyBean> notAdmittedCandidacies = new ArrayList<SelectDFACandidacyBean>();
        List<SelectDFACandidacyBean> substituteCandidacies = new ArrayList<SelectDFACandidacyBean>();

        for (SelectDFACandidacyBean candidacyBean : candidaciesListForSelection) {
            if (candidacyBean.getSelectionSituation() != null) {
                switch (candidacyBean.getSelectionSituation()) {
                case ADMITTED:
                    admittedCandidacies.add(candidacyBean);
                    break;
                case SUBSTITUTE:
                    substituteCandidacies.add(candidacyBean);
                    break;
                case NOT_ADMITTED:
                    notAdmittedCandidacies.add(candidacyBean);
                    break;
                default:
                    break;
                }
            }
        }

        if (admittedCandidacies.isEmpty() && substituteCandidacies.isEmpty() && notAdmittedCandidacies.isEmpty()) {
            return setError(request, mapping, "no.candidacy.situations.selected", "confirmCandidaciesForSelection", null);
        }

        Collections.sort(substituteCandidacies,
                Comparator.comparing(SelectDFACandidacyBean::getCandidacy, Comparator.comparing(DFACandidacy::getNumber)));

        request.setAttribute("admittedCandidacies", admittedCandidacies);
        request.setAttribute("substituteCandidacies", substituteCandidacies);
        request.setAttribute("notAdmittedCandidacies", notAdmittedCandidacies);

        return mapping.findForward("confirmCandidaciesForSelection");
    }

    public ActionForward confirmCandidacies(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {

        List<SelectDFACandidacyBean> admittedCandidacies = retrieveCandidaciesBeans("admittedCandidacies");
        List<SelectDFACandidacyBean> substituteCandidacies = retrieveCandidaciesBeans("substituteCandidacies");
        List<SelectDFACandidacyBean> notAdmittedCandidacies = retrieveCandidaciesBeans("notAdmittedCandidacies");

        if (substituteCandidacies != null) {
            String substituteCandidaciesOrderTree = request.getParameter("substituteCandidaciesOrder");
            String[] substitutesOrder = substituteCandidaciesOrderTree.replaceAll("-0", "").split(",");

            for (int i = 0; i < substitutesOrder.length; i++) {
                substituteCandidacies.get(i).setOrder(Integer.valueOf(substitutesOrder[i]));
            }
        }

        SelectCandidacies.run(admittedCandidacies, substituteCandidacies, notAdmittedCandidacies);

        request.setAttribute("admittedCandidacies", admittedCandidacies);
        request.setAttribute("substituteCandidacies", substituteCandidacies);
        request.setAttribute("notAdmittedCandidacies", notAdmittedCandidacies);

        return mapping.findForward("showCandidaciesForSelectionSuccess");
    }

    public ActionForward printAcceptanceDispatch(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {

        List<SelectDFACandidacyBean> admittedCandidacies = retrieveCandidaciesBeans("admittedCandidacies");
        List<SelectDFACandidacyBean> substituteCandidacies = retrieveCandidaciesBeans("substituteCandidacies");
        List<SelectDFACandidacyBean> notAdmittedCandidacies = retrieveCandidaciesBeans("notAdmittedCandidacies");

        if (substituteCandidacies != null) {
            Collections.sort(substituteCandidacies, Comparator.comparing(SelectDFACandidacyBean::getOrder));
        }

        ExecutionDegree executionDegree =
                retrieveExecutionDegree(admittedCandidacies, substituteCandidacies, notAdmittedCandidacies);

        ExecutionYear executionYear = executionDegree.getExecutionYear();
        request.setAttribute("degreeName", executionDegree.getDegreeCurricularPlan().getDegree().getNameFor(executionYear));
        request.setAttribute("currentExecutionYear", executionYear.getYear());
        request.setAttribute("admittedCandidacies", admittedCandidacies);
        request.setAttribute("substituteCandidacies", substituteCandidacies);
        request.setAttribute("notAdmittedCandidacies", notAdmittedCandidacies);

        PortalLayoutInjector.skipLayoutOn(request);
        return mapping.findForward("printAcceptanceDispatch");
    }

    private List<SelectDFACandidacyBean> retrieveCandidaciesBeans(String candidaciesSituation) {
        return (RenderUtils.getViewState(candidaciesSituation) != null) ? (List<SelectDFACandidacyBean>) RenderUtils
                .getViewState(candidaciesSituation).getMetaObject().getObject() : null;
    }

    private ExecutionDegree retrieveExecutionDegree(List<SelectDFACandidacyBean> admittedCandidacies,
            List<SelectDFACandidacyBean> substituteCandidacies, List<SelectDFACandidacyBean> notAdmittedCandidacies) {
        ExecutionDegree executionDegree = null;
        if (admittedCandidacies != null && !admittedCandidacies.isEmpty()) {
            executionDegree = admittedCandidacies.iterator().next().getCandidacy().getExecutionDegree();
        } else if (substituteCandidacies != null && !substituteCandidacies.isEmpty()) {
            executionDegree = substituteCandidacies.iterator().next().getCandidacy().getExecutionDegree();
        } else {
            executionDegree = notAdmittedCandidacies.iterator().next().getCandidacy().getExecutionDegree();
        }
        return executionDegree;
    }

}