import base64, json
import http.client
import io
import os
import shutil
import subprocess
import urllib.error
import zipfile

import pytest

import host_fetch as hf


def _b64url_decode(s):
    return base64.urlsafe_b64decode(s + "=" * (-len(s) % 4))


def test_signing_input_has_expected_header_and_claims():
    si = hf.build_signing_input("svc@example.iam.gserviceaccount.com", now=1000)
    header_b64, claims_b64 = si.split(".")
    header = json.loads(_b64url_decode(header_b64))
    claims = json.loads(_b64url_decode(claims_b64))
    assert header == {"alg": "RS256", "typ": "JWT"}
    assert claims["iss"] == "svc@example.iam.gserviceaccount.com"
    assert claims["aud"] == "https://oauth2.googleapis.com/token"
    assert claims["scope"] == "https://www.googleapis.com/auth/devstorage.read_only"
    assert claims["iat"] == 1000 and claims["exp"] == 1000 + 3600
    # base64url: no '+' '/' or '=' padding
    assert "=" not in si and "+" not in si and "/" not in si


def test_sign_rs256_invokes_openssl_and_b64url_encodes(monkeypatch):
    calls = {}

    class R:
        returncode = 0
        stdout = b"\xDE\xAD\xBE\xEF"
        stderr = b""

    def fake_runner(argv, input=None, capture_output=False, check=False):
        calls["argv"] = argv
        calls["input"] = input
        return R()

    sig = hf.sign_rs256("aaa.bbb", "-----BEGIN PRIVATE KEY-----\nx\n-----END PRIVATE KEY-----\n",
                        runner=fake_runner)
    assert calls["argv"][:4] == ["openssl", "dgst", "-sha256", "-sign"]
    assert calls["input"] == b"aaa.bbb"
    assert sig == base64.urlsafe_b64encode(b"\xDE\xAD\xBE\xEF").rstrip(b"=").decode()


def test_sign_rs256_raises_without_echoing_key_on_failure():
    class R:
        returncode = 1
        stdout = b""
        stderr = b"some openssl noise"

    def failing_runner(argv, input=None, capture_output=False, check=False):
        return R()

    try:
        hf.sign_rs256("a.b", "SECRET-KEY-MATERIAL", runner=failing_runner)
        assert False, "expected RuntimeError"
    except RuntimeError as e:
        assert "SECRET-KEY-MATERIAL" not in str(e)


def test_mint_token_posts_jwt_bearer_and_returns_access_token(tmp_path, monkeypatch):
    key = {"client_email": "svc@example.iam.gserviceaccount.com",
           "private_key": "-----BEGIN PRIVATE KEY-----\nx\n-----END PRIVATE KEY-----\n"}
    kp = tmp_path / "creds.json"
    kp.write_text(json.dumps(key))

    seen = {}

    class Resp:
        def read(self): return json.dumps({"access_token": "ya29.TESTTOKEN"}).encode()
        def __enter__(self): return self
        def __exit__(self, *a): return False

    def fake_urlopen(req, timeout=None):
        seen["url"] = req.full_url
        seen["body"] = req.data.decode()
        return Resp()

    def fake_runner(argv, input=None, capture_output=False, check=False):
        class R: returncode = 0; stdout = b"sig"; stderr = b""
        return R()

    tok = hf.mint_token(str(kp), urlopen=fake_urlopen, runner=fake_runner, now=1000)
    assert tok == "ya29.TESTTOKEN"
    assert seen["url"] == "https://oauth2.googleapis.com/token"
    assert "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer" in seen["body"]
    assert "assertion=" in seen["body"]


class _FakeResp(io.BytesIO):
    def __init__(self, data, content_length=None):
        super().__init__(data)
        self.headers = {"Content-Length": str(content_length)} if content_length is not None else {}
    def __enter__(self): return self
    def __exit__(self, *a): return False


def test_object_media_url_percent_encodes_slashes():
    url = hf.object_media_url("test-bucket", "prod/apk/03d941bab8db")
    assert url == ("https://storage.googleapis.com/download/storage/v1/b/"
                   "test-bucket/o/prod%2Fapk%2F03d941bab8db?alt=media")


def test_download_object_writes_dest_and_returns_count(tmp_path):
    dest = str(tmp_path / "app.apk")
    def urlopen(req, timeout=None):
        assert req.headers["Authorization"] == "Bearer TOK"
        return _FakeResp(b"APKDATA", content_length=7)
    n = hf.download_object("test-bucket", "prod/apk/x", dest, "TOK", urlopen=urlopen)
    assert n == 7
    assert open(dest, "rb").read() == b"APKDATA"


def test_download_object_rejects_truncation_and_leaves_no_partial(tmp_path):
    dest = str(tmp_path / "app.apk")
    def urlopen(req, timeout=None):
        return _FakeResp(b"SHORT", content_length=999)   # claims 999, sends 5
    try:
        hf.download_object("test-bucket", "prod/apk/x", dest, "TOK", urlopen=urlopen, retries=1)
        assert False, "expected truncation error"
    except (IOError, RuntimeError):
        pass
    assert not os.path.exists(dest)
    assert not os.path.exists(dest + ".part")


def test_download_object_retries_5xx_then_succeeds(tmp_path):
    dest = str(tmp_path / "app.apk")
    calls = {"n": 0}
    def urlopen(req, timeout=None):
        calls["n"] += 1
        if calls["n"] == 1:
            raise urllib.error.HTTPError(req.full_url, 503, "busy", {}, None)
        return _FakeResp(b"OK", content_length=2)
    n = hf.download_object("test-bucket", "prod/apk/x", dest, "TOK",
                           urlopen=urlopen, retries=3, sleep=lambda s: None)
    assert n == 2 and calls["n"] == 2


def test_download_object_fails_fast_on_404(tmp_path):
    dest = str(tmp_path / "app.apk")
    calls = {"n": 0}
    def urlopen(req, timeout=None):
        calls["n"] += 1
        raise urllib.error.HTTPError(req.full_url, 404, "not found", {}, None)
    def token_provider():
        raise AssertionError("404 is not a token problem — must not re-mint")
    try:
        hf.download_object("test-bucket", "prod/apk/x", dest, "TOK",
                           urlopen=urlopen, retries=5, sleep=lambda s: None,
                           token_provider=token_provider)
        assert False, "expected immediate failure"
    except urllib.error.HTTPError as e:
        assert e.code == 404
    assert calls["n"] == 1                      # no retry on a 4xx


def test_download_object_refreshes_token_once_on_401_then_succeeds(tmp_path):
    dest = str(tmp_path / "app.apk")
    calls = {"get": 0}
    refreshes = {"n": 0}

    def urlopen(req, timeout=None):
        calls["get"] += 1
        if calls["get"] == 1:
            assert req.headers["Authorization"] == "Bearer STALE"
            raise urllib.error.HTTPError(req.full_url, 401, "expired token", {}, None)
        assert req.headers["Authorization"] == "Bearer FRESH"
        return _FakeResp(b"OK", content_length=2)

    def token_provider():
        refreshes["n"] += 1
        return "FRESH"

    n = hf.download_object("test-bucket", "prod/apk/x", dest, "STALE",
                           urlopen=urlopen, retries=3, sleep=lambda s: None,
                           token_provider=token_provider)
    assert n == 2
    assert open(dest, "rb").read() == b"OK"
    assert refreshes["n"] == 1                  # re-minted exactly once
    assert calls["get"] == 2                    # failed GET, then the retry
    assert not os.path.exists(dest + ".part")


def test_download_object_retries_incomplete_read_then_succeeds(tmp_path):
    dest = str(tmp_path / "app.apk")
    calls = {"n": 0}
    def urlopen(req, timeout=None):
        calls["n"] += 1
        if calls["n"] == 1:
            raise http.client.IncompleteRead(b"partial")
        return _FakeResp(b"OK", content_length=2)
    n = hf.download_object("test-bucket", "prod/apk/x", dest, "TOK",
                           urlopen=urlopen, retries=3, sleep=lambda s: None)
    assert n == 2 and calls["n"] == 2
    assert not os.path.exists(dest + ".part")


def _write_meta(folder, **over):
    os.makedirs(folder, exist_ok=True)
    meta = {"platform": "ANDROID", "flow_file_path": "flow.yaml",
            "app_storage_path": "prod/apk/APPHASH",
            "workspace_storage_path": "prod/workspaces/WS",
            "run_artifacts_storage_prefix": "prod/run_artifacts/run_x/"}
    meta.update(over)
    with open(os.path.join(folder, "metadata.json"), "w") as fh:
        json.dump(meta, fh)


def _zip_bytes(names):
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w") as z:
        for n in names:
            z.writestr(n, "x")
    return buf.getvalue()


def test_fetch_folder_downloads_app_and_workspace_never_run_artifacts(tmp_path):
    folder = str(tmp_path / "corpus" / "0" / "run_1")
    _write_meta(folder)
    ws_zip = _zip_bytes(["flow.yaml"])
    requested = []
    def fake_download(bucket, object_path, dest, token, urlopen=None):
        requested.append(object_path)
        if object_path.endswith("WS"):
            with open(dest, "wb") as fh: fh.write(ws_zip)
        else:
            with open(dest, "wb") as fh: fh.write(b"APK")
        return 3
    import host_fetch as hf
    orig = hf.download_object
    hf.download_object = fake_download
    try:
        hf.fetch_folder(folder, "test-bucket", "TOK")
    finally:
        hf.download_object = orig
    assert os.path.exists(os.path.join(folder, "app.apk"))
    assert os.path.exists(os.path.join(folder, "workspace.zip"))
    assert os.path.isfile(os.path.join(folder, "workspace", "flow.yaml"))
    assert "prod/apk/APPHASH" in requested and "prod/workspaces/WS" in requested
    assert not any("run_artifacts" in p for p in requested)


def test_fetch_folder_ios_names_ipa(tmp_path):
    folder = str(tmp_path / "run_ios")
    _write_meta(folder, platform="IOS")
    ws_zip = _zip_bytes(["flow.yaml"])
    def fake_download(bucket, object_path, dest, token, urlopen=None):
        with open(dest, "wb") as fh:
            fh.write(ws_zip if object_path.endswith("WS") else b"IPA")
        return 3
    import host_fetch as hf
    orig = hf.download_object; hf.download_object = fake_download
    try:
        hf.fetch_folder(folder, "test-bucket", "TOK")
    finally:
        hf.download_object = orig
    assert os.path.exists(os.path.join(folder, "app.ipa"))
    assert not os.path.exists(os.path.join(folder, "app.apk"))


def test_fetch_folder_skips_app_when_requires_app_install_false(tmp_path):
    folder = str(tmp_path / "run_builtin")
    _write_meta(folder, requires_app_install=False)
    ws_zip = _zip_bytes(["flow.yaml"])
    requested = []
    def fake_download(bucket, object_path, dest, token, urlopen=None):
        requested.append(object_path)
        with open(dest, "wb") as fh: fh.write(ws_zip)
        return 3
    import host_fetch as hf
    orig = hf.download_object; hf.download_object = fake_download
    try:
        hf.fetch_folder(folder, "test-bucket", "TOK")
    finally:
        hf.download_object = orig
    assert requested == ["prod/workspaces/WS"]           # workspace only, no app
    assert not os.path.exists(os.path.join(folder, "app.apk"))


def test_main_returns_nonzero_when_all_folders_fail(tmp_path, monkeypatch):
    monkeypatch.setattr(hf, "mint_token", lambda key_path: "TOK")
    def fail_fetch(folder, bucket, token, urlopen=None, token_provider=None):
        raise RuntimeError("boom")
    monkeypatch.setattr(hf, "fetch_folder", fail_fetch)
    key = tmp_path / "key.json"
    key.write_text("{}")
    rc = hf.main(["--key", str(key), "--bucket", "test-bucket", "f1", "f2"])
    assert rc != 0


def test_main_returns_zero_on_partial_fail(tmp_path, monkeypatch):
    monkeypatch.setattr(hf, "mint_token", lambda key_path: "TOK")
    def sometimes_fail(folder, bucket, token, urlopen=None, token_provider=None):
        if folder == "f1":
            raise RuntimeError("boom")
    monkeypatch.setattr(hf, "fetch_folder", sometimes_fail)
    key = tmp_path / "key.json"
    key.write_text("{}")
    rc = hf.main(["--key", str(key), "--bucket", "test-bucket", "f1", "f2"])
    assert rc == 0


def test_sign_rs256_real_openssl_roundtrip(tmp_path):
    # Real subprocess.run (no fake runner) against an ephemeral, throwaway key
    # generated in a tmp dir — never the real SA key. Catches flag/encoding
    # regressions (e.g. wrong digest, PKCS1 vs raw) that a mocked runner can't.
    if shutil.which("openssl") is None:
        pytest.skip("openssl not on PATH")

    keyfile = tmp_path / "throwaway.pem"
    pubfile = tmp_path / "throwaway_pub.pem"
    subprocess.run(["openssl", "genrsa", "-out", str(keyfile), "2048"],
                   check=True, capture_output=True)
    subprocess.run(["openssl", "rsa", "-in", str(keyfile), "-pubout", "-out", str(pubfile)],
                   check=True, capture_output=True)

    signing_input = "aaa.bbb"
    sig_b64url = hf.sign_rs256(signing_input, keyfile.read_text())   # real runner

    sigfile = tmp_path / "sig.bin"
    sigfile.write_bytes(_b64url_decode(sig_b64url))
    verify = subprocess.run(
        ["openssl", "dgst", "-sha256", "-verify", str(pubfile), "-signature", str(sigfile)],
        input=signing_input.encode(), capture_output=True,
    )
    assert verify.returncode == 0, verify.stderr
    assert b"Verified OK" in verify.stdout
