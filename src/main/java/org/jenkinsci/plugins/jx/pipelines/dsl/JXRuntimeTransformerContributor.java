package org.jenkinsci.plugins.jx.pipelines.dsl;

import hudson.Extension;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.jenkinsci.plugins.jx.pipelines.arguments.StepContainer;
import org.jenkinsci.plugins.jx.pipelines.helpers.ConfigHelper;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTStep;
import org.jenkinsci.plugins.pipeline.modeldefinition.parser.StepRuntimeTransformerContributor;

import javax.annotation.Nonnull;

import static org.codehaus.groovy.ast.tools.GeneralUtils.args;
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.classX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.constX;

@Extension
public class JXRuntimeTransformerContributor extends StepRuntimeTransformerContributor {
    @Override
    @Nonnull
    public MethodCallExpression transformStep(@Nonnull ModelASTStep step, @Nonnull MethodCallExpression methodCall) {
        PipelineDSLGlobal global = PipelineDSLGlobal.getGlobalForName(step.getName());
        if (global != null && global.getArgumentsType() != null) {
            TupleExpression args = (TupleExpression)methodCall.getArguments();
            MapExpression m = new MapExpression();

            for (Expression e : args.getExpressions()) {
                if (e instanceof ClosureExpression) {
                    MapExpression closureMap = closureToMapExpression((ClosureExpression)e);
                    for (MapEntryExpression mee : closureMap.getMapEntryExpressions()) {
                        m.addMapEntryExpression(mee);
                    }
                } else if (e instanceof MapExpression) {
                    for (MapEntryExpression mee : ((MapExpression)e).getMapEntryExpressions()) {
                        m.addMapEntryExpression(mee);
                    }
                }
            }

            methodCall.setArguments(callX(ClassHelper.make(ConfigHelper.class),
                    "populateBeanFromConfiguration",
                    args(classX(ClassHelper.make(global.getArgumentsType().clazz)), m)));
        }

        return methodCall;
    }

    @Nonnull
    private MapExpression closureToMapExpression(@Nonnull ClosureExpression closureExpression) {
        MapExpression m = new MapExpression();

        if (closureExpression.getCode() instanceof BlockStatement) {
            BlockStatement block = (BlockStatement) closureExpression.getCode();
            for (Statement s : block.getStatements()) {
                if (s instanceof ExpressionStatement) {
                    ExpressionStatement es = (ExpressionStatement) s;
                    if (es.getExpression() instanceof MethodCallExpression) {
                        MethodCallExpression mce = (MethodCallExpression) es.getExpression();
                        String methName = mce.getMethodAsString();

                        TupleExpression origArgs = (TupleExpression)mce.getArguments();

                        if (origArgs.getExpressions().size() == 1) {
                            if (origArgs.getExpression(0) instanceof ClosureExpression) {
                                if (StepContainer.closureNames.contains(methName)) {
                                    m.addMapEntryExpression(constX(methName), origArgs.getExpression(0));
                                } else {
                                    m.addMapEntryExpression(constX(methName),
                                            closureToMapExpression((ClosureExpression) origArgs.getExpression(0)));
                                }
                            } else {
                                m.addMapEntryExpression(constX(methName), origArgs.getExpression(0));
                            }
                        } else {
                            // TODO: Not sure if this ever will get encountered, but if it does, that's probably a problem.
                            throw new IllegalArgumentException("Can't add an argument list of " + mce.getArguments() + " for method " + mce.getMethodAsString());
                        }
                    }
                }
            }
        }

        return m;
    }
}
