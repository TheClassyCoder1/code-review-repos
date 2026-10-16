import { Type } from 'class-transformer';
import { ArrayMaxSize, ArrayMinSize, IsInt, IsNumber, IsString, IsUUID, Length, Max, Min, ValidateNested } from 'class-validator';

export class PrescriptionItemDto {
  @IsString()
  @Length(10, 11)
  ndc!: string;

  @IsString()
  @Length(1, 200)
  name!: string;

  @IsString()
  @Length(1, 200)
  dosage!: string;

  @IsInt()
  @Min(1)
  @Max(365)
  quantity!: number;

  @IsNumber({ maxDecimalPlaces: 2 })
  @Min(0)
  unitPrice!: number;
}

export class CreatePrescriptionDto {
  @IsUUID()
  consultationId!: string;

  @IsInt()
  @Min(0)
  @Max(11)
  refillsAllowed!: number;

  @IsInt()
  @Min(1)
  @Max(365)
  validForDays!: number;

  @ValidateNested({ each: true })
  @Type(() => PrescriptionItemDto)
  @ArrayMinSize(1)
  @ArrayMaxSize(10)
  items!: PrescriptionItemDto[];
}
