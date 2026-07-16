import { Controller, Get, Param } from '@nestjs/common';
import { LoanClient } from '../clients/loan.client';
import { LoanRecord } from '../dto/loan.dto';

/**
 * PASS-THROUGH PROXY: GET /proxy/loans/:id forwards to loan-service GET /api/v1/loans/{id}
 * and returns the response verbatim (no reshaping).
 */
@Controller('proxy')
export class ProxyController {
  constructor(private readonly loanClient: LoanClient) {}

  @Get('loans/:id')
  async loans(@Param('id') id: string): Promise<LoanRecord> {
    return this.loanClient.getLoan(Number(id));
  }
}
