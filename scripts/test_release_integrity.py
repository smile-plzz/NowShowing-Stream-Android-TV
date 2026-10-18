import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from package_release import verify_bundle


class BundleTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.info = {"version": "1.5.0", "versionCode": 15003, "commit": "1" * 40,
                     "signingMode": "development", "prerelease": True}
        self.save_info()
        self.names = [f"NowShowing-TV-1.5.0-{kind}.apk" for kind in ("release", "debug")]
        lines = []
        for name in self.names:
            data = ("test-apk-" + name).encode()
            (self.root / name).write_bytes(data)
            lines.append(f"{hashlib.sha256(data).hexdigest()}  {name}\n")
        (self.root / "SHA256SUMS.txt").write_text("".join(lines))
        (self.root / "SIGNING-CERTIFICATES.txt").write_text("\n".join(self.names))

    def save_info(self):
        (self.root / "build-info.json").write_text(json.dumps(self.info))

    def test_complete_bundle_is_accepted(self):
        self.assertEqual(self.info, verify_bundle(self.root, "1" * 40))

    def test_modified_apk_is_rejected(self):
        (self.root / self.names[0]).write_bytes(b"modified")
        with self.assertRaises(ValueError):
            verify_bundle(self.root)

    def test_missing_variant_is_rejected(self):
        path = self.root / "SHA256SUMS.txt"
        path.write_text(path.read_text().splitlines()[0] + "\n")
        with self.assertRaises(ValueError):
            verify_bundle(self.root)

    def test_wrong_commit_is_rejected(self):
        with self.assertRaises(ValueError):
            verify_bundle(self.root, "2" * 40)

    def test_development_build_cannot_claim_stable(self):
        self.info["prerelease"] = False
        self.save_info()
        with self.assertRaises(ValueError):
            verify_bundle(self.root)

    def test_missing_certificate_report_is_rejected(self):
        (self.root / "SIGNING-CERTIFICATES.txt").write_text(self.names[0])
        with self.assertRaises(ValueError):
            verify_bundle(self.root)
