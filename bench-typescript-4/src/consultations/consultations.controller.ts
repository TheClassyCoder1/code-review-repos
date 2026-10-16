import { Body, Controller, Get, Param, ParseUUIDPipe, Post, Query, Req, Res, UseGuards } from '@nestjs/common';
import { Request, Response } from 'express';
import { pipeline } from 'node:stream/promises';
import { PatientAuthGuard } from '../common/patient-auth.guard';
import { RecordingsService } from '../recordings/recordings.service';
import { ConsultationsService } from './consultations.service';
import { BookConsultationDto, CancelConsultationDto, HistoryQueryDto } from './dto';

type PatientRequest = Request & { patient: { id: string } };

@Controller('api/v1/consultations')
@UseGuards(PatientAuthGuard)
export class ConsultationsController {
  constructor(
    private readonly consultations: ConsultationsService,
    private readonly recordings: RecordingsService,
  ) {}

  @Get()
  async listUpcoming(@Req() req: PatientRequest) {
    return this.consultations.listUpcoming(req.patient.id);
  }

  @Get('history')
  async history(@Req() req: PatientRequest, @Query() query: HistoryQueryDto) {
    return this.consultations.listHistory(req.patient.id, query);
  }

  @Get(':id')
  async getConsultation(@Req() req: PatientRequest, @Param('id', ParseUUIDPipe) id: string) {
    const consultation = await this.consultations.getForPatient(req.patient.id, id);
    return {
      id: consultation.id,
      status: consultation.status,
      startsAt: consultation.startsAt,
      clinician: {
        id: consultation.clinician.id,
        displayName: consultation.clinician.displayName,
      },
    };
  }

  @Post()
  async book(@Req() req: PatientRequest, @Body() dto: BookConsultationDto) {
    return this.consultations.bookSlot(req.patient.id, dto);
  }

  @Get(':id/join')
  async join(@Req() req: PatientRequest, @Param('id', ParseUUIDPipe) id: string) {
    return this.consultations.getJoinLink(req.patient.id, id);
  }

  @Post(':id/cancel')
  async cancel(@Req() req: PatientRequest, @Param('id', ParseUUIDPipe) id: string, @Body() dto: CancelConsultationDto) {
    return this.consultations.cancelConsultation(req.patient.id, id, dto);
  }

  @Get(':id/recordings')
  async recordingsList(@Req() req: PatientRequest, @Param('id', ParseUUIDPipe) id: string) {
    return this.recordings.listRecordings(req.patient.id, id);
  }

  @Get(':id/recordings/:recordingId')
  async downloadRecording(
    @Req() req: PatientRequest,
    @Param('id', ParseUUIDPipe) id: string,
    @Param('recordingId', ParseUUIDPipe) recordingId: string,
    @Res() res: Response,
  ) {
    const { stream, sizeBytes } = await this.recordings.openRecordingStream(req.patient.id, id, recordingId);
    res.setHeader('Content-Type', 'video/mp4');
    res.setHeader('Content-Length', String(sizeBytes));
    await pipeline(stream, res);
  }
}
