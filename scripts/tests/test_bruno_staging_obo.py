import base64
import importlib.util
import json
from pathlib import Path
import unittest
from unittest.mock import patch


path = Path(__file__).resolve().parents[1] / "bruno-staging-obo.py"
spec = importlib.util.spec_from_file_location("bruno_staging_obo", path)
launcher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(launcher)


def secret_data(token_url, scope="api://example/DataStore.Access"):
    return json.dumps({
        "data": {
            "DATASTORE_TOKEN_URI": base64.b64encode(token_url.encode()).decode(),
            "DATASTORE_OAUTH_SCOPES": base64.b64encode(scope.encode()).decode(),
        }
    })


class StagingBrunoLauncherTest(unittest.TestCase):
    @patch.object(launcher.os, "execvpe")
    @patch.object(launcher.subprocess, "run")
    def test_passes_staging_configuration_to_op(self, run, execute):
        run.return_value.stdout = secret_data(
            "https://login.microsoftonline.com/12345678-1234-1234-1234-123456789abc"
            "/oauth2/v2.0/token"
        )

        launcher.main()

        executable, args, env = execute.call_args.args
        self.assertEqual(executable, "op")
        self.assertEqual(args[1], "run")
        self.assertTrue(env["STAGING_ENTRA_AUTHORIZE_URL"].endswith("/oauth2/v2.0/authorize"))
        self.assertEqual(env["STAGING_DATASTORE_SCOPE"], "api://example/DataStore.Access")

    @patch.object(launcher.os, "execvpe")
    @patch.object(launcher.subprocess, "run")
    def test_rejects_untrusted_token_endpoint_and_empty_scope(self, run, execute):
        for token_url, scope in (
            ("https://example.com/12345678-1234-1234-1234-123456789abc/oauth2/v2.0/token", "scope"),
            ("https://login.microsoftonline.com/12345678-1234-1234-1234-123456789abc/oauth2/v2.0/token", ""),
        ):
            with self.subTest(token_url=token_url, scope=scope):
                run.return_value.stdout = secret_data(token_url, scope)
                with self.assertRaises(ValueError):
                    launcher.main()
        execute.assert_not_called()


if __name__ == "__main__":
    unittest.main()
