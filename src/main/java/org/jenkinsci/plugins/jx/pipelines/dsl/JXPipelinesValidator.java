package org.jenkinsci.plugins.jx.pipelines.dsl;

import hudson.Extension;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTStep;
import org.jenkinsci.plugins.pipeline.modeldefinition.validator.DeclarativeValidatorContributor;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

@Extension
public class JXPipelinesValidator extends DeclarativeValidatorContributor {
    @CheckForNull
    public String validateElement(@Nonnull ModelASTStep step, @CheckForNull FlowExecution execution) {
        String stepName = step.getName();

        PipelineDSLGlobal global = PipelineDSLGlobal.getGlobalForName(stepName);

        // No matching global, so move on.
        if (global == null) {
            return null;
        }

        return global.validate(step);
    }

}
