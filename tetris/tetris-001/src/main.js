import { startTerminal } from './terminal.js';

try {
  startTerminal();
} catch (error) {
  console.error(error.message);
  process.exitCode = 1;
}
