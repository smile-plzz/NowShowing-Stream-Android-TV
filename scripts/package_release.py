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


if __name__ == "__main__":
    main()
