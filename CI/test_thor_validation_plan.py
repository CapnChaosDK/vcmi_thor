import copy
import hashlib
import json
import tempfile
import unittest
from pathlib import Path
from thor_validation_plan import validate, package


class PlanTests(unittest.TestCase):
    def setUp(self):
        self.plan = json.loads((Path(__file__).parent / 'thor-validation-plan.json').read_text(encoding='utf-8'))

    def test_valid(self):
        validate(self.plan)

    def test_reject_invalid_plans(self):
        for change in [lambda p: p.update(schema_version=2), lambda p: p.update(checks=[]),
                       lambda p: p['checks'].append(copy.deepcopy(p['checks'][0])),
                       lambda p: p.update(candidate_commit='a'*40),
                       lambda p: p['checks'][0].update(expected=''),
                       lambda p: p['checks'][0].update(steps='x'*4001)]:
            with self.subTest(change=change):
                plan = copy.deepcopy(self.plan)
                change(plan)
                with self.assertRaises(ValueError):
                    validate(plan)

    def test_package_binds_exact_build_and_hash(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source, receipt, output = root/'source.json', root/'receipt.json', root/'plan.json'
            source.write_text(json.dumps(self.plan), encoding='utf-8')
            receipt.write_text(json.dumps(dict(candidate_commit='a'*40, apk_sha256='b'*64, package_id='is.xyz.vcmi.thor')), encoding='utf-8')
            package(source, receipt, output)
            bound = json.loads(output.read_text(encoding='utf-8'))
            self.assertEqual(bound['candidate_commit'], 'a'*40)
            self.assertEqual(bound['apk_sha256'], 'b'*64)
            self.assertEqual(json.loads(receipt.read_text())['validation_plan_sha256'], hashlib.sha256(output.read_bytes()).hexdigest())
            self.assertNotIn('candidate_commit', json.loads(source.read_text()))


if __name__ == '__main__':
    unittest.main()
