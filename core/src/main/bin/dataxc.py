#!/usr/bin/env python
# -*- coding:utf-8 -*-

import json
import os
import platform
import re
import signal
import subprocess
import sys
import time
import urllib2
from apollo.apollo_client import ApolloClient
from optparse import OptionGroup
from optparse import OptionParser
from string import Template

ispy2 = sys.version_info.major == 2

def is_windows():
    return platform.system() == 'Windows'

ENGINE_COMMAND = "python /opt/app/datax/bin/datax.py ${job_json_file} ${params}"

RET_STATE = {
    "KILL": 143,
    "FAIL": -1,
    "OK": 0,
    "RUN": 1,
    "RETRY": 2
}


def suicide(signum):
    global child_process
    if ispy2:
        print >> sys.stderr, "[Error] DataX receive unexpected signal %d, starts to suicide." % (signum)
    else:
        print("[Error] DataX receive unexpected signal %d, starts to suicide." % (signum), sys.stderr)

    if child_process:
        child_process.send_signal(signal.SIGQUIT)
        time.sleep(1)
        child_process.kill()
    if ispy2:
        print >> sys.stderr, "DataX Process was killed ! you did ?"
    else:
        print("DataX Process was killed ! you did ?", sys.stderr)
    sys.exit(RET_STATE["KILL"])


def register_signal():
    if not is_windows():
        global child_process
        signal.signal(2, suicide)
        signal.signal(3, suicide)
        signal.signal(15, suicide)


def getOptionParser():
    usage = "usage: %prog [options] job-url-or-path"
    option_parser = OptionParser(usage=usage)

    prod_env_option_group = OptionGroup(option_parser, "Product Env Options",
                                        "Normal user use these options to set jvm parameters, job runtime mode etc. "
                                        "Make sure these options can be used in Product Env.")
    prod_env_option_group.add_option("-j", "--jvm", metavar="<jvm parameters>", dest="jvmParameters", action="store",
                                     help="Set jvm parameters if necessary.")
    prod_env_option_group.add_option("--jobid", metavar="<job unique id>", dest="jobid", action="store",
                                     help="Set job unique id when running by Distribute/Local Mode.")
    prod_env_option_group.add_option("-m", "--mode", metavar="<job runtime mode>",
                                     action="store",
                                     help="Set job runtime mode such as: standalone, local, distribute. "
                                          "Default mode is standalone.")
    prod_env_option_group.add_option("-p", "--params", metavar="<parameter used in job config>",
                                     action="store", dest="params",
                                     help='Set job parameter, eg: the source tableName you want to set it by command, '
                                          'then you can use like this: -p"-DtableName=your-table-name", '
                                          'if you have mutiple parameters: -p"-DtableName=your-table-name '
                                          '-DcolumnName=your-column-name". '
                                          'Note: you should config in you job tableName with ${tableName}.')
    prod_env_option_group.add_option("-r", "--reader", metavar="<parameter used in view job config[reader] template>",
                                     action="store", dest="reader", type="string",
                                     help='View job config[reader] template, eg: mysqlreader,streamreader')
    prod_env_option_group.add_option("-w", "--writer", metavar="<parameter used in view job config[writer] template>",
                                     action="store", dest="writer", type="string",
                                     help='View job config[writer] template, eg: mysqlwriter,streamwriter')
    option_parser.add_option_group(prod_env_option_group)

    dev_env_option_group = OptionGroup(option_parser, "Develop/Debug Options",
                                       "Developer use these options to trace more details of DataX.")
    dev_env_option_group.add_option("-d", "--debug", dest="remoteDebug", action="store_true",
                                    help="Set to remote debug mode.")
    dev_env_option_group.add_option("--loglevel", metavar="<log level>", dest="loglevel", action="store",
                                    help="Set log level such as: debug, info, all etc.")
    option_parser.add_option_group(dev_env_option_group)
    return option_parser


def is_url(path):
    if not path:
        return False

    assert (isinstance(path, str))
    m = re.match(r"^http[s]?://\S+\w*", path.lower())
    if m:
        return True
    else:
        return False


def build_start_command(options, json_file):
    print(options)
    command_map = {"job_json_file": json_file}
    params = ""
    if options.jvmParameters:
        params += " -j " + options.jvmParameters

    if options.remoteDebug:
        params += " -d " + options.remoteDebug

    if options.loglevel:
        params += " --loglevel " + options.loglevel

    if options.mode:
        params += " -m " + options.mode

    if options.params:
        params += " -p \"" + options.params + "\""

    if options.jobid:
        params += " --jobid " + options.jobid

    if options.reader:
        params += " -r " + options.reader

    if options.writer:
        params += " -w " + options.writer

    command_map["params"] = params

    print(Template(ENGINE_COMMAND).substitute(**command_map))

    return Template(ENGINE_COMMAND).substitute(**command_map)


def get_config(keys):
    try:
        value_map = {}
        for key in keys:
            config_key = key.lstrip("#{").rstrip("}")
            value = client.get_value(config_key)
            if value:
                value_map[key] = value
            else:
                raise Exception("can not get config for " + config_key + " from config center")
        # print "get config for key: ", value_map
        return value_map
    except Exception as e:
        print("catch exception when get config from config center")
        print(e)


def build_new_job(args):
    # jobResource 可能是 URL，也可能是本地文件路径（相对,绝对）
    job_resource = args[0]
    if not is_url(job_resource):
        job_resource = os.path.abspath(job_resource)
        if job_resource.lower().startswith("file://"):
            job_resource = job_resource[len("file://"):]

    with open(job_resource, 'rt') as f:
        job_json = f.read().decode('utf-8')
        r = r"#{.*?}"
        m = list(set(re.findall(r, job_json)))
        # if len(args) > 1:
        #     for i in range(1, len(args)):
        #         m.remove(args[i])
        value_map = get_config(m)
        new_job_json = job_json
        for tag in m:
            new_job_json = new_job_json.replace(tag, value_map[tag])

        filename = job_resource.split(os.sep)[-1]
        new_job_resource = os.sep + "tmp" + os.sep + filename  # job_resource + "." + str(int(time.time()))
        with open(new_job_resource, 'w') as nf:
            nf.write(new_job_json.encode('utf-8'))
            print("write new job json file: ", new_job_resource)
        return new_job_resource


if __name__ == "__main__":
    apollo_config_url = os.environ.get("APOLLO_CONFIG_URL")
    apollo_access_key_secret = os.environ.get("APOLLO_ACCESS_KEY_SECRET")
    client = ApolloClient(config_url=apollo_config_url, app_id='it-dipper-etl', cluster='default', secret=apollo_access_key_secret)

    parser = getOptionParser()
    options, args = parser.parse_args(sys.argv[1:])
    job_json_file = build_new_job(args)

    startCommand = build_start_command(options, job_json_file)
    print(startCommand)

    child_process = subprocess.Popen(startCommand, shell=True)
    register_signal()
    (stdout, stderr) = child_process.communicate()

    if os.path.exists(job_json_file):
        os.remove(job_json_file)
        print("remove temp job json file: ", job_json_file)

    sys.exit(child_process.returncode)
