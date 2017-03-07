#!/bin/bash

INSTALL_DIR=$HOME/.local/stf-connect
JAR_LOCATION=${INSTALL_DIR}/stfc.jar
INSTALL_LOCATION=${INSTALL_DIR}/stf

if [ $# -eq 0 ]; then
    ARTIFACT_LOCATION=build/libs/STFC-all-1.0.jar
else
    ARTIFACT_LOCATION=$1
fi

echo "artifact location is $ARTIFACT_LOCATION"

mkdir -p ${INSTALL_DIR}

yes | cp -rf ${ARTIFACT_LOCATION} ${JAR_LOCATION}

echo "#!/bin/bash" > ${INSTALL_LOCATION}
echo "java -jar ${JAR_LOCATION} \"\$@\"" >> ${INSTALL_LOCATION}

chmod +x ${INSTALL_LOCATION}
ln -fs ${INSTALL_LOCATION} ~/.local/bin/stf

