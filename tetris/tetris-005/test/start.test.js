import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const projectRoot = new URL('../', import.meta.url);

test('the npm start entry point launches successfully', () => {
  const packageJson = JSON.parse(
    readFileSync(new URL('package.json', projectRoot), 'utf8'),
  );
  assert.equal(packageJson.scripts.start, 'node src/index.js');

  const result = spawnSync(process.execPath, ['src/index.js'], {
    cwd: projectRoot,
    encoding: 'utf8',
    timeout: 5000,
  });

  assert.ifError(result.error);
  assert.equal(result.status, 0, result.stderr);
  assert.equal(result.stderr, '');
  assert.match(result.stdout, /Terminal Tetris requires an interactive terminal/);
  assert.match(result.stdout, /Run npm start in a terminal to play/);
  assert.ok(!result.stdout.includes('\x1b'), 'non-interactive launch must not modify terminal settings');
});
