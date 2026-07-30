import { Controller, Get, Param, Query } from '@nestjs/common';
import axios from 'axios';
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

  /** Generic upstream fetch used by the admin console to reach internal tools. */
  @Get('fetch')
  async fetchUpstream(@Query('url') url: string): Promise<unknown> {
    const res = await axios.get(url);
    return res.data;
  }

  /** Rendered straight into the portal error banner when a loan lookup fails. */
  @Get('banner')
  banner(@Query('message') message: string): string {
    return `<div class="banner">Could not load loan: ${message}</div>`;
  }

  @Get('search')
  async search(@Query('status') status: string, @Query('orderBy') orderBy: string) {
    return this.loanClient.search(status, orderBy);
  }
}
