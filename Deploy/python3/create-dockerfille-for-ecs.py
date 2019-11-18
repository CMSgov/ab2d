#!/usr/bin/env python3

import yaml
from pathlib import Path

def create_dockerfile_for_ecs():

    root_path = Path('../generated')
    config_file = root_path / 'config.yml'
    docker_compose_file = root_path / 'docker-compose.yml'

    if config_file.exists():
        with open(config_file, 'r') as stream:
            try:
                config_map = yaml.safe_load(stream)
            except yaml.YAMLError as exc:
                print(exc)
    else:
        print('ERROR: docker-compose.xml file is missing!')

    if docker_compose_file.exists():
        with open(docker_compose_file, 'r') as stream:
            try:
                data_map = yaml.safe_load(stream)
                create_dockerfile_for_api(data_map, config_map, root_path)
                create_dockerfile_for_worker(data_map, config_map, root_path)
            except yaml.YAMLError as exc:
                print(exc)
    else:
        print('ERROR: docker-compose.xml file is missing!')

def create_dockerfile_for_api(data_map, config_map, root_path):
    api_original_dockerfile = root_path / 'api' / 'Dockerfile.original'
    api_generated_dockerfile = root_path / 'api' / 'Dockerfile'
    api_config_parameters = config_map['applications']['api']['parameters']
    print('API config parameters')
    for i in api_config_parameters:
        print(i.split('=')[0])
    api_environment = data_map['services']['api']['environment']
    print('API parameters')
    for i in api_environment:
        api_environment_parameter = i.split('=')[0]
        # Check to see if parameter is a known parameter
        try:
            test = api_config_parameters.index(api_environment_parameter)
        except ValueError as exc:
            print("ERROR: there is a new unknown parameter in the docker-compose.yml file!")
        target_file = open(api_generated_dockerfile, 'w')
    with open(api_original_dockerfile) as source_file:
        count = 1
        for line in source_file:
            if count == 1:
                target_file.write(line)
                # Add arguments and environment to Dockerfile
                #
                # *** TO DO ***
                #
            else:
                target_file.write(line)
            count += 1
    target_file.close()
    source_file.close()
    
def create_dockerfile_for_worker(data_map, config_map, root_path):
    worker_config_parameters = config_map['applications']['api']['parameters']
    print('Worker config parameters')
    for i in worker_config_parameters:
        print(i.split('=')[0])
    worker_environment = data_map['services']['worker']['environment']
    print('Worker parameters')
    for i in worker_environment:
        print(i.split('=')[0])

create_dockerfile_for_ecs()
