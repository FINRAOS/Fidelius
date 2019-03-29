#!/bin/bash

set -e

echo "$(tput setaf 6)"
echo "-------------------------------------------------------"
echo "|         Stopping Fidelius Services                  |"
echo "-------------------------------------------------------"
echo "$(tput sgr0)"
docker-compose -f local-docker-compose.yml down
echo "$(tput setaf 6)"
echo "-------------------------------------------------------"
echo "|         Successfully Stopped Fidelius Services      |"
echo "-------------------------------------------------------"
echo "$(tput sgr0)"