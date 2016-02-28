#!/bin/bash
dirname=$(dirname $0)
META_INF=${dirname}/src/main/resources/META-INF/
PLUGIN_XML=${META_INF}/plugin.xml
VERSION_PROPERTIES=${dirname}/version.properties

apply_version() {
  local VERSION=$1
  echo "version='${VERSION}'" > $VERSION_PROPERTIES
}

usage() {
  echo $0 [-f] version
  exit 1
}

commit() {
  local VERSION=$1
  local FORCE=$2
  local LINE=$(grep -n -h \<change-notes\> ${PLUGIN_XML} | cut -f1 -d:)

  vim +$((${LINE}+2)) ${PLUGIN_XML}

  git diff ${PLUGIN_XML} ${VERSION_PROPERTIES}

  if [ $FORCE -eq 0 ]; then
    echo -n Do you want to commit? [Y/n]
    read R
    if [ x$R != x'Y' ];then
	echo "aborted!" >&2
	return
    fi
  fi

  git add ${PLUGIN_XML} ${VERSION_PROPERTIES}
  git commit -s -m "version $VERSION release"
  git tag -f v${VERSION}
}

FORCE=0
while getopts ":f" opt; do
  case $opt in
    f)
      FORCE=1
      ;;
    \?)
      usage
      ;;
  esac
done

shift $((OPTIND-1))

if [ $# -eq 0 ];then
  usage
fi

VERSION=$@

apply_version $VERSION
commit $VERSION $FORCE
