import {
  Body,
  Controller,
  ForbiddenException,
  Get,
  Headers,
  Param,
  ParseIntPipe,
  Patch,
  Post,
  Req,
  UseGuards,
} from '@nestjs/common';
import { PartnerAuthGuard, PartnerRequest } from '../auth/partner-auth.guard';
import { PartnerDirectory, TokenRequest } from '../auth/partner-directory.service';
import { canViewLoan, readPortalSession } from '../auth/portal-session';
import { PartnerWebhookService } from './partner-webhook.service';
import { PreferencesService } from './preferences.service';
import { NotificationStore } from './notification.store';
import { DeliveryRecord, NotificationPreferences, RegisterWebhookSchema, WebhookSubscription } from './notification.dto';

@Controller('portal')
export class NotificationsController {
  constructor(
    private readonly webhooks: PartnerWebhookService,
    private readonly preferences: PreferencesService,
    private readonly partners: PartnerDirectory,
    private readonly store: NotificationStore,
  ) {}

  @Post('partners/token')
  async token(@Body() body: TokenRequest): Promise<{ apiKey: string }> {
    return { apiKey: await this.partners.exchangeCredentials(body) };
  }

  @UseGuards(PartnerAuthGuard)
  @Post('partners/webhooks')
  async register(@Req() req: PartnerRequest, @Body() body: unknown): Promise<WebhookSubscription> {
    const input = RegisterWebhookSchema.parse(body);
    return this.webhooks.register(req.partner!.id, input);
  }

  @UseGuards(PartnerAuthGuard)
  @Get('partners/webhooks')
  async list(@Req() req: PartnerRequest): Promise<WebhookSubscription[]> {
    return this.store.subscriptionsForPartner(req.partner!.id);
  }

  @UseGuards(PartnerAuthGuard)
  @Get('partners/deliveries')
  async deliveries(@Req() req: PartnerRequest): Promise<DeliveryRecord[]> {
    return this.webhooks.recentDeliveries(req.partner!.id);
  }

  @UseGuards(PartnerAuthGuard)
  @Post('partners/webhooks/test')
  async test(@Body() body: { url: string }): Promise<{ status: number; latencyMs: number }> {
    return this.webhooks.probe(body.url);
  }

  @Patch('users/:userId/notification-preferences')
  async updatePreferences(
    @Param('userId', ParseIntPipe) userId: number,
    @Headers('authorization') authorization: string | undefined,
    @Body() patch: Record<string, unknown>,
  ): Promise<NotificationPreferences> {
    const session = readPortalSession(authorization);
    if (!canViewLoan(session, userId)) {
      throw new ForbiddenException();
    }
    return this.preferences.update(userId, patch);
  }
}
