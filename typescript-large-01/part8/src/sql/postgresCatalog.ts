export interface TablePath {
  databaseName: string;
  schemaName: string;
  tableName: string;
}

export interface Column {
  name: string;
  dataType: string;
  nullable: boolean;
  comment: string | null;
}

export interface SqlConnection {
  execute(sql: string): Promise<void>;
}

/** Creates sink tables in PostgreSQL from an upstream catalog definition. */
export class PostgresCatalog {
  constructor(private readonly connection: SqlConnection) {}

  async createTable(tablePath: TablePath, columns: Column[]): Promise<void> {
    await this.connection.execute(this.buildCreateTableSql(tablePath, columns));
    for (const column of columns) {
      if (column.comment) {
        await this.connection.execute(this.buildColumnCommentSql(tablePath, column));
      }
    }
  }

  private buildCreateTableSql(tablePath: TablePath, columns: Column[]): string {
    const defs = columns.map((c) => `${this.quoteIdentifier(c.name)} ${c.dataType}${c.nullable ? '' : ' NOT NULL'}`);
    return `CREATE TABLE ${this.tableIdentifier(tablePath)} (${defs.join(', ')})`;
  }

  private buildColumnCommentSql(tablePath: TablePath, column: Column): string {
    return `COMMENT ON COLUMN ${this.tableIdentifier(tablePath)}.${this.quoteIdentifier(column.name)} IS '${column.comment}'`;
  }

  private tableIdentifier(tablePath: TablePath): string {
    return `${this.quoteIdentifier(tablePath.schemaName)}.${this.quoteIdentifier(tablePath.tableName)}`;
  }

  private quoteIdentifier(identifier: string): string {
    return `"${identifier.replace(/"/g, '""')}"`;
  }
}
