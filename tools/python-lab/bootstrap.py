"""Trusted entry point; Windows AppContainer is the security boundary."""
import io
import json
import os
import sys
import traceback

root = os.getcwd()
with open('request.json', encoding='utf-8-sig') as f:
    request = json.load(f)

class LimitedOutput(io.TextIOBase):
    def __init__(self):
        self.parts = []
        self.size = 0
    def write(self, text):
        self.size += len(text.encode('utf-8'))
        if self.size > 65536:
            raise RuntimeError('Output limit exceeded: 64 KiB')
        self.parts.append(text)
        return len(text)
    def flush(self): pass

output = LimitedOutput()
sys.stdout = sys.stderr = output
sys.stdin = io.StringIO(request.get('stdin', ''))
scope = {'__name__': '__main__', '__file__': os.path.join(root, 'main.py')}
result = {'status': 'success', 'output': '', 'error': '', 'tests': []}
try:
    exec(compile(request['source'], 'main.py', 'exec'), scope)
    for test in request.get('tests', []):
        try:
            exec(compile(test['code'], '<exercise test>', 'exec'), scope)
            result['tests'].append({'name': test['name'], 'passed': True, 'message': ''})
        except BaseException as exc:
            result['tests'].append({'name': test['name'], 'passed': False, 'message': str(exc) or type(exc).__name__})
    if any(not t['passed'] for t in result['tests']): result['status'] = 'test_failure'
except BaseException as exc:
    result['status'] = 'syntax_error' if isinstance(exc, SyntaxError) else 'runtime_error'
    result['error'] = ''.join(traceback.format_exception(type(exc), exc, exc.__traceback__))[-16000:]
result['output'] = ''.join(output.parts)
with open(os.path.join(root, 'result.json'), 'w', encoding='utf-8') as f:
    json.dump(result, f, ensure_ascii=False)
