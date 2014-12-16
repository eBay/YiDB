_author__ = 'jemartin'

import requests
import json

from cmsclient import exceptions
from urlparse import urlparse


class Client(object):
    """
        CMS Client Class
    """

    def __init__(self, service, repo, token=None, branch='main',
                 filename=None):

        if not service:
            raise exceptions.InvalidParameters('Missing Service')
        self.service = service

        if not token or token == '':
            self.token = None
        else:
            self.token = token

        self.branch = branch

        if not repo or repo == '':
            raise exceptions.InvalidParameters('Missing Repository')
        self.repo = repo

        self.base = 'http://%s/cms/repositories/%s/branches/%s' % (service,
                                                                   repo,
                                                                   branch)

        if not filename:
            uri = 'http://%s/cms/repositories/%s/metadata' % (service,
                                                              self.repo)
        else:
            uri = 'file:%s' % filename

        # This will work as long as the number of classes is not too big
        self.classes = {}
        self.managers = {}
        self.inbound = {}
        self.metadata = self._load_metadata(uri)

        self._generate()

    def _load_metadata(self, uri):

        parsed_uri = urlparse(uri)
        if not parsed_uri or parsed_uri.scheme not in ['http', 'file']:
            raise exceptions.InvalidParameters('Invalid Metadata URI')

        m = None
        if parsed_uri.scheme == 'file':
            with open(parsed_uri.path) as f:
                m = json.load(f)
        else:
            result = self._get(uri, {}, {'Accept': 'application/json'})
            m = json.loads(result)

        if not m:
            return None
        status = m.get('status', {})
        if status.get('code', '') == '200':
            return {k['name']: k for k in m.get('result', [])}
        else:
            return None

    def _default(self, m):
        defaultvalue = m.get('defaultValue')
        if defaultvalue is not None:
            if m['dataType'] == 'boolean':
                if defaultvalue == 'false':
                    return False
                if defaultvalue == 'true':
                    return True
        return defaultvalue

    def _class_factory(self, name):
        if name in self.classes:
            c = self.classes[name]
            return self.classes[name]
        klass = self.metadata[name]
        attr = {'description': klass.get('description', '')}

        # Recursively get the parent classes
        parent = klass.get('parent')
        if parent:
            parent_class = self._class_factory(parent)
        else:
            parent_class = CMSObject
        field_md = {}
        fields = klass.get('fields', {})
        for f in fields:
            field_md[f] = {k: v for k, v in fields[f].items() if k not in [
                'dbName',
                'valueDbName'
            ]}
            attr[f] = self._default(field_md[f])
            if field_md[f].get('dataType') == 'relationship':
                to_class = field_md[f].get('refDataType')
                inbound = self.inbound.get(to_class, [])
                if name not in inbound:
                    inbound.append(name)
                self.inbound[to_class] = inbound

        # was hoping to be able to check the parent metadata from instance
        # but could not find how to call super without causing infinite loop
        # therefore - copy the parent's metadata to enable checks in subs.
        field_md.update(parent_class.metadata)

        attr['metadata'] = field_md
        attr['_type'] = name

        k = type(str(name), (parent_class,), attr)
        self.classes[name] = k
        return k

    def _generate(self):
        for n in self.metadata:
            self._class_factory(n)
            self.managers[n] = Manager(self, n)

    def _object_factory(self, info):
        if not info:
            return None

        k = self.classes[info['_type']](info)

        return k

    def _post(self, url, params, headers, data):
        result = requests.post(url=url, params=params,
                               headers=headers, data=data)
        if result.status_code == 200:
            return result.content
        else:
            raise exceptions.CMSError(
                'CMS returned %s: %s' % (result.status_code, result.content)
            )

    def _delete(self, url, params, headers):
        result = requests.delete(url=url, params=params,
                               headers=headers)
        if result.status_code == 200:
            return result.content
        elif result.status_code == 1004:
            raise exceptions.ObjectNotFound(
                'CMS returned %s: %s' % (result.status_code, result.content)
            )
        else:
            raise exceptions.CMSError(
                'CMS returned %s: %s' % (result.status_code, result.content)
            )

    def _get(self, url, params, headers):
        result = requests.get(url=url, params=params,
                              headers=headers)
        if result.status_code == 200:
            return result.content
        else:
            raise exceptions.CMSError(
                'CMS returned %s: %s' % (result.status_code, result.content)
            )

    def find(self, query, limit=100, offset=0):

        url = '%s/query' % self.base

        headers = {'Accept': 'application/json'}
        if self.token:
            headers['Authorization'] = self.token

        params = {'limit': limit,
                  'skip': offset,
                  'allowFullTableScan': True}
        if not query:
            raise exceptions.InvalidParameters('query cannot be null')

        result = self._post(url, params, headers, query)
        if result:
            r = json.loads(result)
            hasmore = r.get('hasmore', False)
            if r.get('count') == 0:
                return None
            return (hasmore, (self._object_factory(o) for o in
                              r.get( 'result', [])))

        return None

    def create(self, classname, payload, parent_obj=None, parent_ref=None):

        url = '%s/%s' % (self.base, classname)

        headers = {'Accept': 'application/json'}
        if self.token:
            headers['Authorization'] = self.token

        params = {'allowFullTableScan': True}
        if not classname:
            raise exceptions.InvalidParameters('classname cannot be null')
        if not payload:
            raise exceptions.InvalidParameters('payload cannot be null')

        if parent_obj:
            path = parent_obj._type + "!" + parent_obj._oid + "!" + parent_ref
            url = url + "?path=" + path

        result = self._post(url, params, headers, payload)
        if result:
            oid = json.loads(result).get('result')[0]
            return oid

    def update(self, classname, oid, payload):

        url = '%s/%s/%s' % (self.base, classname,oid)

        headers = {'Accept': 'application/json'}
        if self.token:
            headers['Authorization'] = self.token

        params = {'allowFullTableScan': True}
        if not classname:
            raise exceptions.InvalidParameters('classname cannot be null')

        if not oid:
            raise exceptions.InvalidParameters('oid cannot be null')

        if not payload:
            raise exceptions.InvalidParameters('payload cannot be null')

        result = self._post(url, params, headers, payload)
        if result:
            r = json.loads(result)
            return r.get("status")
        else:
            raise AttributeError("Error in update:" + str(result))

    def delattr(self, classname, oid, attribute):

        url = '%s/%s/%s/%s' % (self.base, classname, oid, attribute)

        headers = {'Accept': 'application/json'}
        if self.token:
            headers['Authorization'] = self.token

        params = {'allowFullTableScan': True}
        if not classname:
            raise exceptions.InvalidParameters('classname cannot be null')

        if not oid:
            raise exceptions.InvalidParameters('oid cannot be null')

        if not attribute:
            raise exceptions.InvalidParameters('attribute cannot be null')

        result = self._delete(url, params, headers)
        if result:
            r = json.loads(result)
            return r.get("status").get("msg")
        else:
            raise AttributeError("Error in delete:" + str(result))

    def delete(self, classname, oid):

        url = '%s/%s/%s' % (self.base, classname,oid)

        headers = {'Accept': 'application/json'}
        if self.token:
            headers['Authorization'] = self.token

        params = {'allowFullTableScan': True}
        if not classname:
            raise exceptions.InvalidParameters('classname cannot be null')

        if not oid:
            raise exceptions.InvalidParameters('oid cannot be null')

        result = self._delete(url, params, headers)
        if result:
            r = json.loads(result)
            return r.get("status").get("msg")
        else:
            raise AttributeError("Error in delete:" + str(result))

class Manager(object):
    """
        CMS Class Manager
    """

    def __init__(self, client, name):
        self.classname = name
        self.client = client

    def get(self, id):
        url = '%s/%s/%s' % (self.client.base, self.classname, id)

        headers = {'Accept': 'application/json'}
        if self.client.token:
            headers['Authorization'] = self.client.token

        params = {}

        result = self.client._get(url, params, headers)
        if result:
            r = json.loads(result)
            return self.client._object_factory(r.get('result')[0])

        return None

    def list(self, query=None, limit=100, offset=0):

        if query is None:
            raise AttributeError('query cannot be empty')

        return self.client.find(query, limit=limit, offset=offset)

    def add_inner(self, cmsobj, parent_obj, parent_ref):
        if (cmsobj is None):
            raise AttributeError('object is None when addrel CMSObject')

        if self.client.metadata[self.classname]['embed']:
            raise AttributeError('Embed object %s cannot be add alone'
                                 % (cmsobj._type))

        if parent_obj is None:
            raise AttributeError('Please provide parent object for'
                                 ' inner relationship')
        if parent_ref is None:
            raise AttributeError('Please provide relationship name for'
                                 ' inner relationship')
        if self.client.metadata[self.classname]['inner']:
            return self.client.create(cmsobj._type,json.dumps(
                cmsobj.get_json_payload_dict()),parent_obj, parent_ref)
        else:
            cmsobj._oid = self.client.create(cmsobj._type,json.dumps(
                cmsobj.get_json_payload_dict()))
            d = {}
            d['_oid'] = cmsobj._oid
            d['_type'] = cmsobj._type
            self.client.update(cmsobj._type,cmsobj._oid,
                                  json.dumps(d))
            return cmsobj._oid

    def create(self,cmsobj):
        if (cmsobj is None):
            raise AttributeError('object is None when create CMSObject')

        if self.client.metadata[self.classname]['embed']:
            raise AttributeError('Embed object %s cannot be created alone'
                                 % (cmsobj._type))
        if self.client.metadata[self.classname]['inner']:
            raise AttributeError('Inner object %s needs to create in addrel'
                                 % (cmsobj._type))

        return self.client.create(cmsobj._type,json.dumps(
            cmsobj.get_json_payload_dict()))

    def update(self,cmsobj):
        if (cmsobj is None):
            raise AttributeError('object is None when update CMSObject')
        if (cmsobj._oid is None):
            raise AttributeError('oid is None when update CMSObject')

        return self.client.update(cmsobj._type,cmsobj._oid,
                                  json.dumps(cmsobj.get_json_payload_dict()))

    def delattr(self, oid, attribute):
        return self.client.delattr(self.classname, oid, attribute)

    def delete(self,oid):
        return self.client.delete(self.classname,oid)

def _validate(set):
    def set_attr(self, name, value):
        m = self.metadata.get(name)

        if m:

            if m.get('expression') is not None:
                AttributeError('%s is an expression' % name)

            # assuming that there is no list of enumeration ....
            if m['dataType'] == 'enumeration':
                if value not in m['enumValues']:
                    raise AttributeError('Invalid Value %s for %s' % (value,
                                                                      name))

            if m['cardinality'] == 'Many':
                if type(value) is not list:
                    raise AttributeError('Value %s for %s has to be a list type'
                                         % (value, name))
                else:
                    l = []
                    for v in value:
                        l.append(_check_value(v, name, m))
                    value = l
            else:
                value = _check_value(value, name, m)

            set(self, name, value)
        elif name == '_oid':
            set(self, name, value)
        else:
            raise AttributeError('Unknown Attribute %s' % name)

    return set_attr

def _check_value(v, name, m):
    if v is None and m['mandatory'] == True:
        raise AttributeError('Attribute %s is mandatory' % (name))
    t = m['dataType']
    if t == 'string':
        if type(v) is not str and type(v) is not unicode:
            raise AttributeError('Invalid Value %s for %s' % (v, name))
    if t == 'integer':
        if type(v) is not int:
            raise AttributeError('Invalid Value %s for %s' % (v, name))

    if t == 'relationship':
        r = m['refDataType']
        rt = m['relationType']
        if type(v) is dict:
            if v['_type'] != r:
                raise AttributeError('Invalid type %s for %s' % (v, r))
        else:
            # assume v is a CMSObject here
            if v._type != r:
                raise AttributeError('Invalid obj ref %s for %s' % (v, r))
            if rt == 'Embedded':
                return v.get_json_payload_dict()
            else:
                ref = {}
                ref['_oid'] = v._oid
                ref['_type'] = v._type
                return ref

    if t == 'boolean':
        if type(v) is not bool:
            raise AttributeError('Invalid Value %s for %s' % (v, name))

    if t == 'long':
        if type(v) is not long:
            raise AttributeError('Invalid Value %s for %s' % (v, name))
    return v

class CMSObject(object):
    """
        Base CMS class used to generate all CMS objects
    """
    metadata = {}

    def __init__(self, info = {}):
        for a in self.metadata:
            if a in info:
                v = info[a]
                setattr(self, a, info[a])
        if '_oid' in info:
            setattr(self, '_oid', info['_oid'])

    def get_json_payload_dict(self):
        payload_dict = {}
        for a in self.metadata:
            if hasattr(self,a):
                v = getattr(self,a)
                if v:
                    payload_dict[a] = v
        if (hasattr(self,'_oid')):
            payload_dict['_oid'] = getattr(self,'_oid')
        if (hasattr(self,'_type')):
            payload_dict['_type'] = getattr(self,'_type')
        return payload_dict

    # this is inspired from http://code.activestate.com/recipes/252158/
    __setattr__ = _validate(object.__setattr__)

    class __metaclass__(type):
        __setattr__ = _validate(type.__setattr__)
