import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

export interface FormularyEntry {
  ndc: string;
  name: string;
  tier: number;
  requiresPriorAuth: boolean;
}

@Injectable()
export class FormularyService {
  private readonly logger = new Logger(FormularyService.name);
  private entries = new Map<string, FormularyEntry>();

  constructor(private readonly config: ConfigService) {}

  async loadFormulary(): Promise<number> {
    const path = resolve(this.config.get<string>('pharmacy.formularyPath'));
    const raw = readFileSync(path, 'utf8');
    const parsed = JSON.parse(raw) as FormularyEntry[];
    this.entries = new Map(parsed.filter((entry) => entry.ndc).map((entry) => [entry.ndc, entry]));
    this.logger.log(`Loaded ${this.entries.size} formulary entries`);
    return this.entries.size;
  }

  lookup(ndc: string): FormularyEntry | undefined {
    return this.entries.get(ndc);
  }

  isCovered(ndc: string): boolean {
    const entry = this.entries.get(ndc);
    return entry !== undefined && !entry.requiresPriorAuth;
  }
}
