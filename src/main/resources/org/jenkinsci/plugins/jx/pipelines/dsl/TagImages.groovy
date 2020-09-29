package org.jenkinsci.plugins.jx.pipelines.dsl

import org.jenkinsci.plugins.jx.pipelines.arguments.TagImagesArguments
import org.jenkinsci.plugins.workflow.cps.CpsScript

class TagImages {
  private CpsScript script

  TagImages(CpsScript script) {
    this.script = script
  }

  def call(TagImagesArguments config) {

    def images = config.images
    def tag = config.tag
    def registryPrefix = flow.dockerRegistryPrefix()
    def flow = new CommonFunctions(script)

    return flow.doStepExecution(config) {
      if (tag && images && images.size() > 0) {
        //stage "tag images"
        script.container(config.containerName) {
          for (int i = 0; i < images.size(); i++) {
            def image = images[i]
            script.retry(3) {
              script.sh "docker pull ${registryPrefix}fabric8/${image}:${tag}"
              script.sh "docker tag  ${registryPrefix}fabric8/${image}:${tag} docker.io/fabric8/${image}:${tag}"
              script.sh "docker push docker.io/fabric8/${image}:${tag}"
            }
          }
        }
      }
    }
  }
}