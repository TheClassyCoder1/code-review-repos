const FORMULA_PREFIXES = ['=', '+', '-', '@', '\t', '\r'];

export function neutralizeFormula(cell: string): string {
  if (cell.length > 0 && FORMULA_PREFIXES.includes(cell[0])) {
    return `'${cell}`;
  }
  return cell;
}
