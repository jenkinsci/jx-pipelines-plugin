package org.jenkinsci.plugins.jx.pipelines.dsl

import org.jenkinsci.plugins.jx.pipelines.arguments.PromoteImagesArguments
import org.jenkinsci.plugins.workflow.cps.CpsScript

class PromoteImages {
  private CpsScript script

  PromoteImages(CpsScript script) {
    this.script = script
  }

  def call(PromoteImagesArguments config) {
    if (!config.org) {
      script.error 'Docker Organisation config missing'
    }

    if (!config.toRegistry) {
      script.error 'Promote To Docker Registry config missing'
    }

    def flow = new CommonFunctions(script)
    def registryPrefix = flow.dockerRegistryPrefix()
    def org = config.org
    def images = config.images
    def tag = config.tag
    def toRegistry = config.toRegistry

    return flow.doStepExecution(config) {
      if (tag && toRegistry) {
        script.container(config.containerName) {
          for (int i = 0; i < images.size(); i++) {
            def image = images[i]

            // if we're running on a single node then we already have the image on this host so no need to pull image
            if (flow.isSingleNode()) {
              script.sh "docker tag ${org}/${image}:${tag} ${toRegistry}/${org}/${image}:${tag}"
            } else {
              script.sh "docker pull ${registryPrefix}fabric8/${image}:${tag}"
              script.sh "docker tag ${registryPrefix}${org}/${image}:${tag} ${toRegistry}/${org}/${image}:${tag}"
            }

            script.retry(3) {
              script.sh "docker push ${toRegistry}/${org}/${image}:${tag}"
            }
          }
        }
      }
    }
  }
}