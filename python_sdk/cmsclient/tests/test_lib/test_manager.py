__author__ = 'jemartin'

from cmsclient.tests import CMSTest
import os
from cmsclient.client import Client

from mock import patch, Mock

class managerTests(CMSTest):
    """ tests Manager"""
    __test__ = True

    def test_get(self):

        m = Mock(return_value=self.mocked_get('NetworkBubble',
                                              '51981f62e4b04449cb9a7850'))
        manager = self.cms.managers['NetworkBubble']
        self.assertIsNotNone(manager)

        self.cms._get = m
        result = manager.get('51981f62e4b04449cb9a7850')
        self.assertIsNotNone(result)
        self.assertIsInstance(result, self.cms.classes['NetworkBubble'])

    def test_list(self):

        m = Mock(return_value=self.get_payload('list-10.json'))
        manager = self.cms.managers['NetworkBubble']
        self.assertIsNotNone(manager)

        #TODO need to mock
        self.cms._post = m

        #list all network bubbles
        hasmore, result = manager.list('NetworkBubble')
        self.assertIsNotNone(result)
        x = list(result)
        self.assertEqual(len(x), 10)
        self.assertTrue(hasmore)
        self.assertIsInstance(x[0], self.cms.classes['NetworkBubble'])

    def test_add_inner(self):
        manager = self.cms.managers['Agent']
        self.assertIsNotNone(manager)

        m = Mock(return_value='{"result":["51981f62e4b04449cb9a7852"]}')
        self.cms._post = m

        ob_info = {"resourceId": "SC4b5"}
        host_info = {"_oid" : "51981f62e4b04449cb9a7851",
                    "resourceId": "SC4b5"}
        ob = manager.client.classes['Agent'](ob_info)
        host = manager.client.classes['NodeServer'](host_info)
        r = manager.add_inner(ob,host,'agent')
        self.assertEqual(r, '51981f62e4b04449cb9a7852')

    def test_create(self):
        manager = self.cms.managers['NetworkBubble']
        self.assertIsNotNone(manager)

        m = Mock(return_value='{"result":["51981f62e4b04449cb9a7851"]}')
        self.cms._post = m

        info = {"resourceId": "SC4b5"}
        ob = manager.client.classes['NetworkBubble'](info)
        r = manager.create(ob)
        self.assertEqual(r, '51981f62e4b04449cb9a7851')

    def test_delete(self):
        m = Mock(return_value=self.mocked_get('NetworkBubble',
                                              '51981f62e4b04449cb9a7850'))
        d = Mock(return_value='{"status":{"code":"200","msg":"ok","stackTrace":null},"dbTimeCost":1,"totalTimeCost":3}')
        manager = self.cms.managers['NetworkBubble']
        self.assertIsNotNone(manager)

        self.cms._delete = d
        r = manager.delete('51981f62e4b04449cb9a7850')
        self.assertEqual(r, 'ok')

    def test_delattr(self):
        m = Mock(return_value=self.mocked_get('NetworkBubble',
                                              '51981f62e4b04449cb9a7850'))
        d = Mock(return_value='{"status":{"code":"200","msg":"ok","stackTrace":null},"dbTimeCost":1,"totalTimeCost":3}')
        manager = self.cms.managers['NetworkBubble']
        self.assertIsNotNone(manager)

        self.cms._get = m
        result = manager.get('51981f62e4b04449cb9a7850')
        self.cms._delete = d
        r = manager.delattr(result,"networkDevices")
        self.assertEqual(r, 'ok')

    def test_update(self):
        m = Mock(return_value=self.mocked_get('NetworkBubble',
                                              '51981f62e4b04449cb9a7850'))
        u = Mock(return_value='{"status":"OK"}')
        manager = self.cms.managers['NetworkBubble']
        self.assertIsNotNone(manager)

        self.cms._get = m
        ob = manager.get('51981f62e4b04449cb9a7850')
        ob.resourceId = 'SLC4b5'
        self.cms._post = u
        r = manager.update(ob)
        self.assertEqual(r, 'OK')