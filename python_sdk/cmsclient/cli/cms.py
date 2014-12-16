__author__ = 'jemartin'

from cliff.app import App
from cliff.commandmanager import CommandManager
from cmsclient.client import Client
import logging
import os

def env(*_vars, **kwargs):
    """Search for the first defined of possibly many env vars

    Returns the first environment variable defined in vars, or
    returns the default defined in kwargs.

    """
    for v in _vars:
        value = os.environ.get(v, None)
        if value:
            return value
    return kwargs.get('default', '')

def import_class(import_str):
    """Returns a class from a string including module and class

    :param import_str: a string representation of the class name
    :rtype: the requested class
    """
    import sys
    mod_str, _sep, class_str = import_str.rpartition('.')
    __import__(mod_str)
    return getattr(sys.modules[mod_str], class_str)

COMMANDS = {
    'meta get': import_class(
        'cmsclient.cli.meta.GetCommand'),
    'meta list': import_class(
        'cmsclient.cli.meta.ListCommand'),
    'meta get-rel': import_class(
        'cmsclient.cli.meta.GetRelCommand'),
    'obj get': import_class(
        'cmsclient.cli.objects.GetCommand'),
    'obj create': import_class(
        'cmsclient.cli.objects.CreateCommand'),
    'obj addrel': import_class(
        'cmsclient.cli.objects.AddRelCommand'),
    'obj update': import_class(
        'cmsclient.cli.objects.UpdateCommand'),
    'obj delete': import_class(
        'cmsclient.cli.objects.DeleteCommand'),
    'obj delattr': import_class(
        'cmsclient.cli.objects.DelAttrCommand'),
    'obj find': import_class(
        'cmsclient.cli.objects.QueryCommand'),
    'obj list': import_class(
        'cmsclient.cli.objects.ListCommand'),
    'obj to': import_class(
        'cmsclient.cli.objects.GetRelationsCommand')
}


class Shell(App):
    log = logging.getLogger(__name__)

    def __init__(self):
        super(Shell, self).__init__(
            description='CMS Management CLI',
            version='1.0',
            command_manager=CommandManager('cmsclient.cli.cms')
        )
        self.commands = COMMANDS
        for k, v in self.commands.items():
            self.command_manager.add_command(k, v)


    def build_option_parser(self, description, version):
        parser = super(Shell, self).build_option_parser(
            description,
            version
        )
        parser.add_argument(
            '--service', metavar='<service>',
            default=env('CMS_SERVICE', default='localhost'),
            help='address of the CMS Service'
        )
        parser.add_argument(
            '--repo', metavar='<repo>',
            default=env('CMS_REPO', default='cmsdb'),
            help='Name of the Repository'
        )
        parser.add_argument(
            '--token', metavar='<token>',
            default=env('CMS_API_TOKEN'),
            help='Stratus Security Service Auth Token'
        )
        parser.add_argument(
            '--metadata', metavar='<metadata>',
            help='cached copy of metadata'
        )

        return parser

    def initialize_app(self, argv):
        if not self.options.service:
            raise Exception('missing Service Address')
        self.cms_client = Client(self.options.service,
                                 self.options.repo,
                                 token=self.options.token,
                                 filename=self.options.metadata)

