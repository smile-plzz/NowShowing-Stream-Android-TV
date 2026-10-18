"""Package the checked APKs with version, checksum, and public signing metadata."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess


def read_version(text):
    name = re.search(r'versionName\s*=\s*"([^"]+)"', text)
    code = re.search(r'versionCode\s*=\s*(\d+)', text)
    if not name or not code or not re.fullmatch(r"\d+\.\d+\.\d+(?:-[A-Za-z0-9.-]+)?", name[1]):
        raise ValueError("Missing or invalid Android version")
    return name[1], int(code[1])


def verify_bundle(directory, expected_commit=None):
    """Refuse incomplete, mixed-version or modified downloads before publication."""
    directory = Path(directory)
    info = json.loads((directory / "build-info.json").read_text())
    version, code = read_version(f'versionName = "{info["version"]}"\nversionCode = {info["versionCode"]}')
    if code <= 0 or not re.fullmatch(r"[0-9a-f]{40}", info["commit"]):
        raise ValueError("Invalid build identity")
    if expected_commit and info["commit"] != expected_commit:
        raise ValueError("Artifact commit differs from the publishing checkout")
    if info["signingMode"] not in {"development", "permanent"}:
        raise ValueError("Unknown signing mode")
    if (info["signingMode"] != "permanent" or "-" in version) and info["prerelease"] is not True:
        raise ValueError("Candidate/development signing requires prerelease status")
    expected = {f"NowShowing-TV-{version}-{kind}.apk" for kind in ("release", "debug")}
    checksums = {}
    for line in (directory / "SHA256SUMS.txt").read_text().splitlines():
        digest, name = line.split("  ", 1)
        if name not in expected or name in checksums or not re.fullmatch(r"[0-9a-f]{64}", digest):
            raise ValueError("Unexpected checksum entry")
        checksums[name] = digest
    if set(checksums) != expected:
        raise ValueError("Both APK checksums are required")
    certificates = (directory / "SIGNING-CERTIFICATES.txt").read_text()
    for name, digest in checksums.items():
        if hashlib.sha256((directory / name).read_bytes()).hexdigest() != digest:
            raise ValueError("APK checksum mismatch")
        if name not in certificates:
            raise ValueError("Missing public signing report")
    return info


def main():
    version, code = read_version(Path("app/build.gradle.kts").read_text())
    mode = os.environ.get("SIGNING_MODE", "development")
    if mode not in {"development", "permanent"}:
        raise ValueError("Unknown signing mode")
    out = Path("dist")
    out.mkdir(exist_ok=True)
    sdk = Path(os.environ.get("ANDROID_HOME", os.environ.get("ANDROID_SDK_ROOT", "")))
    signers = sorted(sdk.glob("build-tools/*/apksigner"))
    if not signers:
        raise RuntimeError("Android apksigner is required")
    reports = []
    checksums = []
    for kind in ("release", "debug"):
        source = Path(f"app/build/outputs/apk/{kind}/app-{kind}.apk")
        dest = out / f"NowShowing-TV-{version}-{kind}.apk"
        shutil.copyfile(source, dest)
        result = subprocess.run([str(signers[-1]), "verify", "--verbose", "--print-certs", str(dest)],
                                check=True, capture_output=True, text=True)
        reports.append(f"## {dest.name}\n{result.stdout}")
        checksums.append(f"{hashlib.sha256(dest.read_bytes()).hexdigest()}  {dest.name}\n")
    (out / "SHA256SUMS.txt").write_text("".join(checksums))
    (out / "SIGNING-CERTIFICATES.txt").write_text("\n".join(reports))
    (out / "build-info.json").write_text(json.dumps({
        "version": version, "versionCode": code, "commit": os.environ["GITHUB_SHA"],
        "signingMode": mode, "run": os.environ.get("GITHUB_RUN_ID"),
        "prerelease": "-" in version or mode != "permanent"
    }, indent=2) + "\n")
    verify_bundle(out, os.environ["GITHUB_SHA"])


if __name__ == "__main__":
    main()
