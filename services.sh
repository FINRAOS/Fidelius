#!/bin/bash

set -e

if [[ -z "${http_proxy}" ]]; then
    echo "No Proxy provided, setting http_proxy to be blank"
    export http_proxy=""
fi

if [[ -z "${AWS_DIRECTORY}" ]]; then
    echo "Please set AWS_DIRECTORY environment before proceeding"
    exit 1;
fi

if [[ -z "${AWS_ACCOUNT_NUMBER}" ]]; then
    echo "Please set AWS_ACCOUNT_NUMBER environment before proceeding"
    exit 1;
fi

echo "$(tput setaf 6)"
echo "-------------------------------------------------------"
echo "|              Starting Fidelius Setup                |"
echo "-------------------------------------------------------"
echo "$(tput sgr0)"
docker-compose -f setup-docker-compose.yml up
ret=$(docker wait fidelius_fidelius_setup_1)
if [[ $ret -eq 1 ]]; then
    echo "$(tput setaf 1)"
    echo "-------------------------------------------------------"
    echo "|        Error encountered running Fidelius setup     |"
    echo "|        Please fix and try again.                    |"
    echo "-------------------------------------------------------"
    echo "$(tput sgr0)"
else
    echo "$(tput setaf 6)"
    echo "-------------------------------------------------------"
    echo "|         Starting Fidelius Local Environment         |"
    echo "-------------------------------------------------------"
    echo "$(tput sgr0)"
    docker-compose -f local-docker-compose.yml up -d
    result1=$(docker container logs fidelius_fidelius-service_1)
    result2=$(docker container logs fidelius_fidelius_accounts_1)
    while [[ $result1 != *"Started FideliusApp"* || $result2 != *"Started FakeAccountServiceApplication"* ]]; do
        echo ""
        echo "Waiting for containers to start to launch browser..."
        sleep 5
        result1=$(docker container logs fidelius_fidelius-service_1)
        result2=$(docker container logs fidelius_fidelius_accounts_1)
    done
    open https://localhost:443
    open https://localhost:444
    open https://localhost:445
    echo "$(tput setaf 6)"
    echo "-------------------------------------------------------"
    echo "|              Fidelius User Endpoints                |"
    echo "|                                                     |"
    echo "|         DEV user -      https://localhost:443       |"
    echo "|         OPS user -      https://localhost:444       |"
    echo "|         MASTER user -   https://localhost:445       |"
    echo "|                                                     |"
    echo "-------------------------------------------------------"
    echo "$(tput sgr0)"
fi