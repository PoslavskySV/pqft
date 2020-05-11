# We'll start from a minimal Jupyter image
# Also see https://jupyter-docker-stacks.readthedocs.io/en/latest/using/selecting.html
FROM jupyter/base-notebook

# Install Scala and Almond
# Based on Dockerfiles from https://github.com/almond-sh/almond
# and https://github.com/almond-sh/docker-images/tree/coursier
USER root

RUN apt-get -y update && \
  apt-get install --no-install-recommends -y \
  curl \
  openjdk-12-jre-headless \
  ca-certificates-java && \
apt-get clean && \
  rm -rf /var/lib/apt/lists/*

RUN curl -Lo /usr/local/bin/coursier https://github.com/coursier/coursier/releases/download/v1.1.0-M11/coursier && \
    chmod +x /usr/local/bin/coursier

USER jovyan
ENV SCALA_VERSION 2.12.8
ENV ALMOND_VERSION 0.6.0

# Install Almond kernel
RUN coursier bootstrap \
      -r jitpack \
      -i user -I user:sh.almond:scala-kernel-api_$SCALA_VERSION:$ALMOND_VERSION \
      sh.almond:scala-kernel_$SCALA_VERSION:$ALMOND_VERSION \
      --default=true --sources \
      -o almond && \
    ./almond --install --log info --metabrowse --id scala$SCALA_VERSION --display-name "Scala $SCALA_VERSION" && \
    rm -f almond && \
    rm -fr .cache


ENTRYPOINT ["jupyter", "notebook"]
