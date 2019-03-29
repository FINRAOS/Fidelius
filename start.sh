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

pushd containers
echo "$(tput setaf 6)"
echo "-------------------------------------------------------"
echo "|         Building base Fidelius containers           |"
echo "-------------------------------------------------------"
echo "$(tput sgr0)"
docker-compose build
popd
pushd demo-services/fake-account-service
echo "$(tput setaf 6)"
echo "-------------------------------------------------------"
echo "|         Building Fidelius demo services             |"
echo "-------------------------------------------------------"
echo "$(tput sgr0)"
mvn clean install
popd
pushd demo-services/fidelius-setup
echo "$(tput setaf 6)"
echo "-------------------------------------------------------"
echo "|         Building Fidelius setup container           |"
echo "-------------------------------------------------------"
echo "$(tput sgr0)"
mvn clean install
popd
pushd fidelius-sdk
echo "$(tput setaf 6)"
echo "-------------------------------------------------------"
echo "|         Building Fidelius sdk                       |"
echo "-------------------------------------------------------"
echo "$(tput sgr0)"
mvn clean install
popd
pushd fidelius-service
echo "$(tput setaf 6)"
echo "-------------------------------------------------------"
echo "|         Building Fidelius backend service           |"
echo "-------------------------------------------------------"
echo "$(tput sgr0)"
mvn clean install
popd
pushd fidelius-ui
echo "$(tput setaf 6)"
echo "-------------------------------------------------------"
echo "|         Building Fidelius UI                        |"
echo "-------------------------------------------------------"
echo "$(tput sgr0)"
npm install
npm run build
popd
echo "$(tput setaf 6)"
echo "-------------------------------------------------------"
echo "|         Building Fidelius containers                |"
echo "-------------------------------------------------------"
echo "$(tput sgr0)"
docker-compose -f setup-docker-compose.yml build
docker-compose -f local-docker-compose.yml build
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