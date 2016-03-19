#!/bin/bash
dirname=$(dirname $0)
CHANGELOG_MD=${dirname}/CHANGELOG.md
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
  local OLD_VERSION=$1
  local VERSION=$2
  local FORCE=$3

  local CHANGELOG_MD_NEW=${CHANGELOG_MD}.template

  echo "### ${VERSION} ($(env LC_ALL=C date +%F)) ###" > ${CHANGELOG_MD_NEW}
  echo " *" >> ${CHANGELOG_MD_NEW}
  echo  >> ${CHANGELOG_MD_NEW}
  cat ${CHANGELOG_MD} >> ${CHANGELOG_MD_NEW}

  mv -f ${CHANGELOG_MD_NEW} ${CHANGELOG_MD}
  TEMPFILE=`mktemp gitlog-XXXXX`
  git log v$OLD_VERSION.. > $TEMPFILE
  vim -c "new|e ${CHANGELOG_MD}|2" $TEMPFILE
  rm -f $TEMPFILE

  git diff ${CHANGELOG_MD} ${VERSION_PROPERTIES}

  if [ $FORCE -eq 0 ]; then
    echo -n Do you want to commit? [Y/n]
    read R
    if [ x$R != x'Y' ];then
	git checkout ${CHANGELOG_MD} ${VERSION_PROPERTIES}
	echo "aborted!" >&2
	return
    fi
  fi

  git add ${CHANGELOG_MD} ${VERSION_PROPERTIES}
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

source $VERSION_PROPERTIES
OLD_VERSION=$version
apply_version $VERSION
commit $OLD_VERSION $VERSION $FORCE
