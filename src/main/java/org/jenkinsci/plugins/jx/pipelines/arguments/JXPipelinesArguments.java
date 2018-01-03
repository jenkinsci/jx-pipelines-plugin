package org.jenkinsci.plugins.jx.pipelines.arguments;

import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import java.io.Serializable;

public abstract class JXPipelinesArguments<T extends JXPipelinesArguments<T>> extends AbstractDescribableImpl<T> implements Serializable {

    public abstract static class JXPipelinesArgumentsDescriptor<A extends JXPipelinesArguments<A>> extends Descriptor<A> {

    }
}
