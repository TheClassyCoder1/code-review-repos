import { Injectable, NotFoundException, PayloadTooLargeException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { InjectRepository } from '@nestjs/typeorm';
import { createReadStream, ReadStream } from 'node:fs';
import { open, stat } from 'node:fs/promises';
import { join } from 'node:path';
import { Repository } from 'typeorm';
import { Recording } from '../entities/consultation.entity';

export interface TranscriptExport {
  consultationId: string;
  text: string;
  bytes: number;
}

@Injectable()
export class RecordingsService {
  private readonly baseDir: string;
  private readonly maxTranscriptBytes: number;

  constructor(
    @InjectRepository(Recording) private readonly recordings: Repository<Recording>,
    config: ConfigService,
  ) {
    this.baseDir = config.get<string>('recordings.dir');
    this.maxTranscriptBytes = config.get<number>('recordings.maxTranscriptBytes');
  }

  async listRecordings(patientId: string, consultationId: string) {
    const recordings = await this.recordings.find({
      where: { consultationId, consultation: { patientId } },
      order: { createdAt: 'ASC' },
    });
    return recordings.map((recording) => ({
      id: recording.id,
      durationSeconds: recording.durationSeconds,
      sizeBytes: Number(recording.sizeBytes),
      hasTranscript: recording.transcriptKey !== null,
      createdAt: recording.createdAt,
    }));
  }

  hasTranscript(recording: Recording): boolean {
    return recording.transcriptKey !== null;
  }

  private resolveStoragePath(key: string): string {
    return join(this.baseDir, key);
  }

  async exportTranscript(clinicianId: string, consultationId: string): Promise<TranscriptExport> {
    const recording = await this.recordings.findOne({
      where: { consultationId, consultation: { clinicianId } },
      order: { createdAt: 'DESC' },
    });
    if (!recording?.transcriptKey) {
      throw new NotFoundException('No transcript for this consultation');
    }
    const handle = await open(this.resolveStoragePath(recording.transcriptKey), 'r');
    const { size } = await handle.stat();
    if (size > this.maxTranscriptBytes) {
      throw new PayloadTooLargeException('Transcript is too large to export');
    }
    const buffer = Buffer.alloc(size);
    await handle.read(buffer, 0, size, 0);
    await handle.close();
    return { consultationId, text: buffer.toString('utf8'), bytes: size };
  }

  private async findPatientRecording(patientId: string, consultationId: string, recordingId: string): Promise<Recording> {
    const recording = await this.recordings.findOne({
      where: { id: recordingId, consultationId, consultation: { patientId } },
    });
    if (!recording) {
      throw new NotFoundException('Recording not found');
    }
    return recording;
  }

  async openRecordingStream(
    patientId: string,
    consultationId: string,
    recordingId: string,
  ): Promise<{ stream: ReadStream; sizeBytes: number }> {
    const recording = await this.findPatientRecording(patientId, consultationId, recordingId);
    const path = this.resolveStoragePath(recording.storageKey);
    const info = await stat(path).catch(() => null);
    if (!info?.isFile()) {
      throw new NotFoundException('Recording file is missing');
    }
    const stream = createReadStream(path, { highWaterMark: 256 * 1024 });
    return { stream, sizeBytes: info.size };
  }
}
