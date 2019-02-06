package Build
node('build-slave') {
    try {
        String ANSI_GREEN = "\u001B[32m"
        String ANSI_NORMAL = "\u001B[0m"
        String ANSI_BOLD = "\u001B[1m"
        String ANSI_RED = "\u001B[31m"
        String ANSI_YELLOW = "\u001B[33m"

        ansiColor('xterm') {
            stage('Checkout') {
                cleanWs()
                def scmVars = checkout scm
                if (params.github_release_tag == "") {
                    checkout scm
                    commit_hash = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    branch_name = sh(script: 'git name-rev --name-only HEAD | rev | cut -d "/" -f1| rev', returnStdout: true).trim()
                    artifact_version = branch_name + "_" + commit_hash
                    println(ANSI_BOLD + ANSI_YELLOW + "github_release_tag not specified, using the latest commit hash: " + commit_hash + ANSI_NORMAL)
                }
                else {
                    checkout scm: [$class: 'GitSCM', branches: [[name: "refs/tags/${params.github_release_tag}"]], userRemoteConfigs: [[url: scmVars.GIT_URL]]]
                    artifact_version = params.github_release_tag
                    println(ANSI_BOLD + ANSI_YELLOW + "github_release_tag specified, building from: " + params.github_release_tag + ANSI_NORMAL)
                }
                echo "artifact_version: "+ artifact_version
            }


            stage('Build') {
                sh """
                    git clone https://github.com/project-sunbird/sunbird-auth.git -b release-1.13
                    git clone https://github.com/ahghatol/sunbird-devops-1.git project-sunbird-devops
                    wget https://www.dropbox.com/s/98kl792ukt3dgsu/keycloak-3.2.0.Final.tar.gz
                    wget https://jdbc.postgresql.org/download/postgresql-9.4.1212.jar
                    mkdir -p keycloak_build
                    tar -zcvf ansible/artifacts/sunbird/login.tar.gz ansible/artifacts/sunbird/*
                    tar -zcvf ansible/artifacts/email/email.tar.gz ansible/artifacts/email/*
                    cd sunbird-auth/keycloak/sms-provider && mvn package
                    cd templates && tar -zcvf templates.tar.gz *
                    cd ${WORKSPACE}
                    mv sunbird-auth/keycloak/sms-provider/target/keycloak-email-phone-autthenticator-1.0-SNAPSHOT.jar keycloak_build
                    mv sunbird-auth/keycloak/sms-provider/templates/templates.tar.gz keycloak_build
                    mv keycloak-3.2.0.Final.tar.gz keycloak_build
                    mv postgresql-9.4.1212.jar keycloak_build
                    mkdir -p keycloak_build/ansible-templates
                    mv project-sunbird-devops/ansible/roles/keycloak/templates/* keycloak_build/ansible-templates
                    mv ansible/artifacts/sunbird/login.tar.gz keycloak_build
                    mv ansible/artifacts/email/email.tar.gz keycloak_build
                    tar -zcvf keycloak_build.tar.gz keycloak_build
                """
            }

            stage('ArchiveArtifacts') {
                sh """
                        mkdir keycloak_artifacts
                        cp keycloak_build.tar.gz keycloak_artifacts
                        zip -j keycloak_artifacts.zip:${artifact_version} keycloak_artifacts/*
                    """
                archiveArtifacts artifacts: "keycloak_artifacts.zip:${artifact_version}", fingerprint: true, onlyIfSuccessful: true
                sh """echo {\\"artifact_name\\" : \\"keycloak_artifacts.zip\\", \\"artifact_version\\" : \\"${artifact_version}\\", \\"node_name\\" : \\"${env.NODE_NAME}\\"} > metadata.json"""
                archiveArtifacts "metadata.json"
                currentBuild.description = "${build_tag}"
            }
        }

    }
    catch (err) {
        currentBuild.result = "FAILURE"
        throw err
    }
}
