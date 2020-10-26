import argparse
from datetime import datetime
import logging
import os
import re
import requests
import time

logging.basicConfig()
logging.getLogger().setLevel(logging.DEBUG)
requests_log = logging.getLogger("requests.packages.urllib3")
requests_log.setLevel(logging.DEBUG)
requests_log.propagate = True


class ApiError(Exception):
    pass


class Action:
    EXPIRATION_BUFFER=90

    def __init__(self, idp_url, api_url, auth, verbose=False):
        """
        :param idp_url: url of okta identity provider
        :type idp_url: str
        :param api_url: url of AB2D api
        :type api_url: str
        :param auth: base64 encoded okta client password and okta client id
        :type auth: str
        """
        self.idp_url = idp_url
        self.api_url = api_url
        self.auth = auth
        self.verbose = verbose
        self.token = None
        self.token_generated = None

    def retrieve_token(self):
        """
        Retrieve an Okta bearer token from the given provider
        :return:
        """

        full_url = "%s?grant_type=client_credentials&scope=clientCreds" % self.idp_url
        basic_auth = "Basic %s" % self.auth

        self.token_generated = time.time()

        response = requests.post(full_url, headers={
            "Content-Type": "application/x-www-form-urlencoded",
            "Accept": "application/json",
            "Authorization": basic_auth
        })

        if response.status_code != 200:
            raise ApiError("Could not retrieve bearer token from %s. Response %d. Message %s"
                           % (full_url, response.status_code, response.read()))

        self.token = response.json()
        return self.token

    def get_or_refresh_token(self):
        if self.token is None:
            return self.retrieve_token()

        expires_at = self.token_generated + self.token['expires'] - Action.EXPIRATION_BUFFER
        if expires_at < time.time():
            return self.retrieve_token()

        return self.token


class StartJob(Action):

    def __init__(self, idp_url, api_url, auth, job_id_file, contract=None):
        """
        Start a job in the
        :param job_id_file: location to read job id from
        :type job_id_file: file
        :param contract: the contract
        """
        super().__init__(idp_url, api_url, auth)
        self.job_id_file = job_id_file
        self.contract = contract

    def start_url(self):
        if self.contract:
            return "%s/Group/%s/$export" % (self.api_url, self.contract)
        else:
            return "%s/Patient/$export" % self.api_url

    def __call__(self, *args, **kwargs):

        token = self.retrieve_token()

        url = self.start_url()

        response = requests.get(url, headers={
            "Accept": "application/json",
            "Prefer": "respond-async",
            "Authorization": "Bearer %s" % token['access_token']
        }, params={
            '_outputFormat': "application/fhir+ndjson",
            "_type": "ExplanationOfBenefit"
        })

        status = response.status_code
        if status != 200 and status != 202:
            raise ApiError("Could not start job at url %s. Response %d. Message %s"
                           % (url, response.status_code, response.json()))

        status_url = response.headers['content-location']
        job_id = re.findall('Job/(.*)/\\$status', status_url)[0]

        with open(self.job_id_file, 'w+') as job_file:
            job_file.write(job_id)


class MonitorJob(Action):

    def __init__(self, idp_url, api_url, auth, job_id_path, response_path):
        """
        :param job_id_path: location to read job id from
        :type job_id_path: file
        :param response_path: location to save results on finishing job
        :type response_path: file
        """
        super().__init__(idp_url, api_url, auth)
        self.job_id_path = job_id_path
        self.response_path = response_path
        self.url = self.get_monitor_url()

    def get_monitor_url(self):

        with open(self.job_id_path, 'r') as job_file:
            job_id = job_file.readline()

            return "%s/Job/%s/$status" % (self.api_url, job_id)

    def check_status(self):
        token = self.get_or_refresh_token()
        return requests.get(self.url, headers={
            "Accept": "application/json",
            "Prefer": "respond-async",
            "Authorization": "Bearer %s" % token['access_token']
        })

    def __call__(self, *args, **kwargs):

        response = self.check_status()
        while response.status_code != 200:
            if response == 202:
                time.sleep(60)
                response = self.check_status()
            else:
                raise ApiError("Could not retrieve bearer token from %s. Response %d. Message %s"
                               % (self.url, response.status_code, response.json()))

        with open(self.response_path, 'w+') as response_file:
            output = response.json()['output']
            for file in output:
                response_file.write(file['url'])


class DownloadResults(Action):

    def __init__(self, idp_url, api_url, auth, directory, response_path):
        """
        :param response_path: location to read list of files to import from job
        :type response_path: file
        """
        super().__init__(idp_url, api_url, auth)
        self.directory = directory
        self.response_path = response_path

    # Based on https://stackoverflow.com/questions/16694907/download-large-file-in-python-with-requests
    def download(self, url):

        token = self.get_or_refresh_token()
        local_filename = url.split('/')[-1]
        local_path = self.directory + os.sep + local_filename

        # NOTE the stream=True parameter below
        with requests.get(url, headers={
            "Accept": "application/fhir+ndjson",
            "Authorization": "Bearer %s" % token['access_token']
        }, stream=True) as r:

            # Raise error on failure of the call
            r.raise_for_status()

            # Write out to a file
            with open(local_path, 'wb') as f:
                for chunk in r.iter_content(chunk_size=8192):
                    # If you have chunk encoded response uncomment if
                    # and set chunk_size parameter to None.
                    #if chunk:
                    f.write(chunk)
        return local_filename

    def __call__(self, *args, **kwargs):

        with open(self.response_path, 'r') as response_file:
            urls = response_file.readlines()

            for url in urls:
                self.download(url)


def get_env(args):
    """
    Check that an environment was selected and provide the appropriate URLs for connecting
    to the okta idp (for a bearer token) and the AB2D api (for running jobs)

    :param args: list of arguments provided on command line
    :type args: argparse.Namespace
    :return: base url of identity provider and base url of ab2d api
    :rtype: (str, str)
    """

    if args.prod and args.sandbox:
        raise ValueError("must choose either -prod or -sandbox as an argument")

    if not args.prod and not args.sandbox:
        raise ValueError("must provide -prod or -sandbox as an argument")

    if args.sandbox:
        return ("https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token",
                "https://sandbox.ab2d.cms.gov/api/v1/fhir")
    else:
        return ("https://idm.cms.gov/oauth2/aus2ytanytjdaF9cr297/v1/token",
                "https://api.ab2d.cms.gov/api/v1/fhir")


parser = argparse.ArgumentParser()
parser.add_argument("-prod", action="store_true", help="run a job against the AB2D production environment")
parser.add_argument("-sandbox", action="store_true", help="run a job against the AB2D sandbox environment")
parser.add_argument("--contract", help="pull data for a specific contract")
parser.add_argument("--directory", default="./",
                    help="set the directory to save results to, defaults to current directory")
parser.add_argument("--auth", required=True, help="base64 encoding of okta client id and okta client secret")
parser.add_argument("--only_start", action="store_true", help="only start a job don't monitor it")
parser.add_argument("--only_monitor", action="store_true", help="only monitor an already started job"
                                                                "do not start or download")
parser.add_argument("--only_download", action="store_true", help="only download results from an already finished job"
                                                                 "do not start or monitor")

args = parser.parse_args()

try:
    idp_url, api_url = get_env(args)

    if not os.path.exists(args.directory):
        raise ValueError("directory does not exist: %s" % args.directory)

    job_id_path = "%s%c%s" % (args.directory, os.sep, "job_id.txt")
    completion_id_path = "%s%c%s" % (args.directory, os.sep, "response.json")

    tasks = list()
    if args.only_start:
        tasks.append(StartJob(idp_url, api_url, args.auth, job_id_path))
    elif args.only_monitor:
        tasks.append(MonitorJob(idp_url, api_url, args.auth, job_id_path, completion_id_path))
    elif args.only_download:
        tasks.append(DownloadResults(idp_url, api_url, args.auth, args.directory, completion_id_path))
    else:
        tasks.append(StartJob(idp_url, api_url, args.auth, job_id_path))
        tasks.append(MonitorJob(idp_url, api_url, args.auth, job_id_path, completion_id_path))
        tasks.append(DownloadResults(idp_url, api_url, args.auth, completion_id_path))

    for task in tasks:
        print(task.__class__)
        task()

except ValueError as ve:
    print(ve)

    parser.print_help()
except ApiError as ae:
    print(ae)