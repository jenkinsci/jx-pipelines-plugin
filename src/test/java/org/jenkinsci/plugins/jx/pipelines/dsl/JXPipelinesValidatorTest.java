package org.jenkinsci.plugins.jx.pipelines.dsl;

import org.jenkinsci.plugins.pipeline.modeldefinition.AbstractModelDefTest;
import org.jenkinsci.plugins.pipeline.modeldefinition.Messages;
import org.junit.Test;

public class JXPipelinesValidatorTest extends AbstractModelDefTest {

    @Test
    public void simpleValidationFailure() throws Exception {
        expectError("simpleValidationFailure")
                .logContains(Messages.ModelValidatorImpl_InvalidStepParameter("steveOrganisation", "cdOrganisation"))
                .go();
    }
}
