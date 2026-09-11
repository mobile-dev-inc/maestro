"""host_fetch.py — runs ON a remote host: mint a GCS OAuth token from the
service-account key (openssl + python3 stdlib, no google-auth/gcloud/gsutil),
then download each flow's app binary + workspace.zip from the configured GCS bucket and
unzip the workspace. Stdlib only. Never prints the key, token, or creds."""
from __future__ import annotations

import argparse
import base64
import http.client
import json
import os
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
import zipfile

_SCOPE = "https://www.googleapis.com/auth/devstorage.read_only"
_AUD = "https://oauth2.googleapis.com/token"


def _b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def build_signing_input(client_email, now, scope=_SCOPE, aud=_AUD, lifetime=3600):
    header = {"alg": "RS256", "typ": "JWT"}
    claims = {"iss": client_email, "scope": scope, "aud": aud,
              "iat": now, "exp": now + lifetime}
    h = _b64url(json.dumps(header, separators=(",", ":")).encode())
    c = _b64url(json.dumps(claims, separators=(",", ":")).encode())
    return f"{h}.{c}"


def sign_rs256(signing_input, private_key_pem, runner=subprocess.run):
    # openssl -sign needs the key in a file; write it 0600 and always remove it.
    fd, keyfile = tempfile.mkstemp(prefix="hf-", suffix=".pem")
    try:
        os.close(fd)
        os.chmod(keyfile, 0o600)
        with open(keyfile, "w") as fh:
            fh.write(private_key_pem)
        p = runner(["openssl", "dgst", "-sha256", "-sign", keyfile],
                   input=signing_input.encode(), capture_output=True, check=False)
        if p.returncode != 0:
            # Deliberately generic: never echo the key or openssl stderr.
            raise RuntimeError("openssl signing failed")
        return _b64url(p.stdout)
    finally:
        try:
            os.unlink(keyfile)
        except OSError:
            pass


def mint_token(key_path, urlopen=urllib.request.urlopen, runner=subprocess.run, now=None):
    with open(key_path) as fh:
        key = json.load(fh)
    now = int(time.time()) if now is None else now
    signing_input = build_signing_input(key["client_email"], now)
    signature = sign_rs256(signing_input, key["private_key"], runner=runner)
    assertion = f"{signing_input}.{signature}"
    body = urllib.parse.urlencode({
        "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
        "assertion": assertion,
    }).encode()
    req = urllib.request.Request(
        _AUD, data=body,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
    )
    with urlopen(req, timeout=30) as resp:
        payload = json.loads(resp.read())
    tok = payload.get("access_token")
    if not tok:
        # Never echo the payload — it may carry error detail but also the assertion.
        raise RuntimeError("token exchange returned no access_token")
    return tok


_DOWNLOAD_BASE = "https://storage.googleapis.com/download/storage/v1/b"
_CHUNK = 1024 * 1024


def object_media_url(bucket, object_path):
    encoded = urllib.parse.quote(object_path, safe="")
    return f"{_DOWNLOAD_BASE}/{bucket}/o/{encoded}?alt=media"


def download_object(bucket, object_path, dest, token,
                    urlopen=urllib.request.urlopen, retries=3, sleep=time.sleep,
                    token_provider=None):
    """token_provider: optional zero-arg callable that re-mints a fresh token.
    A stage can outlive the token minted at the start of a host run; a 401
    is recoverable (re-mint once, retry) instead of the fail-fast path used
    for every other 4xx. Never log the token on any path."""
    url = object_media_url(bucket, object_path)
    part = dest + ".part"
    last = None
    refreshed = False
    attempt = 0
    while True:
        try:
            req = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
            with urlopen(req, timeout=120) as resp:
                cl = resp.headers.get("Content-Length")
                expected = int(cl) if cl not in (None, "") else None
                n = 0
                with open(part, "wb") as fh:
                    while True:
                        chunk = resp.read(_CHUNK)
                        if not chunk:
                            break
                        fh.write(chunk)
                        n += len(chunk)
            if expected is not None and n != expected:
                os.unlink(part)
                raise IOError(f"truncated download: got {n} of {expected} bytes for {object_path}")
            os.replace(part, dest)
            return n
        except urllib.error.HTTPError as e:
            if e.code == 401 and token_provider is not None and not refreshed:
                token = token_provider()
                refreshed = True
                _rm(part)
                continue                      # retry now with the fresh token; doesn't
                                               # count against the transient-error budget
            if 400 <= e.code < 500:
                _rm(part)
                raise                          # 4xx (e.g. 404) — not transient
            last = e
        except (urllib.error.URLError, IOError, http.client.IncompleteRead) as e:
            last = e
        _rm(part)
        attempt += 1
        if attempt >= retries:
            break
        sleep(min(2 ** (attempt - 1), 8))
    raise RuntimeError(f"download failed after {retries} attempts for {object_path}: {last}")


def _rm(path):
    try:
        os.unlink(path)
    except OSError:
        pass


_APP_NAME = {"ANDROID": "app.apk", "IOS": "app.ipa"}


def fetch_folder(folder, bucket, token, urlopen=urllib.request.urlopen, token_provider=None):
    """token_provider: optional zero-arg callable that re-mints a fresh token,
    threaded down to download_object so a 401 (token outlived by a long stage)
    is recovered with a single re-mint-and-retry instead of failing the folder."""
    with open(os.path.join(folder, "metadata.json")) as fh:
        meta = json.load(fh)
    platform = str(meta["platform"]).upper()

    dl_kwargs = {"urlopen": urlopen}
    if token_provider is not None:
        dl_kwargs["token_provider"] = token_provider

    # workspace.zip -> workspace/ (the replay tars the whole workspace dir).
    ws_dest = os.path.join(folder, "workspace.zip")
    download_object(bucket, meta["workspace_storage_path"], ws_dest, token, **dl_kwargs)
    ws_dir = os.path.join(folder, "workspace")
    os.makedirs(ws_dir, exist_ok=True)
    with zipfile.ZipFile(ws_dest) as z:
        z.extractall(ws_dir)

    # app binary — skipped for a built-in app (requires_app_install: false) or when
    # no app object is recorded. NEVER fetch run_artifacts_storage_prefix.
    if meta.get("requires_app_install") is not False and meta.get("app_storage_path"):
        app_dest = os.path.join(folder, _APP_NAME[platform])
        download_object(bucket, meta["app_storage_path"], app_dest, token, **dl_kwargs)


def main(argv=None):
    ap = argparse.ArgumentParser(description="Fetch corpus artifacts from GCS on the host.")
    ap.add_argument("--key", required=True, help="service-account key JSON path")
    ap.add_argument("--bucket", required=True, help="GCS bucket name")
    ap.add_argument("folders", nargs="+", help="corpus folder paths (each holds metadata.json)")
    args = ap.parse_args(argv)

    try:
        token = mint_token(args.key)          # once per host; reused for all folders
    except Exception as e:
        print(f"[host_fetch] FATAL: token mint failed: {e}", file=sys.stderr)
        return 1

    # Zero-arg re-mint, threaded down so a 401 (token outlived by a long stage)
    # is recovered in place instead of failing the folder.
    token_provider = lambda: mint_token(args.key)

    failed = []
    for folder in args.folders:
        try:
            fetch_folder(folder, args.bucket, token, token_provider=token_provider)
        except Exception as e:
            # A single flow's fetch failure must not abort the others.
            print(f"[host_fetch] WARN: fetch failed for {folder}: {e}", file=sys.stderr)
            failed.append(folder)
    if failed:
        print(f"[host_fetch] {len(failed)} folder(s) failed; the rest fetched OK",
              file=sys.stderr)
        if len(failed) == len(args.folders):
            # Every folder missed — nothing to diff. Distinct from a partial
            # miss (still 0) so dispatch_host marks the host fetch-failed
            # instead of running the diff against zero inputs.
            return 1
    return 0                                   # partial or full success: run_differential reports the misses


if __name__ == "__main__":
    raise SystemExit(main())
