__author__ = 'jemartin'

from cliff.show import ShowOne
from cliff.lister import Lister
import json
import traceback

def _obj_table(obj):

    columns = []
    data = []
    for k in obj.metadata:
        columns.append(k)
        m = obj.metadata[k]
        v = getattr(obj,k)
        if m['dataType'] == 'relationship' and m['cardinality'] == 'Many':
            if not v:
                v=[]
            data.append('--> %d %s' % (len(v), m['refDataType']))
        else:
            data.append(v)
    return columns, data


def _rel_table(obj, attr=None):

    columns = ('Attribute', 'Type', 'OID')
    data = []
    m = obj.metadata
    for k in [ x for x in m if m[x]['dataType'] == 'relationship']:
        if attr is None or attr == k:
            v = getattr(obj, k)
            if not v:
                v = []
            if isinstance(v,dict):
                data.append((k, v['_type'], v['_oid']))
            else:
                for item in v:
                    data.append((k, item['_type'], item['_oid']))
    return columns, data


def _list_table(items, fields=None):

    if not fields:
        fields = ('_oid', 'resourceId', 'description')
    columns = fields
    data = []
#    m = items[0].metadata
#    for f in fields:
#        if f not in m:
#            raise exceptions.UnknownField('%s' % f)

    data = ([getattr(i,f) for f in fields] for i in items)
    return columns, data


class GetCommand(ShowOne):

    def get_parser(self, prog_name):
        parser = super(GetCommand, self).get_parser(prog_name)

        parser.add_argument('object_class',
                            help="Object class")
        parser.add_argument('--query',
                            help="")
        return parser

    def take_action(self, parsed_args):
        client = self.app.cms_client
        cm = client.manager

        params = parsed_args.query.split('=')
        query = '%s[@%s="%s"]' % (parsed_args.object_class, params[0],
                                  params[1])
        #result = cm.get(parsed_args.oid)

        hasmore, result = client.find(query,
                                      limit=1,
                                      offset=0)
        if result and not hasmore:
            return _obj_table(list(result)[0])
        else:
            if hasmore:
                raise RuntimeError('Invalid request: returned too many objects'
                                   ' - use find to get list')
            else:
                raise RuntimeError('No results')


class GetRelationsCommand(Lister):

    def get_parser(self, prog_name):
        parser = super(GetRelationsCommand, self).get_parser(prog_name)

        parser.add_argument('classname',
                            help="Object class")

        parser.add_argument('oid',
                            help="OID")

        parser.add_argument('--attribute',
                            help="relationship attribute")
        return parser

    def take_action(self, parsed_args):
        client = self.app.cms_client
        cm = client.manager

        result = cm.get(parsed_args.classname,
                        parsed_args.oid)

        attr = parsed_args.attribute

        if attr and attr not in result.metadata:
            raise AttributeError('attribute %s is not part of %s' %
                                   (parsed_args.attribute,
                                   parsed_args.classname))
        if result:
            return _rel_table(result, attr=attr)
        else:
            raise RuntimeError('No result')


class QueryCommand(Lister):

    def get_parser(self, prog_name):
        parser = super(QueryCommand, self).get_parser(prog_name)

        parser.add_argument('query',
                            help="query")

        parser.add_argument('--limit',
                            default=100,
                            help="Maximum returned result")

        parser.add_argument('--offset',
                            default=0,
                            help="Offset")

        parser.add_argument('--fields',
                            nargs='*',
                            help="fields to display")
        return parser

    def take_action(self, parsed_args):
        client = self.app.cms_client

        hasmore, result = client.find(parsed_args.query,
                                      limit=parsed_args.limit,
                                      offset=parsed_args.offset)

        if hasmore:
            print 'More items available'

        if result:
            return _list_table(result, parsed_args.fields)
        else:
            raise RuntimeError('No result')

class AddRelCommand(ShowOne):

    def get_parser(self, prog_name):
        parser = super(CreateCommand, self).get_parser(prog_name)

        parser.add_argument('parentpath',
                            default=None,
                            help="The parent path for this object")
        parser.add_argument('classname',
                            help="Object class: ex: Region")
        parser.add_argument('payload',
                            help="the new object payload: eg: {\"resourceId\" :"
                                 " \"Region4\",  \"description\" : "
                                 "\"Region SLC change 2\"}")
        return parser

    def take_action(self, parsed_args):
        try:
            client = self.app.cms_client
            manager = client.manager

            info = json.loads(parsed_args.payload)
            ob = client.classes[parsed_args.classname](info)
            host_obj = None
            host_rel = None
            if parsed_args.path is not None:
                [host_class,host_oid,host_rel] = parsed_args.parentpath.split('!')
                host_obj = manager.get(host_class,host_oid)
                if (host_obj is None):
                    raise RuntimeError('Parent object not found')
            oid = manager.addrel(ob, host_obj, host_rel)

            if oid:
                columns = ['_oid']
                data = [oid]
                return (columns, data)
            else:
                raise RuntimeError('Add Child fail')
        except:
            tb = traceback.format_exc()
            print tb
            raise

class CreateCommand(ShowOne):

    def get_parser(self, prog_name):
        parser = super(CreateCommand, self).get_parser(prog_name)

        parser.add_argument('classname',
                            help="Object class: ex: Region")

        parser.add_argument('payload',
                            help="the new object payload: eg: "
                                 "{\"resourceId\" : \"Region4\",  "
                                 "\"description\" : \"Region SLC change 2\"}")

        return parser

    def take_action(self, parsed_args):
        client = self.app.cms_client
        manager = client.manager

        info = json.loads(parsed_args.payload)
        ob = client.classes[parsed_args.classname](info)
        oid = manager.create(ob)

        if oid:
            columns = ['_oid']
            data = [oid]
            return (columns, data)
        else:
            raise RuntimeError('Object creation fail')

class UpdateCommand(ShowOne):

    def get_parser(self, prog_name):
        parser = super(UpdateCommand, self).get_parser(prog_name)

        parser.add_argument('classname',
                            help="Object class: eg: Region")

        parser.add_argument('oid',
                            help="Object id in CMS, "
                                 "eg: 52d436c9e4b0ee66348b4518")

        parser.add_argument('payload',
                            help="the new object payload: eg: {\"resourceId\" "
                                 ": \"Region4\",  \"description\" "
                                 ": \"Region SLC change 2\"}")

        return parser

    def take_action(self, parsed_args):
        client = self.app.cms_client
        manager = client.manager

        info = json.loads(parsed_args.payload)
        ob = client.classes[parsed_args.classname](info)
        result = manager.update(ob)

        if result:
            return (['Status'],['OK'])
        else:
            raise RuntimeError('No result')

class DeleteCommand(ShowOne):

    def get_parser(self, prog_name):
        parser = super(DeleteCommand, self).get_parser(prog_name)

        parser.add_argument('classname',
                            help="Object class: eg: Region")

        parser.add_argument('oid',
                            help="Object id in CMS, eg: "
                                 "52d436c9e4b0ee66348b4518")

        return parser

    def take_action(self, parsed_args):
        client = self.app.cms_client

        result = client.delete(parsed_args.classname, parsed_args.oid)

        if result:
            return (['Status'],['OK'])
        else:
            raise RuntimeError('No result')

class DelAttrCommand(ShowOne):

    def get_parser(self, prog_name):
        parser = super(DelAttrCommand, self).get_parser(prog_name)

        parser.add_argument('classname',
                            help="Object class: eg: Region")

        parser.add_argument('oid',
                            help="Object id in CMS, eg: "
                                 "52d436c9e4b0ee66348b4518")

        parser.add_argument('attribute',
                            help="Attribute name in object, eg: "
                                 "networkDevices")

        return parser

    def take_action(self, parsed_args):
        client = self.app.cms_client

        result = client.delattr(parsed_args.classname,
                                parsed_args.oid,
                                parsed_args.attribute)

        if result:
            return (['Status'],['OK'])
        else:
            raise RuntimeError('No result')

class ListCommand(Lister):

    def get_parser(self, prog_name):
        parser = super(ListCommand, self).get_parser(prog_name)

        parser.add_argument('classname',
                            help="class name")

        parser.add_argument('--limit',
                            default=100,
                            help="Maximum returned result")

        parser.add_argument('--offset',
                            default=0,
                            help="Offset")

        parser.add_argument('--fields',
                            nargs='*',
                            help="fields to display")
        return parser

    def take_action(self, parsed_args):
        client = self.app.cms_client

        cm = client.manager

        hasmore, result = cm.list(parsed_args.classname,
                                  limit=parsed_args.limit,
                                  offset=parsed_args.offset)

        if hasmore:
            print 'More items available'

        if result:
            return _list_table(result, parsed_args.fields)
        else:
            raise RuntimeError('No result')
