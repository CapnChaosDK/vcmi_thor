"""Validate and bind the human-authored Thor checklist to an exact CI candidate."""
import argparse
import hashlib
import json
import re
from pathlib import Path


def validate(plan):
    if type(plan.get('schema_version')) is not int or plan['schema_version'] != 1:
        raise ValueError('Unsupported checklist schema')

    def text(obj, key, limit):
        value = obj.get(key)
        if not isinstance(value, str) or not value.strip() or len(value) > limit:
            raise ValueError(f'Invalid {key}')
        return value

    if not re.fullmatch(r'[a-z0-9-]+', text(plan, 'feature_id', 80)):
        raise ValueError('Invalid feature ID')
    text(plan, 'title', 160)
    text(plan, 'summary', 4000)
    text(plan, 'preparation', 4000)
    if not isinstance(plan.get('limitations', ''), str) or len(plan.get('limitations', '')) > 4000:
        raise ValueError('Invalid limitations')
    checks = plan.get('checks')
    if not isinstance(checks, list) or not 1 <= len(checks) <= 50:
        raise ValueError('Checklist must contain 1–50 checks')
    identities = set()
    for check in checks:
        if not isinstance(check, dict):
            raise ValueError('Invalid check')
        identity = text(check, 'id', 80)
        if not re.fullmatch(r'[a-z0-9-]+', identity) or identity in identities:
            raise ValueError('Duplicate or invalid check ID')
        identities.add(identity)
        text(check, 'title', 160)
        text(check, 'steps', 4000)
        text(check, 'expected', 4000)
    if 'candidate_commit' in plan or 'apk_sha256' in plan:
        raise ValueError('Build identity must be bound by CI, not authored in the source plan')
    return plan


def package(source, receipt_path, output):
    plan = validate(json.loads(source.read_text(encoding='utf-8')))
    receipt = json.loads(receipt_path.read_text(encoding='utf-8'))
    if not re.fullmatch('[0-9a-f]{40}', receipt['candidate_commit']):
        raise ValueError('Invalid candidate commit')
    if not re.fullmatch('[0-9a-f]{64}', receipt['apk_sha256']):
        raise ValueError('Invalid APK checksum')
    if receipt['package_id'] != 'is.xyz.vcmi.thor':
        raise ValueError('Incorrect package')
    plan.update(candidate_commit=receipt['candidate_commit'], apk_sha256=receipt['apk_sha256'])
    data = (json.dumps(plan, ensure_ascii=False, indent=2) + '\n').encode('utf-8')
    if len(data) > 256 * 1024:
        raise ValueError('Checklist exceeds artifact metadata limit')
    output.write_bytes(data)
    receipt['validation_plan_sha256'] = hashlib.sha256(data).hexdigest()
    receipt_path.write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', type=Path, default=Path('CI/thor-validation-plan.json'))
    parser.add_argument('--receipt', type=Path)
    parser.add_argument('--output', type=Path, default=Path('thor-validation-plan.json'))
    args = parser.parse_args()
    if args.receipt:
        package(args.source, args.receipt, args.output)
    else:
        validate(json.loads(args.source.read_text(encoding='utf-8')))
        print('Thor validation plan is valid')
