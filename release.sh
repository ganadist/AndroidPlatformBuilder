#!/bin/bash
dirname=$(dirname $0)
META_INF=${dirname}/resources/META-INF/
PLUGIN_XML=${META_INF}/plugin.xml

apply_version_plugin_xml() {
  local VERSION=$1
  sed 's/<version>\([0-9.].*\)<\/version>/<version>'$VERSION'<\/version>/' < ${PLUGIN_XML} > ${PLUGIN_XML}.new
  mv -f ${PLUGIN_XML}.new ${PLUGIN_XML}
}

usage() {
  echo $0 [-f] version
  exit 1
}

commit() {
  local VERSION=$1
  local FORCE=$2

  git diff ${PLUGIN_XML}

  if [ $FORCE -eq 0 ]; then
    echo Do you want to commit?[Y/n]
    read R
    if [ $R != 'Y' ];then
	echo "aborted!" >&2
	return
    fi
  fi

  git add ${PLUGIN_XML}
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

apply_version_plugin_xml $VERSION
commit $VERSION $FORCE
