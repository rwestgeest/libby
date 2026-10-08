import { Game } from './engine.js';

// Terminal rendering and input will be added in the next plan task.
const game = new Game();
console.log(`Tetris engine ready: ${game.board[0].length}×${game.board.length} board.`);
console.log('Interactive terminal play is not implemented yet.');
