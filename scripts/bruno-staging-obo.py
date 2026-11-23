#!/usr/bin/env python3
"""Launch Bruno with staging OAuth settings without writing credentials to disk."""

import base64
import json
import os
from pathlib import Path
import re
import subprocess
import sys
from urllib.parse import urlsplit


ROOT = Path(__file__).resolve().parent.parent
ENV_FILE = ROOT / "bruno-collections/staging-datastore-obo/.env.staging.example"
BRUNO = "/Applications/Bruno.app/Contents/MacOS/Bruno"


def main():
    result = subprocess.run(
        [
            "kubectl", "get", "secret", "rcw-api-env-secrets",
            "-n", "laa-record-controlled-work-api-staging", "-o", "json",
            "--request-timeout=10s",
        ],
        check=True,
        capture_output=True,
        text=True,
    )
    data = json.loads(result.stdout)["data"]
    token_url = base64.b64decode(data["DATASTORE_TOKEN_URI"], validate=True).decode().strip()
    scope = base64.b64decode(data["DATASTORE_OAUTH_SCOPES"], validate=True).decode().strip()
    url = urlsplit(token_url)
    if (
        url.scheme != "https"
        or url.netloc != "login.microsoftonline.com"
        or not re.fullmatch(r"/[0-9a-fA-F-]{36}/oauth2/v2\.0/token", url.path)
        or url.query
        or url.fragment
        or not scope
    ):
        raise ValueError("Unexpected staging Entra token URL or empty datastore scope")

    env = os.environ.copy()
    env["STAGING_ENTRA_TOKEN_URL"] = token_url
    env["STAGING_ENTRA_AUTHORIZE_URL"] = token_url.removesuffix("/token") + "/authorize"
    env["STAGING_DATASTORE_SCOPE"] = scope
    os.execvpe("op", ["op", "run", "--env-file", str(ENV_FILE), "--", BRUNO], env)


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, KeyError, subprocess.CalledProcessError) as error:
        print(f"Could not launch staging Bruno: {error}", file=sys.stderr)
        sys.exit(1)
