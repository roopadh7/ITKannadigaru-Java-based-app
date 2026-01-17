pipeline{
    agent any // decided which node to run

    tools {
        jdk 'java-17'
        maven 'maven'
    }

    environment {
        IMAGE_NAME = "manojkrishnappa/itkannadigaru-blogpost:${GIT_COMMIT}"
        AWS_REGION = "us-west-2"
        CLUSTER_NAME = "itkannadigaru-cluster"
        NAMESPACE = "itkannadigaru"
    }

    stages{
        stage('git-checkout'){
            steps{
                git url: 'https://github.com/ManojKRISHNAPPA/ITKannadigaru-Java-based-app.git', branch: 'karthik'
            }
            
        }

        stage('Compile'){
            steps{
                sh '''
                    mvn compile
                '''
            }
        }
        stage('packaging'){
            steps{
                sh '''
                    mvn clean package
                '''
            }
        }
        stage('docker-build'){
            steps{
                sh '''
                    printenv
                    docker build -t ${IMAGE_NAME} .
                '''
            }
        }
        // stage('Docker-testing'){
        //     steps{
        //         sh '''
        //             docker kill itkannadigaru-blogpost-test
        //             docker rm itkannadigaru-blogpost-test
        //             docker run -it -d --name itkannadigaru-blogpost-test -p 9000:8080 ${IMAGE_NAME}
        //         '''
        //     }
        // }   

        stage('Login to Docker Hub') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-hub-creds', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                        // Login to Docker Hub
                        sh "echo $DOCKER_PASSWORD | docker login -u $DOCKER_USERNAME --password-stdin"
                    }
                }
            }
        }  

        stage('Push to dockerhub'){
            steps{
                sh '''
                    docker push ${IMAGE_NAME}
                '''
            }
        }
    }
}