package dsl

import org.jenkinsci.plugins.jx.pipelines.arguments.TagImagesArguments

def call(TagImagesArguments config) {

  def images = config.images
  def tag = config.tag
  def registryPrefix = flow.dockerRegistryPrefix()
  def flow = new CommonFunctions()

  return flow.doStepExecution(config.stepExtension) {
    if (tag && images && images.size() > 0) {
      //stage "tag images"
      container(config.containerName) {
        for (int i = 0; i < images.size(); i++) {
          def image = images[i]
          retry(3) {
            sh "docker pull ${registryPrefix}fabric8/${image}:${tag}"
            sh "docker tag  ${registryPrefix}fabric8/${image}:${tag} docker.io/fabric8/${image}:${tag}"
            sh "docker push docker.io/fabric8/${image}:${tag}"
          }
        }
      }
    }
  }
}
