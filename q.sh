#!/usr/bin/env bash

set -e

scriptDir=""

os=`uname`

# sed has different parameters for in-place editing on Mac and Linux
sedIcmd=''

case ${os} in
    Darwin)
        scriptDir=$(cd "$(dirname "$0")"; pwd)
        sedIcmd="sudo sed -i ''"
    ;;
    Linux)
        scriptDir="$(dirname "$(readlink -f "$0")")"
        sedIcmd="sudo sed -i"
    ;;
    *)
       echo "Unknown OS."
       exit 1
    ;;
esac

cd ${scriptDir}
script="${scriptDir}/q.sh"

function help() {
    echo "Commands:"
    echo "  build {artifact_name} [docker_args...]    -  build {artifact_name} (possible value: 'q')"
    echo "  dev-build-push {artifact_name}            -  build {artifact_name} (see above) and pushes its image into"
    echo "                                               default registry with default prefix and ':dev' tag"
}

#
# Set global environment variables
#
function set_common_env(){
    if [[ -z "${GOOGLE_DOCKER_REGISTRY_HOST}" ]]; then
        export GOOGLE_DOCKER_REGISTRY_HOST="gcr.io"
    fi

    if [[ -z "${DOCKER_IMAGE_PREFIX}" ]]; then
        export DOCKER_IMAGE_PREFIX="q-"
    fi

    if [[ -z "${GOOGLE_PROJECT_ID}" ]]; then
        export GOOGLE_PROJECT_ID="$(gcloud config get-value project 2> /dev/null)"
    fi

    export IMAGE_NAME_PREFIX="${GOOGLE_DOCKER_REGISTRY_HOST}/${GOOGLE_PROJECT_ID}/${DOCKER_IMAGE_PREFIX}"
}

function set_docker_artifact(){
    if [[ -z "${DOCKER_ARTIFACT}" ]]; then
      DOCKER_ARTIFACT=$1
    fi

    case ${DOCKER_ARTIFACT} in
       *)
           DOCKER_ARTIFACT="core"
           ;;
    esac
}

#
# Set environment variables for building Docker images
#
function set_docker_env(){
    set_docker_artifact $1

    set_common_env

    export IMAGE_NAME="${IMAGE_NAME_PREFIX}${DOCKER_ARTIFACT}"

    STAGES=( )
    DOCKERFILE="Dockerfile"
    TARGET_STAGE=""

    case ${DOCKER_ARTIFACT} in
        core)
            STAGES+=( "builder-base" "runtime-base" )
            TARGET_STAGE="runtime"
            DOCKERFILE="deployment/dockerfiles/Dockerfile.java"
            ;;
        *)
            echo "Unknown artifact ${DOCKER_ARTIFACT}"
            ;;
    esac
}

usage="q.sh [command] [arguments]


Commands:

    build [component]
      build Docker image for Q component

    push [component]
      push Docker image for Q component to gcr.io

    pull [component]
      pull Docker image for Q component from gcr.io

    secret [gcp|jwt|x-auth|grafana-github] [create|remove]
      create or remove k8s secrets for GCP

    deploy-kafka
      deploy Kafka in cluster

    deploy-monitoring
      deploy Prometheus and Grafana and apply all other required monitoting kubernetes configs

    deploy [component|secrets]
      deploy Q component to k8s cluster.

      Options:
       -e, --env
          environment: dev / prod
       -t, --tag
          tag Docker images
        --dry-run
           dry run


      Examples:

       # deploy secrets to dev environment
       ./q.sh deploy -e dev secrets

       # deploy app-endpoints-bm to dev environment
       ./q.sh deploy -e dev app-endpoints-bm


    refresh-pods [component]
      restart all pods for specified Q component

    wait [component]
      waits until Q component is fully deployed

    build-push-refresh-pods [component] -e [env] -t [tag]
      build and upload Docker image, and then refresh pods for specified Q component, no kubectl apply

      Example:
       # for development enviroment
       ./q.sh build-push-refresh-pods app-endpoints-admin -e dev -t b142
       # for production enviroment
       ./q.sh build-push-refresh-pods app-endpoints-admin -e dev -t v2.0.1


    build-push-deploy-refresh-pods [component] -e [env] -t [tag]
      build and upload Docker image, update deployments, and then refresh pods for specified Q component.

      Example:
       # for development enviroment
       ./q.sh build-push-deploy-refresh-pods app-endpoints-admin -e dev -t b142
       # for production enviroment
       ./q.sh build-push-deploy-refresh-pods app-endpoints-admin -e dev -t v2.0.1

    build-push-deploy-refresh-pods-all -e [env] -t [tag]
      deploys all Q components to cluster

    shell
      login to shell pod inside cluster

    shell-start
      start shell pod

    shell-reset
      reset shell pod

    shell-delete
      delete shell pod
"


function set_ktype(){
  KTYPE="StatefulSet"
#  if [[ ${ARTIFACT} == *"deldn2dn"* ]]; then
#    KTYPE="Deployment"
#  fi
}

ACTION=$1
shift

case ${ACTION} in
    -h)
       echo "$usage"
       exit
       ;;
    build)
        # export DOCKER_BUILDKIT=1

        set_docker_artifact $1
        shift

        set_docker_env "${DOCKER_ARTIFACT}"

        docker_args=()

        cache_prefix=""

        while [[ $# > 0 ]]
        do
            key="$1"
            shift
            case "${key}" in
                -t|--tag)
                    docker_args+=( -t "${IMAGE_NAME}:$1" )
                    shift
                    ;;
                --raw-tag)
                    docker_args+=( -t "$1" )
                    shift
                    ;;
                --cache-prefix)
                    cache_prefix="$1"
                    docker_args+=( -t "${IMAGE_NAME}:$cache_prefix" )
                    shift
                    ;;
                -e|--env)
                    shift
                    ;;
                *)
                    echo "build: Unknown option $key"
                    exit 1
                    ;;
            esac
        done

        docker_cache_args=()
        if [[ ! -z "${cache_prefix}" ]]; then
            cache_image="${IMAGE_NAME}:$cache_prefix"
            for stage in "${STAGES[@]}"; do
                cache_image_stage="${cache_image}-${stage}"
                echo "Trying to pull cache image: ${cache_image_stage}"
                docker pull "${cache_image_stage}" && docker_cache_args+=( --cache-from "${cache_image_stage}" ) || echo "WARN: Pull failed, building without cache."

                echo "Building stage: ${stage}"
                docker build "${docker_cache_args[@]}" -f ${DOCKERFILE} -t ${cache_image_stage} --target ${stage} .
            done
            docker pull "${cache_image}" && docker_cache_args+=( --cache-from "${cache_image}" ) || echo "WARN: Pull failed, building without cache."
            docker_args+=( "${docker_cache_args[@]}" )
        fi

        if [[ ! -z "${TARGET_STAGE}" ]]; then
            docker_args+=( --target "${TARGET_STAGE}" )
        fi

        echo docker build "${docker_args[@]}" -f ${DOCKERFILE} .
        docker build "${docker_args[@]}" -f ${DOCKERFILE} .

        ;;

    push|pull)
        set_docker_artifact $1
        shift

        set_docker_env "${ARTIFACT}"

        while [[ $# > 0 ]]
        do
            key="$1"
            shift
            case "${key}" in
                -t|--tag)
                    echo "${ACTION}: ${IMAGE_NAME}:$1"
                    docker ${ACTION} "${IMAGE_NAME}:$1"
                    shift
                    ;;
                -e|--env)
                    echo "-e"
                    shift
                    ;;
                --raw-tag)
                    echo "${ACTION}: $1"
                    docker ${ACTION} "$1"
                    shift
                    ;;
                --cache-prefix)
                    cache_prefix="$1"
                    cache_image="${IMAGE_NAME}:$cache_prefix"
                    for stage in "${STAGES[@]}"; do
                        cache_image_stage="${cache_image}-${stage}"
                        echo "${ACTION}: ${cache_image_stage}"
                        docker ${ACTION} ${cache_image_stage}
                    done
                    echo "${ACTION}: ${cache_image}"
                    docker ${ACTION} ${cache_image}
                    shift
                    ;;
                *)
                    echo "push: Unknown option $key"
                    exit 1
                    ;;
            esac
        done
        ;;

    secret)
        SECRET=$1
        shift

        if [[ $# -eq 0 ]]; then
            echo "Please specify secret action: 'create' or 'remove'"
        fi

        SECRET_ACTION=$1
        shift

        case ${SECRET} in
            gcp)
                if [[ "$SECRET_ACTION" == "remove" ]]; then
                    rm deployment/k8s/secrets/010-gcp.credentials.secret.json
                elif [[ "$SECRET_ACTION" == "create" ]]; then
                    echo "Generating GCP credentials."
                    echo ""
                    echo "Selected gcloud project: $(gcloud config get-value project 2> /dev/null)"
                    echo ""
                    echo "Example: default-account@qplatform.iam.gserviceaccount.com"
                    echo -n "Specify the name of service account: "
                    read SERVICE_ACCOUNT_NAME
                    gcloud iam service-accounts keys create deployment/k8s/secrets/010-gcp.credentials.secret.json \
                           --iam-account ${SERVICE_ACCOUNT_NAME}
                    echo "Done."
                else
                    echo "Unknown action: $SECRET_ACTION"
                fi
                ;;

            jwt)
                if [[ "$SECRET_ACTION" == "remove" ]]; then
                    rm deployment/k8s/secrets/030-jwt-key.secret.txt
                elif [[ "$SECRET_ACTION" == "create" ]]; then
                    echo -n "Please enter JWT Key: "
                    read -s JWT_KEY
                    echo ""
                    if [[ -z "${JWT_KEY}" ]]; then
                      echo "Error: empty JWT key."
                      exit 1
                    fi
                    echo -n "${JWT_KEY}" > deployment/k8s/secrets/030-jwt-key.secret.txt
                    echo "Done."
                else
                    echo "Unknown action: $SECRET_ACTION"
                fi
                ;;

            x-auth)
                if [[ "$SECRET_ACTION" == "remove" ]]; then
                    rm deployment/k8s/secrets/040-x-auth-token.secret.txt
                elif [[ "$SECRET_ACTION" == "create" ]]; then
                    echo -n "Please enter X-Auth Token: "
                    read -s X_AUTH_TOKEN
                    echo ""
                    echo -n "${X_AUTH_TOKEN}" > deployment/k8s/secrets/040-x-auth-token.secret.txt
                    echo "Done."
                else
                    echo "Unknown action: $SECRET_ACTION"
                fi
                ;;

            grafana-github)
                if [[ "$SECRET_ACTION" == "remove" ]]; then
                    rm deployment/k8s/monitoring/200-grafana-github-client-id.secret.txt
                    rm deployment/k8s/monitoring/200-grafana-github-client-secret.secret.txt
                elif [[ "$SECRET_ACTION" == "create" ]]; then
                    echo "Setting up Grafana OAuth with GitHub."

                    echo -n "Please enter GitHub App Client ID: "
                    read CLIENT_ID
                    echo -n "${CLIENT_ID}" > deployment/k8s/monitoring/200-grafana-github-client-id.secret.txt

                    echo -n "Please enter GitHub App Client Secret: "
                    read -s CLIENT_SECRET
                    echo ""
                    echo -n "${CLIENT_SECRET}" > deployment/k8s/monitoring/200-grafana-github-client-secret.secret.txt

                    echo -n "Please enter GitHub Organisation Name to limit access: "
                    read ORGANISATION_NAME
                    echo -n "${ORGANISATION_NAME}" > deployment/k8s/monitoring/200-grafana-github-organisation-name.secret.txt

                    echo -n "Please enter GitHub Team IDs (e.g. 150,300, see this answer http://disq.us/p/23fdz12) to limit access: "
                    read TEAM_IDS
                    echo -n "${TEAM_IDS}" > deployment/k8s/monitoring/200-grafana-github-team-ids.secret.txt

                    echo "Done."
                else
                    echo "Unknown action: $SECRET_ACTION"
                fi
                ;;
        esac
        ;;

    deploy-kafka)
        cd deployment/k8s/kafka
        kubectl apply -k .
        exit
        ;;

    deploy-monitoring)
        cd deployment/k8s/monitoring
        kubectl apply -k .
        exit
        ;;

    deploy)
        set_common_env

        cd ${scriptDir}

        WDIR=$(cat /dev/urandom | env LC_ALL=C tr -dc 'a-zA-Z0-9' | head -c 20)
        mkdir -p .workdir/${WDIR}
        function finish {
            rm -rf ${scriptDir}/.workdir/${WDIR}
        }
        trap finish EXIT

        cd .workdir/${WDIR}

        ENV="dev"
        IMAGE_TAG="dev"
        ARTIFACTS=()
        DRY_RUN=false

        while [[ $# > 0 ]]
        do
            key="$1"
            shift
            case "${key}" in
                -e|--env)
                    ENV="$1"
                    shift
                    ;;
                -t|--tag)
                    IMAGE_TAG="$1"
                    shift
                    ;;
                --dry-run)
                    DRY_RUN=true
                    ;;
                secrets|*)
                    ARTIFACTS+=( "${key}" )
                    ;;
                *)
                    echo "deploy: Unknown option $key"
                    exit 1
                    ;;
            esac
        done

        NAMESPACE="q-${ENV}"

        IMAGES=( "q" )

        # Namespace for the environment
        echo "apiVersion: v1" > namespace.yaml
        echo "kind: Namespace" >> namespace.yaml
        echo "metadata:" >> namespace.yaml
        echo "  name: ${NAMESPACE}" >> namespace.yaml

        # Creating image name substitution list based on the --tag option provided
        echo "images:" > kustomization.yaml
        for IMAGE in "${IMAGES[@]}"; do
            echo "  - name: ${IMAGE}" >> kustomization.yaml
            echo "    newName: ${IMAGE_NAME_PREFIX}${IMAGE}" >> kustomization.yaml
            echo "    newTag: ${IMAGE_TAG}" >> kustomization.yaml
        done

        # Global variables
        echo "" >> kustomization.yaml
        echo "configMapGenerator:" >> kustomization.yaml
        echo "  - name: global-env-vars" >> kustomization.yaml
        echo "    literals:" >> kustomization.yaml
        echo "      - ENV=${ENV}" >> kustomization.yaml

        echo "" >> kustomization.yaml

        echo "generatorOptions:" >> kustomization.yaml
        echo "    disableNameSuffixHash: yes" >> kustomization.yaml

        echo "" >> kustomization.yaml

        # Set each object with the environment-specific namespace
        echo "namespace: ${NAMESPACE}" >> kustomization.yaml
        echo "" >> kustomization.yaml
        echo "resources:" >> kustomization.yaml
        echo "  - namespace.yaml" >> kustomization.yaml
        echo "" >> kustomization.yaml

        # Listing which objects to create
        echo "bases:" >> kustomization.yaml
        for ARTIFACT in "${ARTIFACTS[@]}"; do
            echo "  - ../../deployment/k8s/${ARTIFACT}" >> kustomization.yaml
        done

        if [[ "${DRY_RUN}" == true ]]; then
            # kubectl apply -k . --dry-run -o=yaml
            kustomize build .
        else
            kubectl apply -k .
        fi

        ;;

    refresh-pods|wait)
        ENV="dev"

        ARTIFACTS=()
        while [[ $# > 0 ]]
        do
            key="$1"
            shift
            case "${key}" in
                -e|--env)
                    ENV="$1"
                    shift
                    ;;
                -t|--tag)
                    # Ignore
                    shift
                    ;;
                *)
                    ARTIFACTS+=( "${key}" )
                    ;;
            esac
        done

        NAMESPACE="q-${ENV}"

        case ${ACTION} in
            refresh-pods)
                for ARTIFACT in "${ARTIFACTS[@]}"; do
                    set_ktype
                    kubectl patch $KTYPE -n ${NAMESPACE} ${ARTIFACT} -p \
                        "{\"spec\":{\"template\":{\"metadata\":{\"labels\":{\"date\":\"`date +'%s'`\"}}}}}"
                done
                ;;
            wait)
                for ARTIFACT in "${ARTIFACTS[@]}"; do
                    set_ktype
                    kubectl rollout status $KTYPE -n ${NAMESPACE} ${ARTIFACT}
                done
                ;;
        esac

        ;;

    build-push-deploy-refresh-pods)
        ${script} build "${@}"
        ${script} push "${@}"
        ${script} deploy "${@}"
        ${script} refresh-pods "${@}"
        ${script} wait "${@}"
        ;;

    build-push-refresh-pods)
        ${script} build "${@}"
        ${script} push "${@}"
        ${script} refresh-pods "${@}"
        ${script} wait "${@}"
        ;;

    build-push-deploy-refresh-pods-all)
        envr="dev"
        tag="dev"
        while [[ $# > 0 ]]
        do
            key="$1"
            shift
            case "${key}" in
                -e|--env)
                    echo "enviroment $1"
                    envr="$1"
                    shift
                    ;;
                -t|--tag)
                    echo "tag $1"
                    tag="$1"
                    shift
                    ;;
                *)
                    echo "build-push-deploy-refresh-pods-all: Unknown option $key"
                    exit 1
                    ;;
            esac
        done

        for artifact in "app-endpoints-admin" "app-qgraf" "app-endpoints-theories" "app-endpoints-processes" "app-endpoints-raw-diagrams";
        do
          ${script} build-push-deploy-refresh-pods ${artifact} -e $envr -t $tag
        done;
        ;;

    dev-build-push-refresh-pods)
        ARTIFACT=$1
        ${script} build-push-refresh-pods ${ARTIFACT} -e dev -t dev
        ;;

    dev-build-push-deploy-refresh-pods)
        ARTIFACT=$1
        ${script} build-push-deploy-refresh-pods ${ARTIFACT} -e dev -t dev
        ;;

    dev-build-push-deploy-refresh-pods-all)
        for artifact in "app-endpoints-admin" "app-preferences" "app-learning" "app-endpoints-weights" "app-endpoints-preferences" "app-endpoints-bm" "app-endpoints-generator";
        do
          ${script} dev-build-push-deploy-refresh-pods ${artifact}
        done;
        ;;

    logs|pod-shell)
        ENV="dev"

        ARTIFACT=$1
        shift

        KUBECTL_OPTIONS=()

        while [[ $# > 0 ]]
        do
            key="$1"
            shift
            case "${key}" in
                -e|--env)
                    ENV="$1"
                    shift
                    ;;
                -t|--tag)
                    # Ignore
                    shift
                    ;;
                *)
                    KUBECTL_OPTIONS+=( "$key" )
                    ;;
            esac
        done

        NAMESPACE="q-${ENV}"

        POD_NAME=$(kubectl get pod -l app=${ARTIFACT} -n ${NAMESPACE} -o custom-columns=:metadata.name | tail -n +2 | sort -R | head -n 1)

        echo "Pod name: ${POD_NAME}"

        if [[ "${ACTION}" == "logs" ]]; then
          kubectl logs -n ${NAMESPACE} ${POD_NAME} "${KUBECTL_OPTIONS[@]}"
        else
          kubectl exec -n ${NAMESPACE} -it ${POD_NAME} /bin/bash
        fi

        ;;

    shell)
        kubectl exec -n q-dev -it shell -- /bin/su theuser
        ;;

    shell-start)
#        kubectl apply -f deployment/k8s/shell/010-pvc.yaml || :
        kubectl apply -f deployment/k8s/shell/020-shell.yaml || :
        ;;

    shell-reset)
        kubectl delete -f deployment/k8s/shell/020-shell.yaml || :
        kubectl apply -f deployment/k8s/shell/010-pvc.yaml || :
        kubectl apply -f deployment/k8s/shell/020-shell.yaml || :
        ;;

    shell-delete)
        kubectl delete -f deployment/k8s/shell/020-shell.yaml || :
        kubectl delete -f deployment/k8s/shell/010-pvc.yaml || :
        ;;

    almond)
        kubectl cp ../qPlatform/ q-dev/almond:/home/root/
        kubectl exec -n q-dev -it almond -- bash -c "apt-get update && apt-get install --no-install-recommends -y maven && cd /home/root/qPlatform/q.core/ && mvn clean install -DskipTests && cp -r /root/.m2/ /home/root/.m2/ && cp -r /root/.m2/ /home/jovyan/.m2/" || :
        kubectl port-forward --namespace q-dev almond 8889:8889
        ;;

    almond-refresh)
        kubectl cp ../qPlatform/ q-dev/almond:/home/root/
        kubectl exec -n q-dev -it almond -- bash -c "apt-get update && apt-get install --no-install-recommends -y maven && cd /home/root/qPlatform/q.core/ && mvn clean install -DskipTests && cp -r /root/.m2/ /home/root/.m2/ && cp -r /root/.m2/ /home/jovyan/.m2/" || :
        ;;

    almond-start)
#        kubectl apply -f deployment/k8s/almond/010-pvc.yaml || :
        kubectl apply -f deployment/k8s/almond/020-almond.yaml || :
#        kubectl apply -f deployment/k8s/almond/030-almond-service.yaml || :
        ;;

    almond-reset)
        kubectl delete -f deployment/k8s/almond/020-almond.yaml || :
#        kubectl apply -f deployment/k8s/almond/010-pvc.yaml || :
        kubectl apply -f deployment/k8s/almond/020-almond.yaml || :
#        kubectl apply -f deployment/k8s/almond/030-almond-service.yaml || :
        ;;

    almond-delete)
        kubectl delete -f deployment/k8s/almond/030-almond-service.yaml || :
        kubectl delete -f deployment/k8s/almond/020-almond.yaml || :
        kubectl delete -f deployment/k8s/almond/010-pvc.yaml || :
        ;;

    *)
        echo "Unknown option $key"
        exit 1
        ;;
esac
