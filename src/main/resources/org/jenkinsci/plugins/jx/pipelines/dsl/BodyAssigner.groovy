package org.jenkinsci.plugins.jx.pipelines.dsl

import org.jenkinsci.plugins.jx.pipelines.arguments.JXPipelinesArguments
import org.jenkinsci.plugins.jx.pipelines.helpers.ConfigHelper

class BodyAssigner implements MethodMissingWrapper, Serializable {
    private Map<String,List> config
    private Map<String,?> populatedMap = [:]

    BodyAssigner(Map<String,List> config) {
        this.config = config
    }

    def methodMissing(String methodName, args) {
        def argValue
        if (args.length > 1) {
            argValue = args
        } else if (args.length == 1) {
            argValue = args[0]
        }


        def fieldName = config[methodName]?.get(0)
        def fieldClosure = config[methodName]?.get(1)
        if (fieldName != null && fieldClosure != null) {
            populatedMap[fieldName] = fieldClosure.call(argValue)
        }
    }

    protected <A extends JXPipelinesArguments> A argumentInstance(Class<A> klazz) {
        return ConfigHelper.populateBeanFromConfiguration(klazz, populatedMap)
    }
}
