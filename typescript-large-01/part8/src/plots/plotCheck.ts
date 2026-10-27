import { createHash } from 'crypto';
import { statSync } from 'fs';
import { Logger } from '../util/logger';

const log = new Logger('plot-check');

export interface PlotParam {
  sizeV1: number | null;
}

export interface QualityProtocol {
  getString(): string;
}

export class V2Quality implements QualityProtocol {
  constructor(private readonly partialProof: Buffer, private readonly str: string) {}

  getString(): string {
    return this.str;
  }

  getPartialProof(): Buffer {
    return this.partialProof;
  }
}

export interface Prover {
  getMemo(): Buffer;
  getParam(): PlotParam;
  getId(): Buffer;
  getQualitiesForChallenge(challenge: Buffer): QualityProtocol[];
}

export abstract class V1Prover implements Prover {
  abstract getMemo(): Buffer;
  abstract getParam(): PlotParam;
  abstract getId(): Buffer;
  abstract getQualitiesForChallenge(challenge: Buffer): QualityProtocol[];
  abstract getFullProof(challenge: Buffer, index: number, parallelRead: boolean): Buffer;
}

export abstract class V2Prover implements Prover {
  abstract getMemo(): Buffer;
  abstract getParam(): PlotParam;
  abstract getId(): Buffer;
  abstract getQualitiesForChallenge(challenge: Buffer): QualityProtocol[];
  abstract getStrength(): number;
}

export interface PlotInfo {
  prover: Prover;
  poolPublicKey: string | null;
  poolContractPuzzleHash: Buffer | null;
}

export interface PlotKeys {
  poolPublicKeyOrPuzzleHash: string;
  farmerPublicKey: string;
  localMasterSk: string;
}

/** Native plot / proof primitives, provided by the bindings layer. */
export interface PlotPrimitives {
  parsePlotInfo(memo: Buffer): PlotKeys;
  masterSkToLocalSk(masterSk: string): string;
  encodePuzzleHash(puzzleHash: Buffer, prefix: string): string;
  solveProof(partialProof: Buffer, plotId: Buffer, strength: number, plotSize: number, testnet: boolean): Buffer;
  validateProofV1(plotId: Buffer, size: number, challenge: Buffer, proof: Buffer): string | null;
  validateProofV2(plotId: Buffer, plotSize: number, challenge: Buffer, strength: number, proof: Buffer, testnet: boolean): string | null;
}

export interface PlotCheckOptions {
  addressPrefix: string;
  parallelRead: boolean;
  testnet: boolean;
  challenges: number;
  plotSizeV2: number;
}

/** Serialises console output from concurrent plot checks. */
export class Lock {
  private tail: Promise<void> = Promise.resolve();

  async withLock<T>(fn: () => T): Promise<T> {
    const prev = this.tail;
    let release!: () => void;
    this.tail = new Promise<void>((r) => (release = r));
    await prev;
    try {
      return fn();
    } finally {
      release();
    }
  }
}

export interface PlotCheckTotals {
  totalGoodPlotsV1: Map<number, number>;
  totalGoodPlotsV2: number;
  totalSize: number;
  badPlotsList: string[];
}

function stdHash(data: Buffer): Buffer {
  return createHash('sha256').update(data).digest();
}

function challengeBytes(i: number): Buffer {
  const buf = Buffer.alloc(32);
  buf.writeBigUInt64BE(BigInt(i), 24);
  return buf;
}

function errorText(e: unknown): string {
  return e instanceof Error ? `${e.constructor.name}: ${e.message}` : String(e);
}

function ratio(totalProofs: number, challenges: number): number {
  return Math.round((totalProofs / challenges) * 10_000) / 10_000;
}

export function createPlotChecker(primitives: PlotPrimitives, opts: PlotCheckOptions, totals: PlotCheckTotals) {
  const { addressPrefix, parallelRead, testnet, challenges } = opts;

  return async function processPlot(plotPath: string, plotInfo: PlotInfo, numStart: number, numEnd: number, lock: Lock): Promise<void> {
    const pr = plotInfo.prover;

    // Look up local_sk from plot to save locked memory
    const { farmerPublicKey, localMasterSk } = primitives.parsePlotInfo(pr.getMemo());
    const localSk = primitives.masterSkToLocalSk(localMasterSk);

    await lock.withLock(() => {
      log.info(`Testing plot ${plotPath} k=${JSON.stringify(pr.getParam())}`);
      if (plotInfo.poolPublicKey !== null) {
        log.info(`\t${'Pool public key:'.padEnd(23)} ${plotInfo.poolPublicKey}`);
      }
      if (plotInfo.poolContractPuzzleHash !== null) {
        const pca = primitives.encodePuzzleHash(plotInfo.poolContractPuzzleHash, addressPrefix);
        log.info(`\t${'Pool contract address:'.padEnd(23)} ${pca}`);
      }
      log.info(`\t${'Farmer public key:'.padEnd(23)} ${farmerPublicKey}`);
      log.info(`\t${'Local sk:'.padEnd(23)} ${localSk}`);
    });

    let totalProofs = 0;
    let caughtException = false;
    for (let i = numStart; i < numEnd; i++) {
      const challenge = stdHash(challengeBytes(i));
      let qualities: QualityProtocol[];
      // Some plot errors cause getQualitiesForChallenge to throw
      try {
        const qualityStartTime = Date.now();
        qualities = pr.getQualitiesForChallenge(challenge);
        const qualitySpentTime = Date.now() - qualityStartTime;
        if (qualitySpentTime > 8000) {
          log.warn(
            `\tLooking up qualities took: ${qualitySpentTime} ms. This should be below 8 seconds ` +
              `to minimize risk of losing rewards. Filepath: ${plotPath}`,
          );
        } else {
          log.info(`\tLooking up qualities took: ${qualitySpentTime} ms. Filepath: ${plotPath}`);
        }
      } catch (e) {
        if (e instanceof Error && e.message === 'GRResult_NoProof received') {
          log.info(`Proof dropped due to line point compression. Filepath: ${plotPath}`);
          continue;
        }
        log.error(`${errorText(e)} error in getting challenge qualities for plot ${plotPath}`);
        caughtException = true;
        break;
      }

      for (let index = 0; index < qualities.length; index++) {
        const quality = qualities[index];
        // Other plot errors cause getFullProof or validation to throw
        try {
          const proofStartTime = Date.now();
          const qualityStr = quality.getString();

          let fullProof: Buffer;
          if (pr instanceof V1Prover) {
            fullProof = pr.getFullProof(challenge, index, parallelRead);
          } else if (pr instanceof V2Prover && quality instanceof V2Quality) {
            fullProof = primitives.solveProof(quality.getPartialProof(), pr.getId(), pr.getStrength(), opts.plotSizeV2, testnet);
          } else {
            throw new Error('unsupported prover/quality combination');
          }

          const proofSpentTime = Date.now() - proofStartTime;
          if (proofSpentTime > 15000) {
            const action = pr instanceof V1Prover ? 'Finding' : 'Solving';
            log.warn(
              `\t${action} proof took: ${proofSpentTime} ms. This should be below 15 seconds ` +
                `to minimize risk of losing rewards. Filepath: ${plotPath}`,
            );
          } else {
            log.info(`\tFinding proof took: ${proofSpentTime} ms. Filepath: ${plotPath}`);
          }

          let verQualityStr: string | null = null;
          if (pr instanceof V1Prover) {
            verQualityStr = primitives.validateProofV1(pr.getId(), pr.getParam().sizeV1 ?? 0, challenge, fullProof);
          } else if (pr instanceof V2Prover) {
            verQualityStr = primitives.validateProofV2(pr.getId(), opts.plotSizeV2, challenge, pr.getStrength(), fullProof, testnet);
          }

          if (qualityStr === verQualityStr) {
            totalProofs += 1;
          } else {
            log.warn(`\tQuality doesn't match with proof. Filepath: ${plotPath} This can occasionally happen with a compressed plot.`);
          }
        } catch (e) {
          if (e instanceof Error && e.message === 'GRResult_NoProof received') {
            log.info(`Proof dropped due to line point compression. Filepath: ${plotPath}`);
            continue;
          }
          log.error(`${errorText(e)} error in proving/verifying for plot ${plotPath}. Filepath: ${plotPath}`);
          caughtException = true;
        }
      }
      if (caughtException) {
        break;
      }
    }

    if (totalProofs > 0 && !caughtException) {
      log.info(`\tProofs ${totalProofs} / ${challenges}, ${ratio(totalProofs, challenges)}. Filepath: ${plotPath}`);
      const param = pr.getParam();
      if (param.sizeV1 !== null) {
        const k = param.sizeV1;
        totals.totalGoodPlotsV1.set(k, (totals.totalGoodPlotsV1.get(k) ?? 0) + 1);
        totals.totalSize += statSync(plotPath).size;
      } else {
        totals.totalGoodPlotsV2 += 1;
        totals.totalSize += statSync(plotPath).size;
      }
    } else {
      log.error(`\tProofs ${totalProofs} / ${challenges}, ${ratio(totalProofs, challenges)} Filepath: ${plotPath}`);
      totals.badPlotsList.push(plotPath);
    }
  };
}
