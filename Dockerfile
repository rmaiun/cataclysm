FROM openjdk:11-jre-slim
MAINTAINER @RMaiun
RUN apt-get update; apt-get install -y fontconfig libfreetype6
COPY build/libs/cataclysm-2.1.1.jar /opt/cataclysm-2.1.1.jar
ENV USER="" PASS="" HOST="host.docker.internal"
EXPOSE 8080
#ENTRYPOINT ["/usr/bin/java"]
CMD java -jar -Dtoken=$TOKEN -Ddb.user=$USER -Ddb.password=$PASS -Ddb.host=$HOST /opt/cataclysm-2.1.1.jar