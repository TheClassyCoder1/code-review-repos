export interface Page<T> {
  items: T[];
  nextCursor: string | null;
}

interface CursorPayload {
  /** Sort key of the last item on the previous page. */
  k: string;
  /** Tie-breaker id of the last item, so equal sort keys never repeat or skip rows. */
  id: string;
}

export function encodeCursor(sortKey: string, id: string): string {
  const payload: CursorPayload = { k: sortKey, id };
  return Buffer.from(JSON.stringify(payload), 'utf8').toString('base64url');
}

export function decodeCursor(cursor: string): CursorPayload {
  let parsed: unknown;
  try {
    parsed = JSON.parse(Buffer.from(cursor, 'base64url').toString('utf8'));
  } catch {
    throw new RangeError('malformed pagination cursor');
  }
  if (
    typeof parsed !== 'object' ||
    parsed === null ||
    typeof (parsed as CursorPayload).k !== 'string' ||
    typeof (parsed as CursorPayload).id !== 'string'
  ) {
    throw new RangeError('malformed pagination cursor');
  }
  return parsed as CursorPayload;
}

/**
 * Keyset pagination over an in-memory collection ordered by (sortKey, id).
 * Rows inserted behind the cursor between calls are not revisited, and rows
 * sharing a sort key are split across pages without duplication.
 */
export function paginateByKey<T>(
  rows: readonly T[],
  sortKey: (row: T) => string,
  idOf: (row: T) => string,
  limit: number,
  cursor?: string | null,
): Page<T> {
  if (!Number.isInteger(limit) || limit < 1 || limit > 500) {
    throw new RangeError('limit must be between 1 and 500');
  }
  const after = cursor ? decodeCursor(cursor) : null;
  const ordered = [...rows].sort(
    (a, b) => sortKey(a).localeCompare(sortKey(b)) || idOf(a).localeCompare(idOf(b)),
  );
  const remaining = after
    ? ordered.filter((row) => {
        const byKey = sortKey(row).localeCompare(after.k);
        return byKey > 0 || (byKey === 0 && idOf(row).localeCompare(after.id) > 0);
      })
    : ordered;

  const items = remaining.slice(0, limit);
  const last = items[items.length - 1];
  const nextCursor = remaining.length > limit && last ? encodeCursor(sortKey(last), idOf(last)) : null;
  return { items, nextCursor };
}
