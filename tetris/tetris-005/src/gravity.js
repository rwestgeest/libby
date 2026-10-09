import { GRAVITY_INTERVAL_MS } from './game.js';

// Return a cleanup function so the terminal controller can stop on exit/restart.
export function startGravity(game, {
  onUpdate = () => {},
  now = () => performance.now(),
  schedule = setInterval,
  cancel = clearInterval,
} = {}) {
  let timer;
  let stopped = game.gameOver;
  let previous = now();
  const stop = () => {
    if (stopped) return;
    stopped = true;
    cancel(timer);
  };
  if (!stopped) {
    timer = schedule(() => {
      if (stopped) return;
      const current = now();
      const changed = game.advance(Math.max(0, current - previous));
      previous = current;
      if (game.gameOver) stop();
      if (changed) onUpdate(game);
    }, GRAVITY_INTERVAL_MS);
  }
  return stop;
}
