"""Downloads certstores and healthcheck PEM and saves them locally to specified paths.

Used as the entrypoint for the "ab2-mount-keystore" container image which is run as part of
the "worker" ECS Service. The keystore is downloaded to a shared bind mount
from which the AB2D Worker reads them.
"""

import boto3
import click


@click.command()
@click.option(
    "-b",
    "--bucket",
    envvar="BUCKET",
    type=str,
    required=True,
    help="The name of the bucket containing the keystore",
)
@click.option(
    "-k",
    "--keystore-key",
    envvar="KEYSTORE_KEY",
    type=str,
    default="keystore.pfx",
    show_default=True,
    help="The S3 key of the keystore file",
)
@click.option(
    "-K",
    "--keystore-out",
    envvar="KEYSTORE_OUTPUT_PATH",
    type=str,
    default="./keystore.pfx",
    show_default=True,
    help="The local output path for the keystore file",
)
@click.option(
    "-r",
    "--region",
    envvar="REGION",
    type=str,
    default="us-east-1",
    show_default=True,
    help="AWS Region",
)
def main(
    bucket: str,
    keystore_key: str,
    keystore_out: str,
    region: str,
) -> None:
    """Download the certstores from the specified S3 bucket and the healthcheck PEM certificate."""
    print(f"Bucket: {bucket}")
    print(f"Region: {region}")
    print(f"Keystore Key: {keystore_key}")
    print(f"Keystore Output Path: {keystore_out}")

    print("Downloading certstores from S3...")
    s3 = boto3.client("s3")  # type: ignore

    s3.download_file(Bucket=bucket, Key=keystore_key, Filename=keystore_out)

    print("Keystore downloaded successfully.")


if __name__ == "__main__":
    main()
