pipeline {
  agent {
    label "jenkins-maven"
  }
  stages {
    stage('Maven Release') {
      steps {
        mavenFlow {
          cdOrganisation "jx-pipelines-plugin"
          useStaging true
          useSonatype true

          promoteArtifacts {
            pre {
              echo "====> hook invoked before promote artifacts!"
            }
          }
        }
      }
    }
  }
}