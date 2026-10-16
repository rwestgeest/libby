import stringWidth from 'string-width';

const segmenter = new Intl.Segmenter(undefined, { granularity: 'grapheme' });

// Keep messages on one physical row and never emit a partial grapheme.
// Width follows conventional terminal Unicode widths (ambiguous characters narrow).
export function clipColumns(text, columns) {
  const plain = String(text).replace(/[\p{Cc}\p{Zl}\p{Zp}]/gu, ' ');
  let result = '';
  let width = 0;
  for (const { segment } of segmenter.segment(plain)) {
    const size = stringWidth(segment);
    if (width + size > Math.max(0, columns)) break;
    result += segment;
    width += size;
  }
  return result;
}
