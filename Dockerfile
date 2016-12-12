# Docker StarChat
# VERSION 0.1

# the base image is a trusted ubuntu build with java 9 (https://index.docker.io/u/dockerfile/java/)
FROM java:9

MAINTAINER Angelo Leto, angelo@getjenny.com

# we need this because the workdir is modified in dockerfile/java
WORKDIR /

# run the (java) server as the daemon user
USER daemon

# copy the locally built fat-jar to the image
ADD target/universal/starchat-0.1 /app/starchat-0.1

# run the server when a container based on this image is being run
ENTRYPOINT [ "/app/starchat-0.1/bin/starchat" ]

# the server binds to 8888 - expose that port
EXPOSE 8888
