#!/bin/bash
#
# CODENVY CONFIDENTIAL
# ________________
#
# [2012] - [2015] Codenvy, S.A.
# All Rights Reserved.
# NOTICE: All information contained herein is, and remains
# the property of Codenvy S.A. and its suppliers,
# if any. The intellectual and technical concepts contained
# herein are proprietary to Codenvy S.A.
# and its suppliers and may be covered by U.S. and Foreign Patents,
# patents in process, and are protected by trade secret or copyright law.
# Dissemination of this information or reproduction of this material
# is strictly forbidden unless prior written permission is obtained
# from Codenvy S.A..
#

# load lib.sh from path stored in parameter 1
. $1

printAndLog "TEST CASE: Update previous version of single-node Codenvy On Premise to latest version"
vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

WORKSPACE_NAME="workspace-1"
PROJECT_NAME="project-1"

# install previous version
installCodenvy ${PREV_CODENVY_VERSION}
validateInstalledCodenvyVersion ${PREV_CODENVY_VERSION}

# create data: add account, workspace, project, user, factory
authWithoutRealmAndServerDns "admin" "password"

# create user "cdec.im.test1@gmail.com"
doPost "application/json" "{\"name\":\"cdec\",\"email\":\"cdec.im.test1@gmail.com\",\"password\":\"pwd123ABC\"}" "http://${HOST_URL}/api/user" "${TOKEN}"
fetchJsonParameter "id"
USER_ID=${OUTPUT}

authWithoutRealmAndServerDns "cdec" "pwd123ABC"

# create workspace
doPost "application/json" "{\"defaultEnv\":\"default\",\"commands\":[{\"commandLine\":\"mvn clean install -f $\{current.project.path}\",\"name\":\"build\",\"type\":\"mvn\",\"attributes\":{}}],\"projects\":[],\"name\":\"${WORKSPACE_NAME}\",\"environments\":{\"default\":{\"recipe\":{\"location\":\"codenvy/ubuntu_jdk8\",\"type\":\"dockerimage\"},\"machines\":{\"dev-machine\":{\"servers\":{},\"agents\":[\"org.eclipse.che.terminal\",\"org.eclipse.che.ws-agent\",\"org.eclipse.che.ssh\"],\"attributes\":{\"memoryLimitBytes\":1610612736},\"source\":{\"type\":\"dockerfile\",\"content\":\"FROM codenvy/ubuntu_jdk8\"}}}}},\"links\":[],\"description\":null}" "http://${HOST_URL}/api/workspace/?token=${TOKEN}"
fetchJsonParameter "id"
WORKSPACE_ID=${OUTPUT}

# run workspace
doPost "application/json" "{}" "http://${HOST_URL}/api/workspace/${WORKSPACE_ID}/runtime?token=${TOKEN}"

# verify is workspace running
doSleep "6m"  "Wait until workspace starts to avoid 'java.lang.NullPointerException' error on verifying workspace state"

# obtain network ports
doGet "http://${HOST_URL}/api/workspace/${WORKSPACE_ID}?token=${TOKEN}"
validateExpectedString ".*\"status\":\"RUNNING\".*"
fetchJsonParameter "network.ports"
NETWORK_PORTS=${OUTPUT}

EXT_HOST_PORT_REGEX="4401/tcp=\[PortBinding\{hostIp='127.0.0.1', hostPort='([0-9]*)'\}\]"
EXT_HOST_PORT=$([[ "$NETWORK_PORTS" =~ $EXT_HOST_PORT_REGEX ]] && echo ${BASH_REMATCH[1]})
URL_OF_PROJECT_API="http://${HOST_URL}:81/${EXT_HOST_PORT}_${HOST_URL}/wsagent/ext/project"

# obtain machine token
doGet "http://${HOST_URL}/api/machine/token/${WORKSPACE_ID}?token=${TOKEN}"
fetchJsonParameter "machineToken"
MACHINE_TOKEN=${OUTPUT}

# create project of type "console-java"
doPost "application/json" "{\"location\":\"https://github.com/che-samples/console-java-simple.git\",\"parameters\":{},\"type\":\"git\"}" "${URL_OF_PROJECT_API}/import/${PROJECT_NAME}?token=${MACHINE_TOKEN}"

doGet "http://${HOST_URL}/api/workspace/${WORKSPACE_ID}?token=${TOKEN}"
validateExpectedString ".*\"status\":\"RUNNING\".*"
validateExpectedString ".*\"path\":\"/${PROJECT_NAME}.*"

# create factory from template "minimal"
doPost "application/json" "{\"v\": \"4.0\",\"workspace\": {\"projects\": [{\"links\": [],\"name\": \"Spring\",\"attributes\": {\"languageVersion\": [\"1.6\"],\"language\": [\"java\"]},\"type\": \"maven\", \"source\": {\"location\": \"https://github.com/codenvy-templates/web-spring-java-simple.git\",\"type\": \"git\",\"parameters\": {\"keepVcs\": \"false\", \"branch\": \"3.1.0\"}},\"modules\": [],\"path\": \"/Spring\",\"mixins\": [\"git\"],\"problems\": []}], \"defaultEnv\": \"wss\",\"name\": \"wss\",\"environments\": [{\"machineConfigs\": [{\"dev\": true,\"limits\": {\"ram\":2048},\"source\": {\"location\": \"http://${HOST_URL}/api/recipe/recipe_ubuntu/script\",\"type\": \"recipe\"}, \"name\": \"dev-machine\",\"type\": \"docker\"}],\"name\": \"wss\"}],\"links\": []}}" "http://${HOST_URL}/api/factory?token=${TOKEN}"
fetchJsonParameter "id"
FACTORY_ID=${OUTPUT}

# make backup
executeIMCommand "backup"
fetchJsonParameter "file"
BACKUP_PATH=${OUTPUT}

# update to latest version
executeIMCommand "download" "codenvy" "${LATEST_CODENVY_VERSION}"
executeIMCommand "install" "codenvy" "${LATEST_CODENVY_VERSION}"
validateInstalledCodenvyVersion ${LATEST_CODENVY_VERSION}

# should be an error when try to restore from backup of another version
executeIMCommand "--valid-exit-code=1" "restore" ${BACKUP_PATH}
validateExpectedString ".*\"Version.of.backed.up.artifact.'${PREV_CODENVY_VERSION}'.doesn't.equal.to.restoring.version.'${LATEST_CODENVY_VERSION}'\".*\"status\".\:.\"ERROR\".*"

# check if data was migrated correctly
authWithoutRealmAndServerDns "admin" "password"

doGet "http://${HOST_URL}/api/user/${USER_ID}?token=${TOKEN}"
validateExpectedString ".*cdec.im.test1@gmail.com.*"

authWithoutRealmAndServerDns "cdec.im.test1@gmail.com" "pwd123ABC"

doGet "http://${HOST_URL}/api/workspace/${WORKSPACE_ID}?token=${TOKEN}"
validateExpectedString ".*${PROJECT_NAME}.*${WORKSPACE_NAME}.*"

# verify that there is project on file system
executeSshCommand "sudo ls -R /home/codenvy/codenvy-data/fs"
validateExpectedString ".*/home/codenvy/codenvy-data/fs/[0-9a-z/]*/${WORKSPACE_ID}/${PROJECT_NAME}/src/main/java/org/eclipse/che/examples\:.*HelloWorld.java.*"

doGet "http://${HOST_URL}/api/factory/${FACTORY_ID}?token=${TOKEN}"
validateExpectedString ".*\"name\"\:\"wss\".*"

printAndLog "RESULT: PASSED"
vagrantDestroy
