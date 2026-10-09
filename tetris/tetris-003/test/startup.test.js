import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';

test('the entry point starts successfully', () => {
  const entryPoint = new URL('../src/index.js', import.meta.url);
  const result = spawnSync(process.execPath, [fileURLToPath(entryPoint)], {
    encoding: 'utf8',
    timeout: 5000,
  });

  assert.ifError(result.error);
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /Terminal Tetris/);
});
