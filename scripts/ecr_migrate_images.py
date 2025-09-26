#!/usr/bin/env python3
"""
Pull ECR images (latest/release/in-use) and later push them back.

Subcommands:
  - pull : logs into ECR, discovers tags, pulls images locally, writes ecr_backup_tags.json
  - push : reads ecr_backup_tags.json and pushes images back to ECR

Requires AWS credentials (env/profile) with access to ECR/ECS.

Examples:
  ./ecr_migrate_images.py pull  --account 539247469933 --region us-east-1 --repos ab2d-api ab2d-worker ab2d-properties ab2d-contracts ab2d-events
  ./ecr_migrate_images.py push  --account 539247469933 --region us-east-1
"""

import argparse
import base64
import json
import logging
import os
import re
from typing import Dict, List, Set, Iterable

import boto3
from botocore.config import Config
from botocore.exceptions import ClientError
import docker


BACKUP_FILE = "ecr_backup_tags.json"
DEFAULT_PATTERNS = [
    r"^latest$",
    r"^v[0-9].*",                             # v1, v1.2.3, v1.2.3-rc1
    r"^[0-9]+\.[0-9]+\.[0-9]+(-.*)?$",        # 1.2.3 or 1.2.3-foo
    r"^release-.*$",
    r"^prod-.*$",
    r"^test-.*$",
]


def setup_logging(verbose: bool):
    logging.basicConfig(
        level=logging.DEBUG if verbose else logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )


def boto_session(profile: str | None, region: str):
    if profile:
        return boto3.Session(profile_name=profile, region_name=region)
    return boto3.Session(region_name=region)


def ecr_registry_domain(account: str, region: str) -> str:
    return f"{account}.dkr.ecr.{region}.amazonaws.com"


def docker_login_to_ecr(dkr_client: docker.DockerClient, ecr_client, account: str, region: str):
    """
    Login to ECR using GetAuthorizationToken (works for classic ECR).
    """
    registry = ecr_registry_domain(account, region)
    try:
        token_resp = ecr_client.get_authorization_token(registryIds=[account])
        auth_data = token_resp["authorizationData"][0]
        b64_token = auth_data["authorizationToken"]
        userpass = base64.b64decode(b64_token).decode("utf-8")
        username, password = userpass.split(":", 1)
        dkr_client.login(username=username, password=password, registry=registry)
        logging.info("Docker login OK for %s", registry)
    except ClientError as e:
        logging.error("Failed to get ECR auth token: %s", e)
        raise


def paginate(method, **kwargs) -> Iterable[dict]:
    """
    Generic paginator for AWS APIs that use 'nextToken' or 'NextToken' or 'nextToken' like fields.
    """
    token_keys = ["nextToken", "NextToken", "next_token"]
    while True:
        resp = method(**kwargs)
        yield resp
        token = None
        for k in token_keys:
            if k in resp and resp[k]:
                token = resp[k]
                break
        if not token:
            break
        kwargs["nextToken"] = token  # most APIs accept nextToken


def list_all_ecr_tags(ecr_client, repo: str) -> List[str]:
    tags = []
    for page in paginate(
        ecr_client.list_images,
        repositoryName=repo,
        filter={"tagStatus": "TAGGED"},
        maxResults=1000,
    ):
        for item in page.get("imageIds", []):
            t = item.get("imageTag")
            if t:
                tags.append(t)
    return sorted(set(tags))


def regex_any(patterns: List[str], s: str) -> bool:
    return any(re.search(p, s) for p in patterns)


def ecs_in_use_tags(session, account: str, region: str, repos: Set[str]) -> Dict[str, Set[str]]:
    """
    Discover tags (or resolve digests to tags) currently referenced by ACTIVE ECS services.
    Returns: { repo_name: {tag1, tag2, ...}, ... }
    """
    ecs = session.client("ecs", config=Config(retries={"max_attempts": 10}))
    ecr = session.client("ecr", config=Config(retries={"max_attempts": 10}))
    reg = ecr_registry_domain(account, region)
    result: Dict[str, Set[str]] = {}

    # List clusters
    cluster_arns: List[str] = []
    for page in paginate(ecs.list_clusters, maxResults=100):
        cluster_arns.extend(page.get("clusterArns", []))

    if not cluster_arns:
        return result

    def add_tag(repo: str, tag: str):
        if repo not in result:
            result[repo] = set()
        result[repo].add(tag)

    # For each cluster, list services and map to task definitions
    for cluster in cluster_arns:
        service_arns: List[str] = []
        for page in paginate(ecs.list_services, cluster=cluster, maxResults=100):
            service_arns.extend(page.get("serviceArns", []))
        if not service_arns:
            continue

        # Describe services in chunks
        for i in range(0, len(service_arns), 10):
            chunk = service_arns[i : i + 10]
            desc = ecs.describe_services(cluster=cluster, services=chunk)
            tds = [s["taskDefinition"] for s in desc.get("services", []) if "taskDefinition" in s]
            for td in tds:
                td_desc = ecs.describe_task_definition(taskDefinition=td)
                images = [
                    c["image"]
                    for c in td_desc["taskDefinition"].get("containerDefinitions", [])
                    if "image" in c
                ]
                for img in images:
                    # Match only our account/region ECR images
                    # forms: <acct>.dkr.ecr.<region>.amazonaws.com/<repo>:<tag>
                    #        <acct>.dkr.ecr.<region>.amazonaws.com/<repo>@sha256:<digest>
                    m = re.match(
                        rf"^{re.escape(reg)}/([^:@]+)(?::([^@]+))?(?:@sha256:[0-9a-f]+)?$",
                        img,
                    )
                    if not m:
                        continue
                    repo, maybe_tag = m.group(1), m.group(2)
                    if repo not in repos:
                        continue
                    if maybe_tag:
                        add_tag(repo, maybe_tag)
                    else:
                        # resolve digest -> tags for this repo
                        # image form is likely ".../<repo>@sha256:..."
                        if "@sha256:" in img:
                            digest = img.split("@")[-1]
                            try:
                                di = ecr.describe_images(
                                    repositoryName=repo,
                                    imageIds=[{"imageDigest": digest}],
                                )
                                tags = di["imageDetails"][0].get("imageTags", [])
                                for t in tags:
                                    add_tag(repo, t)
                            except ClientError as e:
                                logging.warning("Could not resolve tags for %s (%s): %s", img, repo, e)

    return result


def pull_images(
    session,
    account: str,
    region: str,
    repos: List[str],
    extra_patterns: List[str] | None,
    verbose: bool,
):
    ecr = session.client("ecr", config=Config(retries={"max_attempts": 10}))
    dkr = docker.from_env()
    docker_login_to_ecr(dkr, ecr, account, region)

    repo_set = set(repos)
    patterns = DEFAULT_PATTERNS + (extra_patterns or [])
    registry = ecr_registry_domain(account, region)

    # Discover ECS in-use tags
    logging.info("Scanning ECS for in-use tags…")
    inuse = ecs_in_use_tags(session, account, region, repo_set)

    backup: Dict[str, List[str]] = {}
    for repo in repos:
        logging.info("Discovering tags for %s", repo)
        all_tags = list_all_ecr_tags(ecr, repo)

        want: Set[str] = set(t for t in all_tags if regex_any(patterns, t))
        want |= inuse.get(repo, set())

        if not want:
            logging.info("  No tags to pull for %s", repo)
            continue

        # Pull each tag
        for tag in sorted(want):
            ref = f"{registry}/{repo}:{tag}"
            try:
                logging.info("  Pulling %s", ref)
                dkr.images.pull(ref)
            except Exception as e:
                logging.error("  Failed to pull %s: %s", ref, e)
                continue

        backup[repo] = sorted(want)

    # Save the list we actually pulled
    with open(BACKUP_FILE, "w", encoding="utf-8") as f:
        json.dump(
            {"account": account, "region": region, "repos": backup},
            f,
            indent=2,
            sort_keys=True,
        )
    logging.info("Wrote %s", BACKUP_FILE)


def push_images(session, account: str, region: str, verbose: bool):
    # Read backup file
    if not os.path.exists(BACKUP_FILE):
        raise SystemExit(f"{BACKUP_FILE} not found. Run `pull` first.")
    with open(BACKUP_FILE, "r", encoding="utf-8") as f:
        data = json.load(f)

    if data.get("account") != account or data.get("region") != region:
        logging.warning(
            "Backup file account/region (%s/%s) != current (%s/%s)",
            data.get("account"),
            data.get("region"),
            account,
            region,
        )

    repos: Dict[str, List[str]] = data.get("repos", {})
    if not repos:
        logging.info("No repos to push.")
        return

    ecr = session.client("ecr", config=Config(retries={"max_attempts": 10}))
    dkr = docker.from_env()
    docker_login_to_ecr(dkr, ecr, account, region)

    registry = ecr_registry_domain(account, region)
    api = dkr.api

    # Push each saved tag
    for repo, tags in repos.items():
        for tag in tags:
            ref = f"{registry}/{repo}:{tag}"
            logging.info("Pushing %s", ref)
            # Ensure the tag exists locally (pull step should have created it)
            try:
                # Use low-level API to stream push progress
                for line in api.push(repository=f"{registry}/{repo}", tag=tag, stream=True, decode=True):
                    if "error" in line:
                        raise RuntimeError(line["error"])
                    # reduce noise: only print key status lines when verbose
                    if verbose and ("status" in line or "progress" in line):
                        status = line.get("status")
                        detail = line.get("progress", "")
                        if status:
                            logging.debug("  %s %s", status, detail)
            except Exception as e:
                logging.error("  Failed to push %s: %s", ref, e)

    logging.info("Push complete.")


def main():
    parser = argparse.ArgumentParser(description="ECR pull/push helper for KMS migration")
    sub = parser.add_subparsers(dest="cmd", required=True)

    def common(p):
        p.add_argument("--profile", help="AWS profile name (optional)")
        p.add_argument("--region", required=True, help="AWS region, e.g. us-east-1")
        p.add_argument("--account", required=True, help="AWS account ID")
        p.add_argument("-v", "--verbose", action="store_true", help="Verbose logging")

    p_pull = sub.add_parser("pull", help="Pull/backup images")
    common(p_pull)
    p_pull.add_argument(
        "--repos",
        nargs="+",
        required=True,
        help="ECR repository names (space-separated)",
    )
    p_pull.add_argument(
        "--pattern",
        action="append",
        help="Additional regex for tag selection (can repeat)",
    )

    p_push = sub.add_parser("push", help="Push/restore images")
    common(p_push)

    args = parser.parse_args()
    setup_logging(args.verbose)
    session = boto_session(args.profile, args.region)

    if args.cmd == "pull":
        pull_images(
            session=session,
            account=args.account,
            region=args.region,
            repos=args.repos,
            extra_patterns=args.pattern,
            verbose=args.verbose,
        )
    elif args.cmd == "push":
        push_images(
            session=session,
            account=args.account,
            region=args.region,
            verbose=args.verbose,
        )


if __name__ == "__main__":
    main()
