package eu.comsode.unifiedviews.plugins.extractor.skmartinbudget;

import eu.unifiedviews.dpu.config.DPUConfigException;
import eu.unifiedviews.helpers.dpu.vaadin.dialog.AbstractDialog;

/**
 * Vaadin configuration dialog .
 */
public class SkMartinBudgetVaadinDialog extends AbstractDialog<SkMartinBudgetConfig_V1> {

    public SkMartinBudgetVaadinDialog() {
        super(SkMartinBudget.class);
    }

    @Override
    public void setConfiguration(SkMartinBudgetConfig_V1 c) throws DPUConfigException {

    }

    @Override
    public SkMartinBudgetConfig_V1 getConfiguration() throws DPUConfigException {
        final SkMartinBudgetConfig_V1 c = new SkMartinBudgetConfig_V1();

        return c;
    }

    @Override
    public void buildDialogLayout() {
    }

}
