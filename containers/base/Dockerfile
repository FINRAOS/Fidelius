FROM amazonlinux:2
MAINTAINER Fidelius Contributors

RUN yum install -y openssl
ADD /scripts/selfsign.sh /tmp/selfsign.sh
RUN chmod a+rx /tmp/selfsign.sh
RUN /tmp/selfsign.sh

