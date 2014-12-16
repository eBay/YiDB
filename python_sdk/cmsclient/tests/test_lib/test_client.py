__author__ = 'jemartin'
import unittest2
import os
import json
from cmsclient.client import Client
from mock import patch, Mock


class clientTests(unittest2.TestCase):
    """test environment class"""

    def setUp(self):
        super(clientTests, self).setUp()

    def test_constructor(self):
        filename = os.path.join(os.path.dirname(__file__),
                                '../resources/metadata.json')
        cl = Client('localhost', 'cmsdb', filename=filename)
        self.assertIsNotNone(cl)

        # check that all the classes are parsed
        self.assertEqual(76, len(cl.classes))
        self.assertEqual(76, len(cl.metadata))

        # check that Base class is parsed
        self.assertIn('Base', cl.classes)
        self.assertIn('Base', cl.metadata)

        # check that Resource class is parsed
        self.assertIn('Resource', cl.classes)
        self.assertIn('Resource', cl.metadata)

        # check that AccessPoint class is parsed
        self.assertIn('AccessPoint', cl.classes)
        self.assertIn('AccessPoint', cl.metadata)

        #check the hierarchy of classes
        self.assertTrue(issubclass(cl.classes['Resource'], cl.classes['Base']))
        self.assertTrue(issubclass(cl.classes['AccessPoint'],
                                   cl.classes['Resource']))

        rs = cl.classes['Resource']
        self.assertTrue(hasattr(rs, 'resourceId'))

        ap = cl.classes['AccessPoint']
        self.assertTrue(hasattr(ap, 'description'))
        self.assertTrue(hasattr(ap, 'resourceId'))
        self.assertTrue(hasattr(ap, 'vip'))

    def test_find(self):
        filename = os.path.join(os.path.dirname(__file__),
                                '../resources/metadata.json')
        cl = Client('localhost', 'cmsdb', filename=filename)
        m = Mock(return_value='{"count" : 1, "result":[{"_type": "FQDN",'
                              '"_oid": "51981f62e4b04449cb9a7852"}]}')
        cl._post = m
        q = 'FQDN[@resourceId="host"]'
        r = cl.find(q)
        self.assertEqual(list(r[1])[0]._type, 'FQDN')

    def test_validation(self):

        filename = os.path.join(os.path.dirname(__file__),
                                '../resources/metadata.json')
        cl = Client('localhost', 'cmsdb', filename=filename)

        ob = cl.classes['AccessPoint']({})
        self.assertEqual(ob._type, 'AccessPoint')
        info = {'_oid': '12345',"_type": "AccessPoint"}
        ob = cl.classes['AccessPoint'](info)
        self.assertEqual(ob._type, 'AccessPoint')
        ob = cl._object_factory(info)
        self.assertEqual(ob._type, 'AccessPoint')

        # known attribute should be successful
        ob.protocol = 'ANY'
        self.assertEqual(ob.protocol, 'ANY')

        with self.assertRaises(AttributeError):
            # Enumeration with wrong value
            ob.protocol = 'mine'

        # Inherited attribute should be successful
        ob.label = 'label'
        self.assertEqual(ob.label, 'label')

        with self.assertRaises(AttributeError):
            #unknown attribute should raise exception
            ob.test = 'test'

        nc = cl.classes['NetworkController'](info)

        nc.ifIndex = long(1)
        # default is false
        self.assertFalse(nc.isAllocated)

        nc.isAllocated = True

    def test_assign(self):
        filename = os.path.join(os.path.dirname(__file__),
                                '../resources/metadata.json')
        cl = Client('localhost', 'cmsdb', filename=filename)

        nc = cl.classes['NetworkController']({"_oid" : "12345","label":"l"})

        a = cl.classes['AssetServer']()
        ncs = [nc]
        a.networkControllers = ncs
        nc1 = a.networkControllers
        self.assertEqual(nc1[0]['label'], "l")

        ns = cl.classes['NodeServer']
        ns._oid = "12345"
        a.nodeServer = ns
        ns1 = a.nodeServer
        self.assertEqual(ns1['_type'], "NodeServer")



