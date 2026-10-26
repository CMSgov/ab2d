mvn -U clean install -s settings.xml -Dcheckstyle.skip -DskipTests -Dusername="${ARTIFACTORY_USER}" -Dpassword="${ARTIFACTORY_PASSWORD}" -Drepository_url="${ARTIFACTORY_URL}"
(cd contracts && ./gradlew clean publishToMavenLocal -x test --write-locks)
(cd events && ./gradlew clean publishToMavenLocal -x test --write-locks)
(cd lambdas &&  ./gradlew clean publishToMavenLocal -x test --write-locks)
