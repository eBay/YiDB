__author__ = 'jemartin'

from cmsclient.tests import CMSTest

from mock import patch, Mock

class queryTests(CMSTest):
    """ tests query"""
    __test__ = True

    def test_query(self):

        m = Mock(return_value=self.get_payload('query-one.json'))

        self.cms._post = m
        hasmore, result =\
            self.cms.find(query='Asset[@_oid="519c5712e4b0b07c83311e5b"]')
        self.assertIsNotNone(result)
        x = list(result)
        self.assertEqual(len(x), 1)
        self.assertEqual(x[0]._oid, '519c5712e4b0b07c83311e5b')

