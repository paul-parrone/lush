#!/bin/bash

# Define the location of the Spring Boot JAR file and other variables
#JAR_FILE="../target/${artifactId}-${version}.jar"
#LOG_FILE="${artifactId}-${version}-shell.log"
#JAVA_OPTS="-Xms512m -Xmx1024m -Dspring.profiles.active=clear-ticket"

# Function to start the Spring Boot application
   start_app() {
       echo "Starting Spring Boot application..."
       source ./rt-env.sh
       nohup java $JAVA_OPTS -jar $JAR_FILE > $LOG_FILE 2>&1 & echo $! > spring-boot-app.pid
       cat spring-boot-app.pid
   }

# Function to stop the Spring Boot application
stop_app() {
    if [ -f "spring-boot-app.pid" ]; then
        PID=$(cat spring-boot-app.pid)
        echo "Stopping Spring Boot application with PID $PID..."
        kill $PID
        rm spring-boot-app.pid
        echo "Spring Boot application stopped."
    else
        echo "No PID file found. Is the Spring Boot application running?"
    fi
}

# Function to show the application status
status_app() {
    if [ -f "spring-boot-app.pid" ]; then
        PID=$(cat spring-boot-app.pid)
        if ps -p $PID > /dev/null; then
            echo "Spring Boot application is running with PID $PID."
        else
            echo "Spring Boot application is not running, but PID file exists."
        fi
    else
        echo "Spring Boot application is not running."
    fi
}

# Entry point to the script
case "$1" in
    start)
        start_app
        ;;
    stop)
        stop_app
        ;;
    status)
        status_app
        ;;
    restart)
        stop_app
        start_app
        ;;
    *)
        echo "Usage: $0 {start|stop|status|restart}"
        exit 1
esac

exit 0