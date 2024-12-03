# Define the location of the Spring Boot JAR file and other variables
JAR_FILE="../target/${artifactId}-${version}.jar"
LOG_FILE="${artifactId}-${version}-shell.log"
JAVA_OPTS="-Xms512m -Xmx1024m -Dspring.profiles.active=clear-ticket"

GROUP_ID="${groupId}"
ARTIFACT_ID="${artifactId}"
ARTIFACT_VERSION="${version}"
