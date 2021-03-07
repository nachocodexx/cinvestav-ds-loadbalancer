FROM openjdk:8
WORKDIR /app/src
COPY ./target/universal/cinvestav-ds-loadbalancer-0.1.zip /app/src/app.zip
RUN unzip /app/src/app.zip
CMD ls
ENTRYPOINT ["/app/src/cinvestav-ds-loadbalancer-0.1/bin/cinvestav-ds-loadbalancer"]