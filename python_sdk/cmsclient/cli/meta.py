__author__ = 'jemartin'

from cliff.lister import Lister


class GetCommand(Lister):

    def get_parser(self, prog_name):
        parser = super(GetCommand, self).get_parser(prog_name)

        parser.add_argument('object_class',
                            help="Object class")

        parser.add_argument('--filter',
                            nargs='*',
                            help="attribute type filter")

        return parser

    def take_action(self, parsed_args):
        client = self.app.cms_client
        cm = client.metadata[parsed_args.object_class]

        column = ['name', 'mandatory', 'cardinality',
                  'defaultValue', 'enumValues',
                  'dataType', 'refDataType', 'refRepository']
        data = []
        for k, v in cm.get('fields', []).items():
            if parsed_args.filter and v.get('dataType')\
                not in parsed_args.filter:
                continue
            row = (
                v.get('name'),
                v.get('mandatory'),
                v.get('cardinality'),
                v.get('defaultValue'),
                v.get('enumValues'),
                v.get('dataType'),
                v.get('refDataType'),
                v.get('refRepository')
            )
            data.append(row)

        return column, data

class ListCommand(Lister):

    def get_parser(self, prog_name):
        parser = super(ListCommand, self).get_parser(prog_name)

        parser.add_argument('--filter',
                            nargs='*',
                            help="attribute type filter")

        return parser

    def take_action(self, parsed_args):
        client = self.app.cms_client

        column = ['name', 'parent']
        data = []

        data = ((k, v.get('ancestors')) for k,v in client.metadata.items())

        return column, data


class GetRelCommand(Lister):

    def get_parser(self, prog_name):
        parser = super(GetRelCommand, self).get_parser(prog_name)

        parser.add_argument('object_class',
                            help="Object class")

        parser.add_argument('--filter',
                            choices=['from', 'to', 'all'],
                            help="filter relationships by direction")

        return parser

    def take_action(self, parsed_args):
        client = self.app.cms_client
        cm = client.metadata[parsed_args.object_class]

        column = ['Source', 'Destination']
        data = []
        fields = cm['fields']

        if parsed_args.filter == 'all' or parsed_args.filter == 'from':
            for r in [v for k, v in fields.items()
                      if v['dataType'] == 'relationship']:
                row = [r['name'], r.get('refDataType')]
                data.append(row)
        if parsed_args.filter == 'all' or parsed_args.filter == 'to':
            for r in client.inbound[parsed_args.object_class]:
                row = [r, parsed_args.object_class]
                data.append(row)

        return column, data