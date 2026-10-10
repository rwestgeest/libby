import { startTerminal } from './terminal.js';

try {
  startTerminal();
} catch (error) {
  console.error(`Tetris: ${error.message}`);
  process.exitCode = 1;
}
