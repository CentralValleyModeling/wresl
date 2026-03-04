package gov.ca.water.wresl.domain;

import java.util.List;

public class WRIMS_CaseData extends WRIMSComponent {
    public String caseCondition;
    public String caseExpression;

    public WRIMS_CaseData(String caseCondition, String caseExpression) {
        this.caseCondition = caseCondition;
        this.caseExpression = caseExpression;
    }
}
