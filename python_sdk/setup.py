#!/usr/bin/env python
# encoding: utf-8
"""
setup.py
"""

from setuptools import setup, find_packages
import textwrap

version = '0.0.1'
setup(name='python-cmsclient',
    version=version,
    description='CMS Client',
    author='Openstratus',
    author_email='dl-ebay-openstratus-dev@ebay.com',
    url='http://github.scm.corp.ebay.com/OpenStratus',
    packages=find_packages(exclude=['bin']),
    include_package_data=True,
    classifiers=[
        'Development Status :: 3 - Alpha',
        'Topic :: System :: Systems Administration',
        'License :: Other/Proprietary License',
        'Operating System :: POSIX :: Linux',
        'Programming Language :: Python :: 2.7',
        'Environment :: No Input/Output (Daemon)',
        'Environment :: OpenStack',
    ],
)
