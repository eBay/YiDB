__author__ = 'jemartin'
import os
import sys
from cmsclient.cli.cms import Shell as cms_shell
from cmsclient.tests import CMSTest
from cmsclient import client as cms_client
from mock import patch
import cStringIO


class MetaTests(CMSTest):
    """test environment class"""
    __test__ = True

    def setUp(self):
        super(MetaTests, self).setUp()
        self.filename = os.path.join(os.path.dirname(__file__),
                                     '../resources/metadata.json')


    # from quantum client
    def shell(self, argstr):
        orig = sys.stdout
        clean_env = {}
        _old_env, os.environ = os.environ, clean_env.copy()
        try:
            sys.stdout = cStringIO.StringIO()
            _shell = cms_shell()
            _shell.run(argstr.split())
        except SystemExit:
            exc_type, exc_value, exc_traceback = sys.exc_info()
            self.assertEqual(exc_value.code, 0)
        finally:
            out = sys.stdout.getvalue()
            sys.stdout.close()
            sys.stdout = orig
            os.environ = _old_env
        return out

    def test_help(self):

        help_text = self.shell('--metadata %s help' % self.filename)
        self.assertEqual('usage:', help_text[:6])

    def test_rel(self):

        output = self.shell('--metadata %s meta get-rel Subnet --filter all' % self.filename)
        print output
