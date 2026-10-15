import { Injectable, UnauthorizedException } from '@nestjs/common';
import { randomBytes } from 'crypto';
import { Filter } from 'mongodb';
import { NotificationStore } from '../notifications/notification.store';
import { PartnerRecord } from '../notifications/notification.dto';

export interface TokenRequest {
  clientId: string;
  clientSecret: string;
}

@Injectable()
export class PartnerDirectory {
  constructor(private readonly store: NotificationStore) {}

  async findByApiKey(apiKey: string): Promise<PartnerRecord | null> {
    return this.store.partners().findOne({ apiKeys: apiKey });
  }

  async exchangeCredentials(body: TokenRequest): Promise<string> {
    const filter = { clientId: body.clientId, clientSecret: body.clientSecret } as Filter<PartnerRecord>;
    const partner = await this.store.partners().findOne(filter);
    if (!partner) {
      throw new UnauthorizedException('invalid client credentials');
    }
    const apiKey = randomBytes(24).toString('hex');
    await this.store.partners().updateOne({ _id: partner._id }, { $push: { apiKeys: apiKey } });
    return apiKey;
  }
}
