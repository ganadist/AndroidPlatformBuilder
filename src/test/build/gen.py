#!/usr/bin/env python2
import os
import shutil

DIR='build'
BUILD_ID_MK = '/'.join((DIR, 'core', 'build_id.mk'))
VERSION_MK = '/'.join((DIR, 'core', 'version_defaults.mk'))

def checkout(tag):
    os.system('(cd %s; git checkout %s)'%(DIR, tag))

def get_level():
    for line in open(VERSION_MK):
        line = line.strip()
        if not line.startswith('PLATFORM_SDK_VERSION'):
            continue
        version = line.split('=')[-1].strip()
        return version
    return '0'

def copy_build_files(level):
    dest = '/'.join(('api-' + level, 'build', 'core'))
    try: os.makedirs(dest)
    except: pass
    for f in (BUILD_ID_MK, VERSION_MK):
        shutil.copy(f, dest)

for line in open('/'.join((DIR, 'tag'))):
    tag = line.strip()
    checkout(tag)
    level = get_level()
    print(level)
    copy_build_files(level)

