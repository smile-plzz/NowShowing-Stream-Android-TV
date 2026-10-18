"""Create a draft with all assets, then publish; never overwrite an existing release."""
import json
import os
from package_release import read_version, verify_bundle
from pathlib import Path
import shutil
import subprocess


def gh(*args, **kwargs):
    return subprocess.run(["gh", *args], check=True, text=True, **kwargs)


def main():
    info = verify_bundle("dist", os.environ.get("GITHUB_SHA"))
    version, code = read_version(Path("app/build.gradle.kts").read_text())
    if (info["version"], info["versionCode"]) != (version, code):
        raise ValueError("Bundle version differs from the publishing checkout")
    tag = "v" + info["version"]
    existing = subprocess.run(["gh", "release", "view", tag, "--json", "tagName"],
                              capture_output=True, text=True)
    if existing.returncode == 0:
        raise RuntimeError(f"{tag} already exists. Bump the version; release assets are immutable by policy.")
    notes = Path(f"docs/releases/{tag}.md").read_text()
    notes += "\n\nBuild commit: " + info["commit"] + "\n"
    if info["signingMode"] != "permanent":
        notes += ("\n**Signing status: development-signed sideload candidate.** "
                  "No permanent release key is configured. It may not update an existing installation; "
                  "do not uninstall without considering loss of local history/watchlists. "
                  "See docs/RELEASE_SIGNING.md. This is a prerelease, not a production-signing claim.\n")
    notes_path = Path("dist/release-notes.md")
    notes_path.write_text(notes)
    assets = sorted(str(p) for p in Path("dist").iterdir() if p.is_file() and p != notes_path)
    args = ["release", "create", tag, *assets, "--target", info["commit"], "--draft",
            "--title", f"NowShowing TV {info['version']}", "--notes-file", str(notes_path)]
    if info["prerelease"]:
        args += ["--prerelease"]
    gh(*args)
    gh("release", "edit", tag, "--draft=false")
    print(f"Published {tag} with {len(assets)} verified assets")


if __name__ == "__main__":
    main()
