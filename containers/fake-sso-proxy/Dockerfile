FROM httpd:2.4.29
MAINTAINER Fidelius Contributors

ADD certs/server.crt /usr/local/apache2/certs/server.crt
ADD certs/server.key /usr/local/apache2/certs/server.key
ADD httpd/httpd.conf /usr/local/apache2/conf/httpd.conf
ADD httpd/httpd-vhosts.conf /usr/local/apache2/conf/extra/httpd-vhosts.conf
