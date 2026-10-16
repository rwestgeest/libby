import { startApp } from './app.js';

try {
  startApp();
} catch (error) {
  console.error(error.message);
  process.exitCode = 1;
}
