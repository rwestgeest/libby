import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import test from 'node:test';

const projectRoot = new URL('../', import.meta.url);

test('the application entry point launches successfully', () => {
  const result = spawnSync(process.execPath, ['src/index.js'], {
    cwd: projectRoot,
    encoding: 'utf8',
    timeout: 5000,
  });

  assert.ifError(result.error);
  assert.equal(result.status, 0, result.stderr);
  assert.equal(result.stderr, '');
  assert.match(result.stdout, /Terminal Tetris/);
});
