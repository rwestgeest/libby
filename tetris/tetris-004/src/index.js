import { Game } from './game.js';
import { TerminalRenderer } from './render.js';

// The interactive loop and terminal lifecycle are connected in the next task.
new TerminalRenderer().render(new Game());
