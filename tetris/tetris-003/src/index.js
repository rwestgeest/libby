import { Game } from './game.js';
import { drawFrame } from './render.js';

const game = new Game();
game.spawn();
drawFrame(game);
