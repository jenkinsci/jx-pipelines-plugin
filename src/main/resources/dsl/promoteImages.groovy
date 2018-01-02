package dsl

import org.jenkinsci.plugins.jx.pipelines.arguments.PromoteImagesArguments

def call(PromoteImagesArguments config) {
  if (!config.org) {
    error 'Docker Organisation config missing'
  }

  if (!config.toRegistry) {
    error 'Promote To Docker Registry config missing'
  }

  def flow = new CommonFunctions()
  def registryPrefix = flow.dockerRegistryPrefix()
  def org = config.org
  def images = config.images
  def tag = config.tag
  def toRegistry = config.toRegistry

  return flow.doStepExecution(config.stepExtension) {
    if (tag && toRegistry) {
      container(config.containerName) {
        for (int i = 0; i < images.size(); i++) {
          image = images[i]

          // if we're running on a single node then we already have the image on this host so no need to pull image
          if (flow.isSingleNode()) {
            sh "docker tag ${org}/${image}:${tag} ${toRegistry}/${org}/${image}:${tag}"
          } else {
            sh "docker pull ${registryPrefix}fabric8/${image}:${tag}"
            sh "docker tag ${registryPrefix}${org}/${image}:${tag} ${toRegistry}/${org}/${image}:${tag}"
          }

          retry(3) {
            sh "docker push ${toRegistry}/${org}/${image}:${tag}"
          }
        }
      }
    }
  }
}
