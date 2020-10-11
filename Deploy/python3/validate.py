import datetime
import glob
import json
import os
import re
import sys

VERBOSE = False
def to_year(date_string):
    return datetime.datetime.strptime(date_string, '%Y-%m-%d').year
def check_file(year, file):
    """
    :param year: year of billing period
    :type year: int
    :param file: full name of file to get
    :type file: str
    :return: number of files violating year check
    :rtype: tuple(int, int, int, int)
    """
    last_updated_time = 0
    bill_period_start = 0
    bill_period_end = 0
    service_period_start = 0
    service_period_end = 0
    with open(file, 'r') as result:
        line_in_file = 0
        for line in result:
            found_issue = False
            eob = json.loads(line)
            last_time = eob['meta']['lastUpdated']
            last_updated = to_year(last_time[:last_time.index('T')])
            bill_start = to_year(eob['billablePeriod']['start'])
            bill_end = to_year(eob['billablePeriod']['end'])
            if last_updated != year:
                found_issue = True
                last_updated_time += 1
            if bill_start != year:
                found_issue = True
                bill_period_start += 1
            if bill_end != year:
                found_issue = True
                bill_period_end += 1
            for item in eob['item']:
                if 'servicedPeriod' in item:
                    service_start = to_year(item['servicedPeriod']['start'])
                    service_end = to_year(item['servicedPeriod']['end'])
                    if service_start != year:
                        found_issue = True
                        service_period_start += 1
                    if service_end != year:
                        found_issue = True
                        service_period_end += 1
            if VERBOSE and found_issue:
                print('issue at %s-%d' % (result.name, line_in_file))
            line_in_file += 1
    return (last_updated_time, bill_period_start, bill_period_end, service_period_start, service_period_end)
# Code to walk results directory and check each file line by line
regex = re.compile(".*ndjson")
if len(sys.argv) < 3:
    print("Need two arguments. Year to expect and directory to check."
          " Optionally provide -v as third argument for verbose logging")
    exit(2)
expected_year = int(sys.argv[1])
directory = sys.argv[2]
if len(sys.argv) == 4 and sys.argv[3] == '-v':
    VERBOSE = True
# Iterate over each eob saved in each file and discover all issues
files = os.listdir(directory)
files_with_errors = 0
total_last_updated = 0
total_bill_start = 0
total_bill_end = 0
total_service_start = 0
total_service_end = 0
ndjson_files_count = len(glob.glob1(directory,"*.ndjson"))
print("\n--------------------------------")
print("         Files processed")
print("--------------------------------")
ndjson_files_processed_count = 0
for results_file in files:
    if not regex.match(results_file):
        continue
    (last_updated, bill_start, bill_end, service_start, service_end) =\
        check_file(expected_year, directory + '/' + results_file)
    issue = (last_updated + bill_start + bill_end + service_start + service_end) != 0
    if issue:
        print("\n%s has issues\t\tlast_updated: %d\tbill_start: %d\tbill_end: %d\tservice_start: %d\tservice_end: %d" %
              (results_file, last_updated, bill_start, bill_end, service_start, service_end))
    else:
        print("\n%s OK") % (results_file)
    total_last_updated += last_updated
    total_bill_start += bill_start
    total_bill_end = bill_end
    total_service_start = service_start
    total_service_end = service_end
    ndjson_files_processed_count += 1
print("\n--------------------------------")
print("File download and process counts")
print("--------------------------------\n")
print("Files downloaded: %d") % (ndjson_files_count)
print("Files processed: %d") % (ndjson_files_processed_count)
print("\n--------------------------------")
print("     Total issues detected")
print("--------------------------------\n")
print("last_updated: %d\nbill_start: %d\nbill_end: %d\nservice_start: %d\nservice_end: %d\n"
      % (total_last_updated, total_bill_start, total_bill_end, total_service_start, total_service_end))
