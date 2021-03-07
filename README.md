# Load Balancer
## Dependencies
- sbt
- java
## Build 
Using the following command to build the project and create a docker image.
```
chmod +x build.sh && ./build.sh
```
## Runnig
```
docker run --name loadbalancer --env PORT=7000 -d -p 7000:7000 nachocode/cinvestav-ds-loadbalancer 
```
