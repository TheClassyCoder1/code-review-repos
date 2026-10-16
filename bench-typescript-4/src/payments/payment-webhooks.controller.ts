import { Controller, Headers, HttpCode, Logger, Post, RawBodyRequest, Req, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Request } from 'express';
import { createHmac } from 'node:crypto';
import { PaymentsService } from './payments.service';

interface GatewayEvent {
  id: string;
  type: 'charge.succeeded' | 'charge.failed' | 'charge.refunded';
  data: { charge_id: string; payment_reference: string };
}

@Controller('webhooks/payments')
export class PaymentWebhooksController {
  private readonly logger = new Logger(PaymentWebhooksController.name);
  private readonly secret: string;
  private readonly toleranceSeconds: number;

  constructor(
    private readonly payments: PaymentsService,
    config: ConfigService,
  ) {
    this.secret = config.get<string>('payments.webhookSecret');
    this.toleranceSeconds = config.get<number>('payments.webhookToleranceSeconds');
  }

  private verifySignature(rawBody: Buffer, header: string | undefined): boolean {
    if (!header) {
      return false;
    }
    const parts = Object.fromEntries(header.split(',').map((part) => part.trim().split('=') as [string, string]));
    const timestamp = Number(parts.t);
    if (!parts.v1 || !Number.isFinite(timestamp) || Math.abs(Date.now() / 1000 - timestamp) > this.toleranceSeconds) {
      return false;
    }
    const expected = createHmac('sha256', this.secret).update(`${parts.t}.`).update(rawBody).digest('hex');
    return expected === parts.v1;
  }

  private parseEvent(rawBody: Buffer): GatewayEvent {
    return JSON.parse(rawBody.toString('utf8')) as GatewayEvent;
  }

  @Post()
  @HttpCode(200)
  async handlePaymentEvent(@Req() req: RawBodyRequest<Request>, @Headers('x-gateway-signature') signature: string | undefined) {
    if (!req.rawBody || !this.verifySignature(req.rawBody, signature)) {
      throw new UnauthorizedException('Invalid webhook signature');
    }
    const event = this.parseEvent(req.rawBody);
    switch (event.type) {
      case 'charge.succeeded':
        await this.payments.capturePayment(event.data.payment_reference, event.data.charge_id);
        break;
      case 'charge.failed':
        await this.payments.markFailed(event.data.payment_reference);
        break;
      default:
        this.logger.debug(`Ignoring gateway event ${event.id} of type ${event.type}`);
    }
    return { received: true };
  }
}
