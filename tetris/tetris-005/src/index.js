import { startTerminal } from './terminal.js';

if (process.stdin.isTTY && process.stdout.isTTY) {
  startTerminal();
} else {
  console.log('Terminal Tetris requires an interactive terminal. Run npm start in a terminal to play.');
}
