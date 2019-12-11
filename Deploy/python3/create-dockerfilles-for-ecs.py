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
                create_dockerfile('api', data_map, config_map, root_path)
                create_dockerfile('worker', data_map, config_map, root_path)
            except yaml.YAMLError as exc:
                print(exc)
    else:
        print('ERROR: docker-compose.xml file is missing!')


def create_dockerfile(application, data_map, config_map, root_path):
    original_dockerfile = root_path / application / 'Dockerfile.original'
    generated_dockerfile = root_path / application / 'Dockerfile'
    config_parameters = config_map['applications'][application]['parameters']
    environment = data_map['services'][application]['environment']
    for i in environment:
        environment_parameter = i.split('=')[0]
        # Check to see if parameter is a known parameter
        try:
            test = config_parameters.index(environment_parameter)
            print(test)
        except ValueError:
            print("ERROR: there is a new unknown parameter "
                  + "in the docker-compose.yml file!")
        target_file = open(generated_dockerfile, 'w')
    with open(original_dockerfile) as source_file:
        count = 1
        for line in source_file:
            if count == 1:
                target_file.write(line)
                # Add arguments and environment to Dockerfile
                for i in config_parameters:
                    target_file.write('ARG ' + i.lower() + '_arg\n')
                for i in config_parameters:
                    target_file.write('ENV ' + i + '=$'
                                      + i.lower() + '_arg\n')
            else:
                target_file.write(line)
            count += 1
    target_file.close()
    source_file.close()


create_dockerfile_for_ecs()
