pipeline {
  agent {
    label "jx-maven"
  }
  stages {
    stage('Maven Release') {
      steps {
        mavenFlow(
                cdOrganisation: "fabric8-jenkins",
                useStaging: true,
                useSonatype: true
        ) {
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