#!/bin/bash
sbt clean && sbt compile && sbt universal:packageBin && docker build -t nachocode/cinvestav-ds-loadbalancer .