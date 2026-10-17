import unittest
from package_release import read_version


class VersionTests(unittest.TestCase):
    def test_candidate_version_and_android_code(self):
        self.assertEqual(("1.5.0-rc.1", 15001), read_version('versionName = "1.5.0-rc.1"\nversionCode = 15001'))

    def test_missing_or_unsafe_versions_are_rejected(self):
        for text in ('versionName = "oops"', 'versionCode = 1', 'versionName = "../bad"\nversionCode = 1'):
            with self.assertRaises(ValueError):
                read_version(text)


if __name__ == "__main__":
    unittest.main()
