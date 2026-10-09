import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const root = new URL('../', import.meta.url);

test('npm start points to a working Node.js entry point', () => {
  const manifest = JSON.parse(readFileSync(new URL('package.json', root), 'utf8'));
  assert.equal(manifest.scripts.start, 'node src/index.js');
  assert.equal(manifest.scripts.test, 'node --test');

  const result = spawnSync(process.execPath, ['src/index.js'], {
    cwd: root,
    encoding: 'utf8',
    timeout: 5000,
  });

  assert.ifError(result.error);
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /Terminal Tetris/);
});
