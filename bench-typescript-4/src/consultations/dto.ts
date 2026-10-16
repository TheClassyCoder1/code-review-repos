import { Type } from 'class-transformer';
import { IsInt, IsOptional, IsPhoneNumber, IsString, IsUUID, Max, MaxLength, Min } from 'class-validator';

export class BookConsultationDto {
  @IsUUID()
  slotId!: string;

  @IsPhoneNumber()
  phone!: string;
}

export class CompleteConsultationDto {
  @IsString()
  @MaxLength(20_000)
  notes!: string;
}

export class CancelConsultationDto {
  @IsOptional()
  @IsString()
  @MaxLength(500)
  reason?: string;
}

export class HistoryQueryDto {
  @Type(() => Number)
  @IsInt()
  @Min(0)
  page: number = 0;

  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(50)
  pageSize: number = 20;
}

export interface JoinLink {
  url: string;
  clinicianName: string;
  startsAt: Date;
}
