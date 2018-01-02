package org.jenkinsci.plugins.jx.pipelines.dsl

class BodyAssigner implements MethodMissingWrapper, Serializable {
    private Map config

    BodyAssigner(Map config) {
        this.config = config
    }

    def methodMissing(String methodName, args) {
        def argValue
        if (args.length > 1) {
            argValue = args
        } else if (args.length == 1) {
            argValue = args[0]
        }

        config[methodName].call(argValue)
    }
}
