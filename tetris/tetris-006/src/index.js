import { Game } from './game.js';

// The terminal input and bounded renderer are the next planned task.
const game = new Game();
console.log(`Tetris engine ready (${game.board[0].length}x${game.board.length}). Terminal interface not implemented yet.`);
