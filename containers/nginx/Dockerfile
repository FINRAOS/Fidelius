FROM fidelius/base:latest
MAINTAINER Fidelius Contributors

ARG proxy
ENV http_proxy ${proxy}
ENV https_proxy ${proxy}

RUN amazon-linux-extras install nginx1.12

RUN mkdir -p /opt/fidelius/static
ADD static/index.html /opt/fidelius/static/index.html
ADD vhost/nginx.conf /etc/nginx/nginx.conf

ENV http_proxy ""
ENV https_proxy ""

CMD ["nginx", "-g", "daemon off;"]