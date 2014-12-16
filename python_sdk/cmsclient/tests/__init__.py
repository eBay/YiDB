__author__ = 'jemartin'

import unittest2
import os
from cmsclient.client import Client

class CMSTest(unittest2.TestCase):
    __test__ = False

    def setUp(self):
        super(CMSTest, self).setUp()
        filename = os.path.join(os.path.dirname(__file__),
                                'resources/metadata.json')
        self.cms = Client('phx5qa01c-5fca.stratus.phx.qa.ebay.com:8080', 'cmsdb',
                          filename=filename)

    def tearDown(self):
        super(CMSTest, self).tearDown()

    def mocked_get(self, klass, id):
        name = 'get-%s!%s.json' % (klass, id)
        return self.get_payload(name)

    def mocked_delete(self, klass, id):
        return "OK"

    def get_payload(self, name):
        filename = os.path.join(os.path.dirname(__file__),
                                'resources/%s' % name)
        with open(filename) as f:
            msg = f.read()
        return msg

